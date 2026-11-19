package com.yogeshwar.budgettracker.ledger

import java.time.Instant

/** A store that lives in memory. Used by tests; there is no rollback in [inTransaction]. */
class InMemoryLedgerStore : LedgerStore {
    private var nextId = 1L
    private fun id() = nextId++

    private val categories = mutableListOf<Category>()
    private val merchants = mutableMapOf<Long, Merchant>()
    private val aliases = mutableMapOf<String, Long>()
    private val expenses = mutableMapOf<Long, Expense>()
    private val reviews = mutableMapOf<Long, ReviewItem>()
    private val ignored = mutableMapOf<Long, IgnoredEntry>()

    override fun <T> inTransaction(block: () -> T): T = block()

    override fun categories() = categories.toList()
    override fun insertCategory(name: String) = Category(id(), name).also { categories += it }

    override fun merchants() = merchants.values.toList()
    override fun merchant(id: Long) = merchants[id]
    override fun insertMerchant(displayName: String, categoryId: Long?) =
        Merchant(id(), displayName, categoryId).also { merchants[it.id] = it }

    override fun updateMerchant(merchant: Merchant) { merchants[merchant.id] = merchant }
    override fun deleteMerchant(id: Long) { merchants.remove(id) }

    override fun merchantIdForKey(key: String) = aliases[key]
    override fun aliases(): Map<String, Long> = aliases.toMap()
    override fun setAlias(key: String, merchantId: Long) { aliases[key] = merchantId }
    override fun repointAliases(fromMerchantId: Long, toMerchantId: Long) {
        aliases.replaceAll { _, v -> if (v == fromMerchantId) toMerchantId else v }
    }

    override fun expense(id: Long) = expenses[id]
    override fun expensesBetween(from: Instant, toExclusive: Instant) =
        expenses.values.filter { it.occurredAt >= from && it.occurredAt < toExclusive }.sortedBy { it.occurredAt }

    override fun insertExpense(expense: Expense) = expense.copy(id = id()).also { expenses[it.id] = it }
    override fun updateExpense(expense: Expense) { expenses[expense.id] = expense }
    override fun deleteExpense(id: Long) { expenses.remove(id) }
    override fun repointExpenses(fromMerchantId: Long, toMerchantId: Long) {
        expenses.replaceAll { _, e -> if (e.merchantId == fromMerchantId) e.copy(merchantId = toMerchantId) else e }
    }

    override fun reviewItems() = reviews.values.toList()
    override fun insertReviewItem(item: ReviewItem) = item.copy(id = id()).also { reviews[it.id] = it }
    override fun deleteReviewItem(id: Long) { reviews.remove(id) }

    override fun ignoredEntries() = ignored.values.toList()
    override fun insertIgnoredEntry(entry: IgnoredEntry) = entry.copy(id = id()).also { ignored[it.id] = it }

    override fun lastAlertAt(): Instant? = listOfNotNull(
        expenses.values.filter { it.source != null }.maxOfOrNull { it.occurredAt },
        reviews.values.maxOfOrNull { it.receivedAt },
        ignored.values.maxOfOrNull { it.receivedAt },
    ).maxOrNull()

    override fun notificationSeen(key: String) =
        expenses.values.any { it.notificationKey == key } ||
            reviews.values.any { it.notificationKey == key } ||
            ignored.values.any { it.notificationKey == key }
}
