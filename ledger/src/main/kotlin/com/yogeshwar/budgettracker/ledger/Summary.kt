package com.yogeshwar.budgettracker.ledger

import java.time.YearMonth

data class CategoryTotal(val categoryId: Long?, val categoryName: String, val totalCents: Long)

data class MerchantTotal(
    val merchantId: Long,
    val merchantName: String,
    val categoryName: String?,
    val totalCents: Long,
    val expenseCount: Int,
)

data class MonthSummary(
    val month: YearMonth,
    val totalCents: Long,
    val previousMonthTotalCents: Long,
    /** Biggest first. Merchants with no category are grouped as "Uncategorized". */
    val byCategory: List<CategoryTotal>,
    /** Biggest first. */
    val byMerchant: List<MerchantTotal>,
)

sealed interface RecordOutcome {
    data class Saved(val expense: Expense) : RecordOutcome
    data object IgnoredSaved : RecordOutcome
    data object ReviewSaved : RecordOutcome
    /** This exact notification was saved before (Android re-delivered it). */
    data object AlreadySaved : RecordOutcome
    data object NothingToSave : RecordOutcome
}

/** An Expense with the names the screens need. */
data class ExpenseItem(
    val expense: Expense,
    val merchantName: String,
    val categoryId: Long?,
    val categoryName: String?,
)

data class MerchantsOverview(
    /** Merchants with no category, with their total for the month (may be zero). */
    val needCategory: List<MerchantTotal>,
    /** Merchants that have a category, biggest spend first. */
    val ranked: List<MerchantTotal>,
)

/** What an import did. [problem] is set when the file could not be used at all. */
data class ImportResult(
    val imported: Int = 0,
    val alreadyThere: Int = 0,
    val invalid: Int = 0,
    val problem: String? = null,
)
