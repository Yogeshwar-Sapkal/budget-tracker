package com.yogeshwar.budgettracker.ledger.presentation

import com.yogeshwar.budgettracker.ledger.ExpenseItem
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

data class ExpenseRowModel(
    val id: Long,
    val merchantName: String,
    val subtitle: String,
    val amountText: String,
    val isRefund: Boolean,
    val isLikelyDuplicate: Boolean,
)

data class ExpenseGroup(val heading: String, val rows: List<ExpenseRowModel>)

private val SHORT_DAY = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
private val LONG_DAY = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)

/** Newest first, grouped by local day. [query] matches merchant name or amount. */
fun expenseGroups(items: List<ExpenseItem>, zone: ZoneId, today: LocalDate, query: String = ""): List<ExpenseGroup> {
    val q = query.trim().lowercase().removePrefix("$").replace(",", "")
    val matching = if (q.isEmpty()) items else items.filter {
        it.merchantName.lowercase().contains(q) ||
            formatCents(abs(it.expense.amountCents)).removePrefix("$").replace(",", "").contains(q)
    }
    return matching
        .sortedByDescending { it.expense.occurredAt }
        .groupBy { it.expense.occurredAt.atZone(zone).toLocalDate() }
        .map { (day, list) -> ExpenseGroup(heading(day, today), list.map(::row)) }
}

private fun heading(day: LocalDate, today: LocalDate) = when {
    day == today -> "Today"
    day == today.minusDays(1) -> "Yesterday"
    day.year == today.year -> day.format(SHORT_DAY)
    else -> day.format(LONG_DAY)
}

private fun row(item: ExpenseItem): ExpenseRowModel {
    val account = item.expense.accountLabel?.let { if (it.all(Char::isDigit)) "••$it" else it }
    return ExpenseRowModel(
        id = item.expense.id,
        merchantName = item.merchantName,
        subtitle = listOfNotNull(item.categoryName ?: "Uncategorized", account).joinToString(" · "),
        amountText = formatCents(item.expense.amountCents),
        isRefund = item.expense.amountCents < 0,
        isLikelyDuplicate = item.expense.likelyDuplicateOfId != null,
    )
}
