package com.yogeshwar.budgettracker.ledger

import com.yogeshwar.budgettracker.ledger.presentation.CsvExport
import com.yogeshwar.budgettracker.parser.NotificationSource
import com.yogeshwar.budgettracker.parser.ParseResult
import com.yogeshwar.budgettracker.parser.ParsedTransaction
import com.yogeshwar.budgettracker.parser.TransactionKind
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CsvParserTest {
    @Test
    fun `plain quoted and multi-line fields`() {
        val rows = CsvParser.parse("a,\"b,c\",\"say \"\"hi\"\"\"\r\n1,\"line1\nline2\",3\r\n")
        assertEquals(listOf(listOf("a", "b,c", "say \"hi\""), listOf("1", "line1\nline2", "3")), rows)
    }

    @Test
    fun `byte order mark blank lines and a missing final newline`() {
        assertEquals(listOf(listOf("x", "y"), listOf("1", "")), CsvParser.parse("﻿x,y\n\n1,"))
    }

    @Test
    fun `empty fields are kept`() {
        assertEquals(listOf(listOf("", "", "")), CsvParser.parse(",,\n"))
    }
}

class CsvImportTest {
    private val zone = ZoneId.of("America/New_York")
    private val t0 = Instant.parse("2026-10-24T19:00:37Z")

    private class World {
        val store = InMemoryLedgerStore()
        val ledger = Ledger(store).also { it.seedIfNeeded() }
    }

    private fun parsed(cents: Long, merchant: String) = ParseResult.Expense(
        ParsedTransaction(NotificationSource.CAPITAL_ONE, TransactionKind.PURCHASE, cents, merchant, "1111", null),
    )

    private fun export(w: World) = CsvExport.expensesCsv(w.ledger.allExpenseItems(), zone, w.ledger.aliasesByMerchant())

    private fun summary(w: World) = w.ledger.allExpenseItems()
        .map { listOf(it.merchantName, it.expense.amountCents, it.categoryName, it.expense.source, it.expense.accountLabel) }
        .sortedBy { it.toString() }

    private fun categoryNamed(w: World, name: String) = w.store.categories().first { it.name == name }

    private fun populated(): World {
        val a = World()
        a.ledger.record(parsed(815, "HEYTEA GRAND ST"), t0, "k1")
        val place = (a.ledger.record(parsed(500, "NEW PLACE 12"), t0.plusSeconds(3600), "k2") as RecordOutcome.Saved).expense
        a.ledger.renameMerchant(place.merchantId, "New Place")
        a.ledger.assignCategory(place.merchantId, a.ledger.addCategory("Hobbies").id)
        a.ledger.addManualExpense(1250, false, "Corner Deli", t0.plusSeconds(7200), categoryNamed(a, "Groceries").id)
        a.ledger.addManualExpense(300, true, "Corner Deli", t0.plusSeconds(7300))
        return a
    }

    @Test
    fun `a round trip restores expenses, categories and merchant aliases`() {
        val a = populated()
        val b = World()
        val result = b.ledger.importCsv(export(a), zone)

        assertEquals(ImportResult(imported = 4), result)
        assertEquals(summary(a), summary(b))
        assertEquals(a.ledger.allExpenseItems().sumOf { it.expense.amountCents }, b.ledger.allExpenseItems().sumOf { it.expense.amountCents })

        // The renamed merchant keeps catching its bank alerts, with no second merchant created.
        val before = b.store.merchants().size
        val next = b.ledger.record(parsed(900, "NEW PLACE 12"), t0.plusSeconds(86400), "k9") as RecordOutcome.Saved
        assertEquals("New Place", b.store.merchant(next.expense.merchantId)?.displayName)
        assertEquals(before, b.store.merchants().size)
    }

    @Test
    fun `importing the same file again adds nothing`() {
        val a = populated()
        val b = World()
        val csv = export(a)
        b.ledger.importCsv(csv, zone)
        assertEquals(ImportResult(alreadyThere = 4), b.ledger.importCsv(csv, zone))
        assertEquals(4, b.ledger.allExpenseItems().size)
    }

    @Test
    fun `importing a file into the ledger it came from adds nothing`() {
        val a = populated()
        assertEquals(ImportResult(alreadyThere = 4), a.ledger.importCsv(export(a), zone))
    }

    @Test
    fun `two identical purchases in the same minute both survive and are not doubled on re-import`() {
        val a = World()
        a.ledger.record(parsed(325, "PATH TAPP PAYGO CP"), t0, "x1")
        a.ledger.record(parsed(325, "PATH TAPP PAYGO CP"), t0.plusSeconds(5), "x2")
        val csv = export(a)

        val b = World()
        assertEquals(2, b.ledger.importCsv(csv, zone).imported)
        assertEquals(ImportResult(alreadyThere = 2), b.ledger.importCsv(csv, zone))

        // One already exists, the file has two: only the missing one is added.
        val c = World()
        c.ledger.record(parsed(325, "PATH TAPP PAYGO CP"), t0, "y1")
        assertEquals(ImportResult(imported = 1, alreadyThere = 1), c.ledger.importCsv(csv, zone))
        assertEquals(2, c.ledger.allExpenseItems().size)
    }

    @Test
    fun `imported flags are not restored and refunds stay negative`() {
        val a = populated()
        val b = World()
        b.ledger.importCsv(export(a), zone)
        assertEquals(true, b.ledger.allExpenseItems().all { it.expense.likelyDuplicateOfId == null })
        assertEquals(1, b.ledger.allExpenseItems().count { it.expense.amountCents < 0 })
    }

    @Test
    fun `text that was defused for spreadsheets comes back as written`() {
        val a = World()
        a.ledger.addManualExpense(100, false, "=SUM(A1)", t0)
        val b = World()
        b.ledger.importCsv(export(a), zone)
        assertEquals("=SUM(A1)", b.ledger.allExpenseItems().single().merchantName)
    }

    @Test
    fun `an older export without the aliases column still imports`() {
        val csv = "Date,Merchant,Category,Amount,Account,Source,Possible duplicate\r\n" +
            "2026-10-24 15:00,HeyTea,Coffee & Drinks,8.15,1111,Capital One,\r\n"
        val w = World()
        assertEquals(ImportResult(imported = 1), w.ledger.importCsv(csv, zone))
        assertEquals(NotificationSource.CAPITAL_ONE, w.ledger.allExpenseItems().single().expense.source)
    }

    @Test
    fun `a file that is not an export is refused with a plain message`() {
        val w = World()
        val r = w.ledger.importCsv("a,b\n1,2\n", zone)
        assertNotNull(r.problem)
        assertEquals(0, r.imported)
        assertEquals(0, w.ledger.allExpenseItems().size)
        assertNotNull(w.ledger.importCsv("", zone).problem)
    }

    @Test
    fun `bad rows are skipped and counted, good rows still import`() {
        val csv = "Date,Merchant,Category,Amount\r\n" +
            "2026-10-24 15:00,Good Shop,,5.00\r\n" +
            "not a date,Bad Date,,5.00\r\n" +
            "2026-10-24 16:00,Bad Amount,,abc\r\n" +
            "2026-10-24 17:00,,,5.00\r\n" +
            "2026-10-24 18:00,Zero,,0.00\r\n"
        val w = World()
        assertEquals(ImportResult(imported = 1, invalid = 4), w.ledger.importCsv(csv, zone))
    }

    @Test
    fun `a category from the file applies to a merchant with no expenses, and an existing choice wins otherwise`() {
        val csv = "Date,Merchant,Category,Amount\r\n" +
            "2026-10-24 15:00,HeyTea,Treats,8.15\r\n" +
            "2026-10-24 16:00,Walmart,Treats,20.00\r\n"
        val w = World()
        // Walmart already has an expense, so its existing category (Shopping) wins; HeyTea has none yet, so the file wins.
        w.ledger.addManualExpense(100, false, "Walmart", t0.minusSeconds(86400 * 30))
        w.ledger.importCsv(csv, zone)
        val byName = w.ledger.allExpenseItems().associate { it.merchantName to it.categoryName }
        assertEquals("Treats", byName["HeyTea"])
        assertEquals("Shopping", byName["Walmart"])
    }

    @Test
    fun `Uncategorized in the file leaves a merchant uncategorized`() {
        val w = World()
        w.ledger.importCsv("Date,Merchant,Category,Amount\r\n2026-10-24 15:00,Mystery,Uncategorized,5.00\r\n", zone)
        assertNull(w.ledger.allExpenseItems().single().categoryId)
    }

    @Test
    fun `aliases by merchant are listed`() {
        val a = populated()
        val place = a.store.merchants().first { it.displayName == "New Place" }
        assertEquals(listOf("new place 12"), a.ledger.aliasesByMerchant()[place.id])
    }
}
