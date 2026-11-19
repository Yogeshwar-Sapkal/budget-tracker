package com.yogeshwar.budgettracker.data

import com.yogeshwar.budgettracker.ledger.Category
import com.yogeshwar.budgettracker.ledger.Expense
import com.yogeshwar.budgettracker.ledger.IgnoredEntry
import com.yogeshwar.budgettracker.ledger.LedgerStore
import com.yogeshwar.budgettracker.ledger.Merchant
import com.yogeshwar.budgettracker.ledger.ReviewItem
import com.yogeshwar.budgettracker.parser.NotificationSource
import java.time.Instant
import java.util.concurrent.Callable

/** Saves and loads for the Ledger. No rules live here. */
class RoomLedgerStore(private val db: AppDatabase) : LedgerStore {
    private val dao = db.dao()

    override fun <T> inTransaction(block: () -> T): T = db.runInTransaction(Callable { block() })

    override fun categories() = dao.categories().map { Category(it.id, it.name) }
    override fun insertCategory(name: String): Category =
        Category(dao.insertCategory(CategoryEntity(name = name)), name)

    override fun merchants() = dao.merchants().map { it.toDomain() }
    override fun merchant(id: Long) = dao.merchant(id)?.toDomain()
    override fun insertMerchant(displayName: String, categoryId: Long?): Merchant =
        Merchant(dao.insertMerchant(MerchantEntity(displayName = displayName, categoryId = categoryId)), displayName, categoryId)

    override fun updateMerchant(merchant: Merchant) =
        dao.updateMerchant(MerchantEntity(merchant.id, merchant.displayName, merchant.categoryId))

    override fun deleteMerchant(id: Long) = dao.deleteMerchant(id)

    override fun merchantIdForKey(key: String) = dao.merchantIdForKey(key)
    override fun aliases(): Map<String, Long> = dao.aliases().associate { it.normalizedKey to it.merchantId }
    override fun setAlias(key: String, merchantId: Long) = dao.setAlias(AliasEntity(key, merchantId))
    override fun repointAliases(fromMerchantId: Long, toMerchantId: Long) =
        dao.repointAliases(fromMerchantId, toMerchantId)

    override fun expense(id: Long) = dao.expense(id)?.toDomain()
    override fun expensesBetween(from: Instant, toExclusive: Instant) =
        dao.expensesBetween(from.toEpochMilli(), toExclusive.toEpochMilli()).map { it.toDomain() }

    override fun insertExpense(expense: Expense): Expense =
        expense.copy(id = dao.insertExpense(expense.toEntity().copy(id = 0)))

    override fun updateExpense(expense: Expense) = dao.updateExpense(expense.toEntity())
    override fun deleteExpense(id: Long) = dao.deleteExpense(id)
    override fun repointExpenses(fromMerchantId: Long, toMerchantId: Long) =
        dao.repointExpenses(fromMerchantId, toMerchantId)

    override fun reviewItems() = dao.reviewItems().map {
        ReviewItem(it.id, NotificationSource.valueOf(it.source), it.title, it.body, Instant.ofEpochMilli(it.receivedAtMs), it.reason, it.notificationKey)
    }

    override fun insertReviewItem(item: ReviewItem): ReviewItem = item.copy(
        id = dao.insertReviewItem(
            ReviewEntity(0, item.source.name, item.title, item.body, item.receivedAt.toEpochMilli(), item.reason, item.notificationKey),
        ),
    )

    override fun deleteReviewItem(id: Long) = dao.deleteReviewItem(id)

    override fun ignoredEntries() = dao.ignoredEntries().map {
        IgnoredEntry(it.id, NotificationSource.valueOf(it.source), it.description, it.amountCents, Instant.ofEpochMilli(it.receivedAtMs), it.reason, it.notificationKey)
    }

    override fun insertIgnoredEntry(entry: IgnoredEntry): IgnoredEntry = entry.copy(
        id = dao.insertIgnoredEntry(
            IgnoredEntity(0, entry.source.name, entry.description, entry.amountCents, entry.receivedAt.toEpochMilli(), entry.reason, entry.notificationKey),
        ),
    )

    override fun lastAlertAt(): Instant? =
        listOfNotNull(dao.lastExpenseAlertMs(), dao.lastReviewMs(), dao.lastIgnoredMs()).maxOrNull()?.let(Instant::ofEpochMilli)

    override fun notificationSeen(key: String) = dao.notificationCount(key) > 0

    private fun MerchantEntity.toDomain() = Merchant(id, displayName, categoryId)

    private fun ExpenseEntity.toDomain() = Expense(
        id = id,
        merchantId = merchantId,
        amountCents = amountCents,
        occurredAt = Instant.ofEpochMilli(occurredAtMs),
        source = source?.let { NotificationSource.valueOf(it) },
        accountLabel = accountLabel,
        likelyDuplicateOfId = likelyDuplicateOfId,
        notificationKey = notificationKey,
    )

    private fun Expense.toEntity() = ExpenseEntity(
        id = id,
        merchantId = merchantId,
        amountCents = amountCents,
        occurredAtMs = occurredAt.toEpochMilli(),
        source = source?.name,
        accountLabel = accountLabel,
        likelyDuplicateOfId = likelyDuplicateOfId,
        notificationKey = notificationKey,
    )
}
