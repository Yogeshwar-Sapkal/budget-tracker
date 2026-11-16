package com.yogeshwar.budgettracker.ledger

import java.time.Instant

/**
 * Plain saving and loading, with no rules. All rules live in [Ledger].
 * Implementations are blocking, so callers must not use them on the Android main thread.
 */
interface LedgerStore {
    fun <T> inTransaction(block: () -> T): T

    fun categories(): List<Category>
    fun insertCategory(name: String): Category

    fun merchants(): List<Merchant>
    fun merchant(id: Long): Merchant?
    fun insertMerchant(displayName: String, categoryId: Long?): Merchant
    fun updateMerchant(merchant: Merchant)
    fun deleteMerchant(id: Long)

    /** Which Merchant a normalized merchant key belongs to. Several keys can point to one Merchant. */
    fun merchantIdForKey(key: String): Long?
    fun setAlias(key: String, merchantId: Long)
    fun repointAliases(fromMerchantId: Long, toMerchantId: Long)

    fun expense(id: Long): Expense?
    fun expensesBetween(from: Instant, toExclusive: Instant): List<Expense>
    fun insertExpense(expense: Expense): Expense
    fun updateExpense(expense: Expense)
    fun deleteExpense(id: Long)
    fun repointExpenses(fromMerchantId: Long, toMerchantId: Long)

    fun reviewItems(): List<ReviewItem>
    fun insertReviewItem(item: ReviewItem): ReviewItem
    fun deleteReviewItem(id: Long)

    fun ignoredEntries(): List<IgnoredEntry>
    fun insertIgnoredEntry(entry: IgnoredEntry): IgnoredEntry

    /** True if a notification with this key was already saved as an Expense, review item or ignored entry. */
    fun notificationSeen(key: String): Boolean
}
