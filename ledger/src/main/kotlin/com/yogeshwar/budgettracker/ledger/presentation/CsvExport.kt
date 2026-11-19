package com.yogeshwar.budgettracker.ledger.presentation

import com.yogeshwar.budgettracker.ledger.ExpenseItem
import com.yogeshwar.budgettracker.parser.NotificationSource
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Turns expenses into a CSV a spreadsheet can open. */
object CsvExport {
    private val HEADER = listOf("Date", "Merchant", "Category", "Amount", "Account", "Source", "Possible duplicate", "Merchant aliases")
    private val DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)
    private const val EOL = "\r\n"

    /** Separates the match names of one merchant inside the "Merchant aliases" cell. */
    const val ALIAS_SEPARATOR = " | "

    fun fileName(today: LocalDate) = "budget-tracker-$today.csv"

    /** Oldest first, one row per expense, amounts in plain dollars (refunds negative). */
    fun expensesCsv(
        items: List<ExpenseItem>,
        zone: ZoneId,
        aliasesByMerchant: Map<Long, List<String>> = emptyMap(),
    ): String {
        val rows = items.sortedBy { it.expense.occurredAt }.map { item ->
            val e = item.expense
            listOf(
                text(e.occurredAt.atZone(zone).format(DATE)),
                text(item.merchantName),
                text(item.categoryName ?: "Uncategorized"),
                amount(e.amountCents),
                text(e.accountLabel.orEmpty()),
                text(sourceName(e.source)),
                if (e.likelyDuplicateOfId != null) "Yes" else "",
                text(aliasesByMerchant[e.merchantId].orEmpty().joinToString(ALIAS_SEPARATOR)),
            )
        }
        return (listOf(HEADER) + rows).joinToString(separator = "") { row -> row.joinToString(",") { quote(it) } + EOL }
    }

    private fun sourceName(source: NotificationSource?) = when (source) {
        NotificationSource.CHASE -> "Chase"
        NotificationSource.CAPITAL_ONE -> "Capital One"
        null -> "Manual"
    }

    private fun amount(cents: Long) =
        (if (cents < 0) "-" else "") + String.format(Locale.US, "%d.%02d", abs(cents) / 100, abs(cents) % 100)

    /** Stops a spreadsheet from running text such as "=HYPERLINK(...)" as a formula. */
    private fun text(value: String) =
        if (value.isNotEmpty() && value[0] in "=+-@\t\r") "'$value" else value

    private fun quote(value: String) =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
}
