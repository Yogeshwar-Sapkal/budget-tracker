package com.yogeshwar.budgettracker.ledger

import com.yogeshwar.budgettracker.parser.MerchantNormalizer
import com.yogeshwar.budgettracker.parser.ParseResult
import com.yogeshwar.budgettracker.parser.ParsedTransaction
import java.time.Duration
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * The rules of the tracker: what gets saved from a notification, how Merchants and Categories
 * relate, which Expenses look like duplicates, and how a Month adds up.
 * Blocking calls, so do not use it on the Android main thread.
 */
class Ledger(private val store: LedgerStore) {

    // ---- Setup ---------------------------------------------------------

    fun seedIfNeeded() = store.inTransaction {
        if (store.categories().isNotEmpty()) return@inTransaction
        val byName = Seed.categories.associateWith { store.insertCategory(it) }
        for (m in Seed.merchants) {
            val merchant = store.insertMerchant(m.displayName, byName.getValue(m.category).id)
            for (raw in m.rawNames) store.setAlias(MerchantNormalizer.normalize(raw).key, merchant.id)
        }
    }

    // ---- Notifications -------------------------------------------------

    /** Saves what a parsed notification means. [receivedAt] is when the notification arrived. */
    fun record(result: ParseResult, receivedAt: Instant, notificationKey: String? = null): RecordOutcome =
        store.inTransaction {
            if (result is ParseResult.NotRelevant) return@inTransaction RecordOutcome.NothingToSave
            if (notificationKey != null && store.notificationSeen(notificationKey)) {
                return@inTransaction RecordOutcome.AlreadySaved
            }
            when (result) {
                is ParseResult.Expense -> saveExpense(result.transaction, receivedAt, notificationKey)
                is ParseResult.Ignored -> {
                    val tx = result.transaction
                    store.insertIgnoredEntry(
                        IgnoredEntry(
                            source = tx.source,
                            description = listOfNotNull(tx.kind.name.lowercase().replace('_', ' '), tx.rawMerchant).joinToString(" "),
                            amountCents = tx.amountCents,
                            receivedAt = receivedAt,
                            reason = result.reason.name,
                            notificationKey = notificationKey,
                        ),
                    )
                    RecordOutcome.IgnoredSaved
                }
                is ParseResult.NeedsReview -> {
                    store.insertReviewItem(
                        ReviewItem(
                            source = result.source,
                            title = result.notification.title,
                            body = result.notification.body.orEmpty(),
                            receivedAt = receivedAt,
                            reason = result.reason,
                            notificationKey = notificationKey,
                        ),
                    )
                    RecordOutcome.ReviewSaved
                }
                ParseResult.NotRelevant -> RecordOutcome.NothingToSave
            }
        }

    private fun saveExpense(tx: ParsedTransaction, receivedAt: Instant, key: String?): RecordOutcome {
        val merchant = resolveMerchant(tx.rawMerchant ?: CASH_MERCHANT)
        val twin = store.expensesBetween(
            receivedAt - DUPLICATE_WINDOW,
            receivedAt + DUPLICATE_WINDOW + Duration.ofMillis(1),
        ).firstOrNull { it.merchantId == merchant.id && it.amountCents == tx.amountCents }
        val saved = store.insertExpense(
            Expense(
                merchantId = merchant.id,
                amountCents = tx.amountCents,
                occurredAt = receivedAt,
                source = tx.source,
                accountLabel = tx.accountLabel,
                likelyDuplicateOfId = twin?.id,
                notificationKey = key,
            ),
        )
        return RecordOutcome.Saved(saved)
    }

    fun dismissReview(id: Long) = store.deleteReviewItem(id)

    // ---- Duplicates ----------------------------------------------------

    fun keepBoth(expenseId: Long) {
        val e = store.expense(expenseId) ?: return
        store.updateExpense(e.copy(likelyDuplicateOfId = null))
    }

    fun removeDuplicate(expenseId: Long) = deleteExpense(expenseId)

    // ---- Expenses by hand ---------------------------------------------

    fun addManualExpense(
        amountCents: Long,
        isRefund: Boolean,
        merchantName: String,
        occurredAt: Instant,
        categoryId: Long? = null,
    ): Expense = store.inTransaction {
        val merchant = resolveMerchant(merchantName, categoryId)
        store.insertExpense(
            Expense(
                merchantId = merchant.id,
                amountCents = if (isRefund) -amountCents else amountCents,
                occurredAt = occurredAt,
                source = null,
                accountLabel = null,
            ),
        )
    }

    /** [amountCents] is signed: negative for a refund. */
    fun editExpense(id: Long, amountCents: Long, merchantName: String, occurredAt: Instant) {
        store.inTransaction {
            val e = store.expense(id) ?: return@inTransaction
            val merchant = resolveMerchant(merchantName)
            store.updateExpense(e.copy(amountCents = amountCents, merchantId = merchant.id, occurredAt = occurredAt))
        }
    }

    fun deleteExpense(id: Long) {
        store.inTransaction {
            val e = store.expense(id) ?: return@inTransaction
            store.deleteExpense(id)
            // Anything that was flagged as a repeat of this expense is no longer a repeat.
            store.expensesBetween(e.occurredAt - DUPLICATE_WINDOW, e.occurredAt + DUPLICATE_WINDOW + Duration.ofMillis(1))
                .filter { it.likelyDuplicateOfId == id }
                .forEach { store.updateExpense(it.copy(likelyDuplicateOfId = null)) }
        }
    }

    // ---- Merchants and categories -------------------------------------

    fun addCategory(name: String): Category = store.inTransaction {
        val clean = name.trim()
        store.categories().firstOrNull { it.name.equals(clean, ignoreCase = true) } ?: store.insertCategory(clean)
    }

    /** Applies to every Expense at that Merchant, past and future. Null clears it. */
    fun assignCategory(merchantId: Long, categoryId: Long?) {
        val m = store.merchant(merchantId) ?: return
        store.updateMerchant(m.copy(categoryId = categoryId))
    }

    fun renameMerchant(merchantId: Long, newName: String) {
        val m = store.merchant(merchantId) ?: return
        store.updateMerchant(m.copy(displayName = newName.trim()))
    }

    /** Moves everything from one Merchant to another. Notifications for the old one now land on the new one. */
    fun mergeMerchants(fromId: Long, intoId: Long) {
        if (fromId == intoId) return
        store.inTransaction {
            val from = store.merchant(fromId) ?: return@inTransaction
            val into = store.merchant(intoId) ?: return@inTransaction
            store.repointExpenses(fromId, intoId)
            store.repointAliases(fromId, intoId)
            if (into.categoryId == null && from.categoryId != null) {
                store.updateMerchant(into.copy(categoryId = from.categoryId))
            }
            store.deleteMerchant(fromId)
        }
    }

    private fun resolveMerchant(rawName: String, categoryId: Long? = null): Merchant {
        val n = MerchantNormalizer.normalize(rawName)
        val key = n.key.ifEmpty { "unknown" }
        val existing = store.merchantIdForKey(key)?.let { store.merchant(it) }
        if (existing != null) {
            if (categoryId != null && existing.categoryId != categoryId) {
                return existing.copy(categoryId = categoryId).also { store.updateMerchant(it) }
            }
            return existing
        }
        val created = store.insertMerchant(n.display.ifEmpty { "Unknown" }, categoryId)
        store.setAlias(key, created.id)
        return created
    }


    // ---- Reading for screens -------------------------------------------

    fun categories(): List<Category> = store.categories()

    fun reviewItems(): List<ReviewItem> = store.reviewItems()

    fun expense(id: Long): Expense? = store.expense(id)

    fun expenseItems(month: YearMonth, zone: ZoneId): List<ExpenseItem> {
        val merchants = store.merchants().associateBy { it.id }
        val categories = store.categories().associateBy { it.id }
        return expensesIn(month, zone).map { e ->
            val m = merchants[e.merchantId]
            ExpenseItem(e, m?.displayName ?: "Unknown", m?.categoryId, m?.categoryId?.let { categories[it]?.name })
        }
    }

    fun merchantsOverview(month: YearMonth, zone: ZoneId): MerchantsOverview {
        val summary = monthSummary(month, zone)
        val spent = summary.byMerchant.associateBy { it.merchantId }
        val byName = compareByDescending<MerchantTotal> { it.totalCents }.thenBy { it.merchantName }
        val needCategory = store.merchants().filter { it.categoryId == null }.map {
            spent[it.id] ?: MerchantTotal(it.id, it.displayName, null, 0, 0)
        }.sortedWith(byName)
        return MerchantsOverview(needCategory, summary.byMerchant.filter { it.categoryName != null })
    }

    /** Puts back an Expense that was just deleted. It gets a new id. */
    fun restoreExpense(expense: Expense) {
        store.insertExpense(expense.copy(id = 0, likelyDuplicateOfId = null))
    }

    // ---- Reporting -----------------------------------------------------

    fun monthSummary(month: YearMonth, zone: ZoneId): MonthSummary {
        val merchants = store.merchants().associateBy { it.id }
        val categories = store.categories().associateBy { it.id }
        val expenses = expensesIn(month, zone)

        val byCategory = expenses
            .groupBy { merchants[it.merchantId]?.categoryId }
            .map { (categoryId, list) ->
                CategoryTotal(categoryId, categories[categoryId]?.name ?: UNCATEGORIZED, list.sumOf { it.amountCents })
            }
            .sortedWith(compareByDescending<CategoryTotal> { it.totalCents }.thenBy { it.categoryName })

        val byMerchant = expenses
            .groupBy { it.merchantId }
            .map { (merchantId, list) ->
                val m = merchants[merchantId]
                MerchantTotal(
                    merchantId = merchantId,
                    merchantName = m?.displayName ?: "Unknown",
                    categoryName = m?.categoryId?.let { categories[it]?.name },
                    totalCents = list.sumOf { it.amountCents },
                    expenseCount = list.size,
                )
            }
            .sortedWith(compareByDescending<MerchantTotal> { it.totalCents }.thenBy { it.merchantName })

        return MonthSummary(
            month = month,
            totalCents = expenses.sumOf { it.amountCents },
            previousMonthTotalCents = expensesIn(month.minusMonths(1), zone).sumOf { it.amountCents },
            byCategory = byCategory,
            byMerchant = byMerchant,
        )
    }

    /** Expenses in a calendar month, newest first. */
    fun expensesIn(month: YearMonth, zone: ZoneId): List<Expense> {
        val from = month.atDay(1).atStartOfDay(zone).toInstant()
        val to = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
        return store.expensesBetween(from, to).sortedByDescending { it.occurredAt }
    }

    companion object {
        const val CASH_MERCHANT = "Cash"
        const val UNCATEGORIZED = "Uncategorized"
        val DUPLICATE_WINDOW: Duration = Duration.ofMinutes(5)
    }
}
