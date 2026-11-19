package com.yogeshwar.budgettracker.ledger.presentation

import com.yogeshwar.budgettracker.ledger.Expense
import com.yogeshwar.budgettracker.ledger.ExpenseItem
import com.yogeshwar.budgettracker.ledger.ImportResult
import com.yogeshwar.budgettracker.parser.NotificationSource
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class ExportAndStatusTest {
    private val zone = ZoneId.of("America/New_York")
    private val crlf = "\r\n"

    private fun item(
        merchant: String,
        cents: Long,
        at: String,
        category: String? = null,
        source: NotificationSource? = NotificationSource.CAPITAL_ONE,
        account: String? = "1111",
        dup: Long? = null,
    ) = ExpenseItem(Expense(1, 1, cents, Instant.parse(at), source, account, dup), merchant, null, category)

    // ---- CSV -----------------------------------------------------------

    @Test
    fun `header and a normal row`() {
        val csv = CsvExport.expensesCsv(
            listOf(item("HeyTea", 815, "2026-10-24T23:00:00Z", "Coffee & Drinks")),
            zone,
        )
        assertEquals(
            "Date,Merchant,Category,Amount,Account,Source,Possible duplicate,Merchant aliases$crlf" +
                "2026-10-24 19:00,HeyTea,Coffee & Drinks,8.15,1111,Capital One,,$crlf",
            csv,
        )
    }

    @Test
    fun `rows are oldest first and manual and chase sources are named`() {
        val csv = CsvExport.expensesCsv(
            listOf(
                item("B", 200, "2026-10-25T12:00:00Z", source = NotificationSource.CHASE, account = "Chase Freedom Unlimited Visa"),
                item("A", 100, "2026-10-24T12:00:00Z", source = null, account = null),
            ),
            zone,
        )
        val rows = csv.trimEnd().split(crlf)
        assertEquals("2026-10-24 08:00,A,Uncategorized,1.00,,Manual,,", rows[1])
        assertEquals("2026-10-25 08:00,B,Uncategorized,2.00,Chase Freedom Unlimited Visa,Chase,,", rows[2])
    }

    @Test
    fun `refunds are negative and duplicates are marked`() {
        val csv = CsvExport.expensesCsv(
            listOf(item("H&M", -2500, "2026-10-24T12:00:00Z", dup = 7)),
            zone,
        )
        assertEquals("2026-10-24 08:00,H&M,Uncategorized,-25.00,1111,Capital One,Yes,", csv.trimEnd().split(crlf)[1])
    }

    @Test
    fun `commas quotes and line breaks are quoted`() {
        val csv = CsvExport.expensesCsv(
            listOf(item("Joe's \"Best\" Pizza, NYC\nSecond line", 100, "2026-10-24T12:00:00Z")),
            zone,
        )
        assertEquals(
            "\"Joe's \"\"Best\"\" Pizza, NYC\nSecond line\"",
            csv.substring(csv.indexOf("2026-10-24 08:00,") + "2026-10-24 08:00,".length).substringBefore(",Uncategorized"),
        )
    }

    @Test
    fun `text that a spreadsheet would run as a formula is defused`() {
        val csv = CsvExport.expensesCsv(listOf(item("=HYPERLINK(\"x\")", 100, "2026-10-24T12:00:00Z")), zone)
        assertEquals(true, csv.contains(",\"'=HYPERLINK(\"\"x\"\")\","))
        val plus = CsvExport.expensesCsv(listOf(item("+1 Cafe", 100, "2026-10-24T12:00:00Z")), zone)
        assertEquals(true, plus.contains(",'+1 Cafe,"))
    }

    @Test
    fun `merchant aliases are listed in the last column`() {
        val item = item("HeyTea", 815, "2026-10-24T23:00:00Z")
        val csv = CsvExport.expensesCsv(listOf(item), zone, mapOf(1L to listOf("heytea", "heytea grand st")))
        assertEquals("2026-10-24 19:00,HeyTea,Uncategorized,8.15,1111,Capital One,,heytea | heytea grand st", csv.trimEnd().split(crlf)[1])
    }

    @Test
    fun `empty export is just the header`() {
        assertEquals("Date,Merchant,Category,Amount,Account,Source,Possible duplicate,Merchant aliases$crlf", CsvExport.expensesCsv(emptyList(), zone))
    }

    // ---- Import summary ------------------------------------------------

    @Test
    fun `import summaries are plain words`() {
        assertEquals("Imported 3 expenses, 2 already here, 1 row skipped", ImportResult(3, 2, 1).summaryText())
        assertEquals("Imported 1 expense", ImportResult(1).summaryText())
        assertEquals("Nothing new: all 5 expenses were already here.", ImportResult(0, 5).summaryText())
        assertEquals("That file had no expenses.", ImportResult().summaryText())
        assertEquals("Imported 0 expenses, 2 rows skipped", ImportResult(0, 0, 2).summaryText())
        assertEquals("Not a Budget Tracker file", ImportResult(problem = "Not a Budget Tracker file").summaryText())
    }

    @Test
    fun `file name uses the date`() {
        assertEquals("budget-tracker-2026-11-16.csv", CsvExport.fileName(java.time.LocalDate.of(2026, 11, 16)))
    }

    // ---- Listening status ---------------------------------------------

    private val now = Instant.parse("2026-11-16T12:00:00Z")

    @Test
    fun `access off is not listening whatever else is true`() {
        assertEquals(ListeningStatus.NOT_LISTENING, listeningStatus(false, now, now))
    }

    @Test
    fun `a brand new install with no alerts yet is fine`() {
        assertEquals(ListeningStatus.LISTENING, listeningStatus(true, null, now))
    }

    @Test
    fun `recent alert is listening and three days of silence is quiet`() {
        assertEquals(ListeningStatus.LISTENING, listeningStatus(true, now.minus(Duration.ofDays(2)), now))
        assertEquals(ListeningStatus.QUIET, listeningStatus(true, now.minus(Duration.ofDays(3)), now))
        assertEquals(ListeningStatus.QUIET, listeningStatus(true, now.minus(Duration.ofDays(10)), now))
    }
}
