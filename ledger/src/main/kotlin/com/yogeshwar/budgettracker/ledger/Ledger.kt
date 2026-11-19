package com.yogeshwar.budgettracker.ledger

import com.yogeshwar.budgettracker.ledger.presentation.CsvExport
import com.yogeshwar.budgettracker.ledger.presentation.parseCents
import com.yogeshwar.budgettracker.parser.MerchantNormalizer
import com.yogeshwar.budgettracker.parser.NotificationSource
import com.yogeshwar.budgettracker.parser.ParseResult
import com.yogeshwar.budgettracker.parser.ParsedTransaction
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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

    fun merchants(): List<Merchant> = store.merchants()

    fun lastAlertAt(): Instant? = store.lastAlertAt()

    /** The names each Merchant is known by in bank alerts, for export. */
    fun aliasesByMerchant(): Map<Long, List<String>> =
        store.aliases().entries.groupBy({ it.value }, { it.key }).mapValues { it.value.sorted() }

    /** Every expense in every month, for export. */
    fun allExpenseItems(): List<ExpenseItem> {
        val merchants = store.merchants().associateBy { it.id }
        val categories = store.categories().associateBy { it.id }
        return store.expensesBetween(Instant.EPOCH, Instant.ofEpochSecond(32_503_680_000L)).map { e ->
            val m = merchants[e.merchantId]
            ExpenseItem(e, m?.displayName ?: "Unknown", m?.categoryId, m?.categoryId?.let { categories[it]?.name })
        }
    }

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

    // ---- Import --------------------------------------------------------

    /**
     * Reads a CSV made by [com.yogeshwar.budgettracker.ledger.presentation.CsvExport] and adds what is missing.
     * Safe to run twice: a row counts as already there when the ledger holds as many expenses with the same
     * minute, merchant and amount. A category in the file is applied to a merchant that has none yet or has
     * no expenses; otherwise the category already chosen wins.
     */
    fun importCsv(csv: String, zone: ZoneId): ImportResult = store.inTransaction {
        val rows = CsvParser.parse(csv)
        val header = rows.firstOrNull()?.map { it.trim().lowercase() }.orEmpty()
        fun col(name: String) = header.indexOf(name).takeIf { it >= 0 }
        val dateCol = col("date")
        val merchantCol = col("merchant")
        val amountCol = col("amount")
        if (dateCol == null || merchantCol == null || amountCol == null) {
            return@inTransaction ImportResult(problem = "That doesn't look like a Budget Tracker export.")
        }
        val categoryCol = col("category")
        val accountCol = col("account")
        val sourceCol = col("source")
        val aliasCol = col("merchant aliases")

        val existing = store.expensesBetween(Instant.EPOCH, Instant.ofEpochSecond(32_503_680_000L))
        val merchantsWithExpenses = existing.map { it.merchantId }.toSet()
        val existingCounts = existing.groupingBy { Triple(it.occurredAt.truncatedTo(ChronoUnit.MINUTES), it.merchantId, it.amountCents) }.eachCount()
        val seenInFile = mutableMapOf<Triple<Instant, Long, Long>, Int>()

        var imported = 0
        var alreadyThere = 0
        var invalid = 0

        for (row in rows.drop(1)) {
            fun cell(i: Int?) = i?.let { row.getOrNull(it) }?.let(::undefuse)?.trim().orEmpty()

            val at = parseExportDate(row.getOrNull(dateCol).orEmpty().trim(), zone)
            val cents = parseSignedCents(row.getOrNull(amountCol).orEmpty().trim())
            val name = cell(merchantCol)
            if (at == null || cents == null || name.isEmpty()) { invalid++; continue }

            val aliases = cell(aliasCol).split(CsvExport.ALIAS_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
            val knownId = (listOf(MerchantNormalizer.normalize(name).key) + aliases).firstNotNullOfOrNull { store.merchantIdForKey(it) }

            val key = Triple(at.truncatedTo(ChronoUnit.MINUTES), knownId ?: -1L, cents)
            val nth = (seenInFile[key] ?: 0) + 1
            seenInFile[key] = nth
            if (knownId != null && nth <= (existingCounts[key] ?: 0)) { alreadyThere++; continue }

            var merchant = knownId?.let { store.merchant(it) }
            if (merchant == null) {
                merchant = store.insertMerchant(name, null)
                store.setAlias(MerchantNormalizer.normalize(name).key, merchant.id)
            }
            for (alias in aliases) if (store.merchantIdForKey(alias) == null) store.setAlias(alias, merchant.id)

            val categoryName = cell(categoryCol)
            if (categoryName.isNotEmpty() && !categoryName.equals(UNCATEGORIZED, ignoreCase = true) &&
                (merchant.categoryId == null || merchant.id !in merchantsWithExpenses)
            ) {
                store.updateMerchant(merchant.copy(categoryId = addCategory(categoryName).id))
            }

            store.insertExpense(
                Expense(
                    merchantId = merchant.id,
                    amountCents = cents,
                    occurredAt = at,
                    source = when (cell(sourceCol)) {
                        "Chase" -> NotificationSource.CHASE
                        "Capital One" -> NotificationSource.CAPITAL_ONE
                        else -> null
                    },
                    accountLabel = cell(accountCol).ifEmpty { null },
                ),
            )
            imported++
        }
        ImportResult(imported, alreadyThere, invalid)
    }

    /** Undoes the quote that export adds in front of text a spreadsheet could run as a formula. */
    private fun undefuse(value: String) =
        if (value.length > 1 && value[0] == '\'' && value[1] in "=+-@\t\r") value.substring(1) else value

    private fun parseExportDate(text: String, zone: ZoneId): Instant? = try {
        LocalDateTime.parse(text, EXPORT_DATE).atZone(zone).toInstant()
    } catch (_: Exception) {
        null
    }

    private fun parseSignedCents(text: String): Long? {
        val negative = text.startsWith("-")
        val cents = parseCents(text.removePrefix("-")) ?: return null
        return if (negative) -cents else cents
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
        private val EXPORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
