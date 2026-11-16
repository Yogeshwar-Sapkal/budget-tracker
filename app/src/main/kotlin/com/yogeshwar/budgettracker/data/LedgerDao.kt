package com.yogeshwar.budgettracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/** Blocking queries. Never call these on the main thread. */
@Dao
interface LedgerDao {
    @Query("SELECT * FROM categories ORDER BY id")
    fun categories(): List<CategoryEntity>

    @Insert
    fun insertCategory(category: CategoryEntity): Long

    @Query("SELECT * FROM merchants")
    fun merchants(): List<MerchantEntity>

    @Query("SELECT * FROM merchants WHERE id = :id")
    fun merchant(id: Long): MerchantEntity?

    @Insert
    fun insertMerchant(merchant: MerchantEntity): Long

    @Update
    fun updateMerchant(merchant: MerchantEntity)

    @Query("DELETE FROM merchants WHERE id = :id")
    fun deleteMerchant(id: Long)

    @Query("SELECT merchantId FROM merchant_aliases WHERE normalizedKey = :key")
    fun merchantIdForKey(key: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setAlias(alias: AliasEntity)

    @Query("UPDATE merchant_aliases SET merchantId = :toId WHERE merchantId = :fromId")
    fun repointAliases(fromId: Long, toId: Long)

    @Query("SELECT * FROM expenses WHERE id = :id")
    fun expense(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE occurredAtMs >= :fromMs AND occurredAtMs < :toMs ORDER BY occurredAtMs")
    fun expensesBetween(fromMs: Long, toMs: Long): List<ExpenseEntity>

    @Insert
    fun insertExpense(expense: ExpenseEntity): Long

    @Update
    fun updateExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    fun deleteExpense(id: Long)

    @Query("UPDATE expenses SET merchantId = :toId WHERE merchantId = :fromId")
    fun repointExpenses(fromId: Long, toId: Long)

    @Query("SELECT * FROM review_items ORDER BY receivedAtMs DESC")
    fun reviewItems(): List<ReviewEntity>

    @Insert
    fun insertReviewItem(item: ReviewEntity): Long

    @Query("DELETE FROM review_items WHERE id = :id")
    fun deleteReviewItem(id: Long)

    @Query("SELECT * FROM ignored_entries ORDER BY receivedAtMs DESC")
    fun ignoredEntries(): List<IgnoredEntity>

    @Insert
    fun insertIgnoredEntry(entry: IgnoredEntity): Long

    @Query("SELECT MAX(occurredAtMs) FROM expenses WHERE source IS NOT NULL")
    fun lastExpenseAlertMs(): Long?

    @Query("SELECT MAX(receivedAtMs) FROM review_items")
    fun lastReviewMs(): Long?

    @Query("SELECT MAX(receivedAtMs) FROM ignored_entries")
    fun lastIgnoredMs(): Long?

    @Query(
        "SELECT (SELECT COUNT(*) FROM expenses WHERE notificationKey = :key) + " +
            "(SELECT COUNT(*) FROM review_items WHERE notificationKey = :key) + " +
            "(SELECT COUNT(*) FROM ignored_entries WHERE notificationKey = :key)",
    )
    fun notificationCount(key: String): Int
}
