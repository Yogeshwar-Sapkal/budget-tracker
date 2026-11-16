package com.yogeshwar.budgettracker.ledger.presentation

import com.yogeshwar.budgettracker.ledger.MonthSummary
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CategoryBar(val name: String, val amountText: String, val fraction: Float)

data class HomeModel(
    val monthTitle: String,
    val totalText: String,
    val previousMonthName: String,
    val previousTotalText: String,
    /** 0..1, scaled so the larger of this month and last month fills the bar. */
    val currentBar: Float,
    val previousBar: Float,
    val categories: List<CategoryBar>,
    val reviewCount: Int,
    val uncategorizedCount: Int,
    val isEmpty: Boolean,
)

fun homeModel(summary: MonthSummary, reviewCount: Int, uncategorizedCount: Int): HomeModel {
    val scale = maxOf(summary.totalCents, summary.previousMonthTotalCents).coerceAtLeast(0)
    fun bar(cents: Long) = if (scale == 0L) 0f else (cents.coerceAtLeast(0).toFloat() / scale)
    val top = summary.byCategory.maxOfOrNull { it.totalCents }?.coerceAtLeast(0) ?: 0
    return HomeModel(
        monthTitle = summary.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)),
        totalText = formatCents(summary.totalCents),
        previousMonthName = summary.month.minusMonths(1).format(DateTimeFormatter.ofPattern("MMMM", Locale.US)),
        previousTotalText = formatCents(summary.previousMonthTotalCents),
        currentBar = bar(summary.totalCents),
        previousBar = bar(summary.previousMonthTotalCents),
        categories = summary.byCategory.map {
            CategoryBar(it.categoryName, formatCents(it.totalCents), if (top == 0L) 0f else it.totalCents.coerceAtLeast(0).toFloat() / top)
        },
        reviewCount = reviewCount,
        uncategorizedCount = uncategorizedCount,
        isEmpty = summary.byCategory.isEmpty(),
    )
}
