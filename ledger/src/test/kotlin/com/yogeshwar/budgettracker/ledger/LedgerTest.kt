package com.yogeshwar.budgettracker.ledger

import com.yogeshwar.budgettracker.parser.IgnoreReason
import com.yogeshwar.budgettracker.parser.NotificationSource
import com.yogeshwar.budgettracker.parser.ParseResult
import com.yogeshwar.budgettracker.parser.ParsedTransaction
import com.yogeshwar.budgettracker.parser.RawNotification
import com.yogeshwar.budgettracker.parser.TransactionKind
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LedgerTest {
    private val store = InMemoryLedgerStore()
    private val ledger = Ledger(store).also { it.seedIfNeeded() }
    private val zone = ZoneId.of("America/New_York")

    private val t0 = Instant.parse("2026-10-24T19:00:00Z")

    private fun purchase(
        merchant: String?,
        cents: Long,
        kind: TransactionKind = TransactionKind.PURCHASE,
        source: NotificationSource = NotificationSource.CAPITAL_ONE,
    ) = ParseResult.Expense(ParsedTransaction(source, kind, cents, merchant, "1111", null))

    private fun record(result: ParseResult, at: Instant = t0, key: String? = null) =
        ledger.record(result, at, key)

    private fun categoryName(merchantId: Long): String? =
        store.merchant(merchantId)?.categoryId?.let { id -> store.categories().first { it.id == id }.name }

    private fun saved(outcome: RecordOutcome): Expense {
        assertIs<RecordOutcome.Saved>(outcome)
        return outcome.expense
    }

    // ---- Seed ----------------------------------------------------------

    @Test
    fun `seed creates the starter categories`() {
        val names = store.categories().map { it.name }
        assertTrue("Coffee & Drinks" in names)
        assertTrue("Groceries" in names)
        assertTrue("Cash" in names)
        assertEquals(12, names.size)
    }

    @Test
    fun `seed is idempotent`() {
        val categories = store.categories().size
        val merchants = store.merchants().size
        ledger.seedIfNeeded()
        assertEquals(categories, store.categories().size)
        assertEquals(merchants, store.merchants().size)
    }

    // ---- Recording -----------------------------------------------------

    @Test
    fun `a purchase from a seeded merchant takes its category`() {
        val e = saved(record(purchase("SQ *REMI FLOWER & COFF", 653)))
        assertEquals(653, e.amountCents)
        assertEquals(t0, e.occurredAt)
        assertEquals("Remi Flower & Coffee", store.merchant(e.merchantId)?.displayName)
        assertEquals("Coffee & Drinks", categoryName(e.merchantId))
    }

    @Test
    fun `store numbers and prefixes still reach the same merchant`() {
        val a = saved(record(purchase("Walmart2000153590447", 1468)))
        assertEquals("Walmart", store.merchant(a.merchantId)?.displayName)
        val b = saved(record(purchase("TST*L&B SPUMONI GARDEN", 1416)))
        assertEquals("Food & Dining", categoryName(b.merchantId))
    }

    @Test
    fun `an unknown merchant starts uncategorized and is reused next time`() {
        val a = saved(record(purchase("NEW PLACE 12", 500)))
        assertNull(store.merchant(a.merchantId)?.categoryId)
        assertEquals("NEW PLACE 12", store.merchant(a.merchantId)?.displayName)
        val b = saved(record(purchase("SQ *NEW PLACE 12", 700), t0.plusSeconds(86400)))
        assertEquals(a.merchantId, b.merchantId)
    }

    @Test
    fun `an ATM withdrawal is an expense at the merchant Cash`() {
        val e = saved(record(purchase(null, 3500, TransactionKind.ATM_WITHDRAWAL, NotificationSource.CHASE)))
        assertEquals("Cash", store.merchant(e.merchantId)?.displayName)
        assertEquals("Cash", categoryName(e.merchantId))
    }

    @Test
    fun `a transfer is kept as an ignored entry and not as an expense`() {
        val tx = ParsedTransaction(
            NotificationSource.CHASE, TransactionKind.EXTERNAL_TRANSFER, 200000, "FIDELITY CMA XXXXXXX", "1111", null,
        )
        val outcome = record(ParseResult.Ignored(tx, IgnoreReason.TRANSFER))
        assertIs<RecordOutcome.IgnoredSaved>(outcome)
        assertEquals(1, store.ignoredEntries().size)
        assertEquals(0, ledger.monthSummary(YearMonth.of(2026, 10), zone).totalCents)
    }

    @Test
    fun `an unreadable notification goes to needs review`() {
        val raw = RawNotification("com.chase.sig.android", "Chase", "some new wording")
        val outcome = record(ParseResult.NeedsReview(raw, NotificationSource.CHASE, "Unrecognised CHASE notification"))
        assertIs<RecordOutcome.ReviewSaved>(outcome)
        assertEquals("some new wording", store.reviewItems().single().body)
        ledger.dismissReview(store.reviewItems().single().id)
        assertTrue(store.reviewItems().isEmpty())
    }

    @Test
    fun `not relevant notifications save nothing`() {
        assertEquals(RecordOutcome.NothingToSave, record(ParseResult.NotRelevant))
    }

    @Test
    fun `the same notification delivered twice is saved once`() {
        saved(record(purchase("HEYTEA GRAND ST", 815), key = "n1|1"))
        assertEquals(RecordOutcome.AlreadySaved, record(purchase("HEYTEA GRAND ST", 815), key = "n1|1"))
        assertEquals(1, ledger.monthSummary(YearMonth.of(2026, 10), zone).byMerchant.single().expenseCount)
    }

    // ---- Likely duplicates --------------------------------------------

    @Test
    fun `same merchant and amount within five minutes is flagged but still counted`() {
        val first = saved(record(purchase("PATH TAPP PAYGO CP", 325, source = NotificationSource.CHASE), key = "a"))
        val second = saved(record(purchase("PATH TAPP PAYGO CP", 325, source = NotificationSource.CHASE), t0.plusSeconds(20), key = "b"))
        assertNull(first.likelyDuplicateOfId)
        assertEquals(first.id, second.likelyDuplicateOfId)
        assertEquals(650, ledger.monthSummary(YearMonth.of(2026, 10), zone).totalCents)
    }

    @Test
    fun `different amount or more than five minutes apart is not flagged`() {
        saved(record(purchase("PATH TAPP PAYGO CP", 325), key = "a"))
        assertNull(saved(record(purchase("PATH TAPP PAYGO CP", 300), t0.plusSeconds(30), key = "b")).likelyDuplicateOfId)
        assertNull(saved(record(purchase("PATH TAPP PAYGO CP", 325), t0.plusSeconds(6 * 60), key = "c")).likelyDuplicateOfId)
    }

    @Test
    fun `keeping both clears the flag, removing one deletes it`() {
        saved(record(purchase("PATH TAPP PAYGO CP", 325), key = "a"))
        val b = saved(record(purchase("PATH TAPP PAYGO CP", 325), t0.plusSeconds(10), key = "b"))
        ledger.keepBoth(b.id)
        assertNull(store.expense(b.id)?.likelyDuplicateOfId)

        val c = saved(record(purchase("PATH TAPP PAYGO CP", 325), t0.plusSeconds(20), key = "c"))
        assertNotNull(c.likelyDuplicateOfId)
        ledger.removeDuplicate(c.id)
        assertNull(store.expense(c.id))
    }

    // ---- Merchants and categories -------------------------------------

    @Test
    fun `assigning a category applies to every expense at that merchant`() {
        val a = saved(record(purchase("NEW PLACE 12", 500), key = "a"))
        saved(record(purchase("NEW PLACE 12", 700), t0.plusSeconds(3600), key = "b"))
        val before = ledger.monthSummary(YearMonth.of(2026, 10), zone)
        assertEquals(listOf("Uncategorized"), before.byCategory.map { it.categoryName })

        val groceries = store.categories().first { it.name == "Groceries" }
        ledger.assignCategory(a.merchantId, groceries.id)
        val after = ledger.monthSummary(YearMonth.of(2026, 10), zone)
        assertEquals(listOf("Groceries" to 1200L), after.byCategory.map { it.categoryName to it.totalCents })
    }

    @Test
    fun `renaming a merchant changes it everywhere`() {
        val a = saved(record(purchase("HEYTEA GRAND ST", 815)))
        ledger.renameMerchant(a.merchantId, "HeyTea (Grand St)")
        assertEquals("HeyTea (Grand St)", ledger.monthSummary(YearMonth.of(2026, 10), zone).byMerchant.single().merchantName)
    }

    @Test
    fun `merging merchants moves expenses and future notifications`() {
        val a = saved(record(purchase("COFFEE SHOP ONE", 400), key = "a"))
        val b = saved(record(purchase("COFFEE SHOP 1ST AVE", 600), t0.plusSeconds(3600), key = "b"))
        assertTrue(a.merchantId != b.merchantId)

        ledger.mergeMerchants(fromId = b.merchantId, intoId = a.merchantId)
        assertNull(store.merchant(b.merchantId))
        assertEquals(a.merchantId, store.expense(b.id)?.merchantId)

        val c = saved(record(purchase("COFFEE SHOP 1ST AVE", 300), t0.plusSeconds(7200), key = "c"))
        assertEquals(a.merchantId, c.merchantId)
    }

    @Test
    fun `adding a category twice does not duplicate it`() {
        val before = store.categories().size
        ledger.addCategory("coffee & drinks")
        assertEquals(before, store.categories().size)
        ledger.addCategory("Hobbies")
        assertEquals(before + 1, store.categories().size)
    }

    // ---- Manual expenses -----------------------------------------------

    @Test
    fun `a manual expense and a refund`() {
        val groceries = store.categories().first { it.name == "Groceries" }
        val e = ledger.addManualExpense(1250, isRefund = false, merchantName = "Corner Deli", occurredAt = t0, categoryId = groceries.id)
        assertNull(e.source)
        assertEquals("Groceries", categoryName(e.merchantId))
        ledger.addManualExpense(250, isRefund = true, merchantName = "corner deli", occurredAt = t0.plusSeconds(60))
        assertEquals(1000, ledger.monthSummary(YearMonth.of(2026, 10), zone).totalCents)
    }

    @Test
    fun `editing an expense can change amount merchant and date`() {
        val e = saved(record(purchase("NEW PLACE 12", 500)))
        ledger.editExpense(e.id, amountCents = 900, merchantName = "Other Place", occurredAt = t0.plusSeconds(100000))
        val edited = store.expense(e.id)!!
        assertEquals(900, edited.amountCents)
        assertEquals("Other Place", store.merchant(edited.merchantId)?.displayName)
        assertEquals(t0.plusSeconds(100000), edited.occurredAt)
    }

    @Test
    fun `deleting an expense removes it from totals`() {
        val e = saved(record(purchase("NEW PLACE 12", 500)))
        ledger.deleteExpense(e.id)
        assertEquals(0, ledger.monthSummary(YearMonth.of(2026, 10), zone).totalCents)
    }

    // ---- Month summary -------------------------------------------------

    @Test
    fun `months follow the phone timezone`() {
        // 11:30 PM ET on Oct 31 is Nov 1 03:30 UTC; it belongs to October.
        saved(record(purchase("NEW PLACE 12", 1000), Instant.parse("2026-11-01T03:30:00Z"), key = "a"))
        // 12:30 AM ET on Nov 1 belongs to November.
        saved(record(purchase("NEW PLACE 12", 2000), Instant.parse("2026-11-01T04:30:00Z"), key = "b"))
        assertEquals(1000, ledger.monthSummary(YearMonth.of(2026, 10), zone).totalCents)
        assertEquals(2000, ledger.monthSummary(YearMonth.of(2026, 11), zone).totalCents)
    }

    @Test
    fun `summary has previous month total and sorted breakdowns`() {
        saved(record(purchase("HEYTEA GRAND ST", 815), Instant.parse("2026-09-20T16:00:00Z"), key = "s"))
        saved(record(purchase("H&M", 19350), Instant.parse("2026-10-25T20:23:00Z"), key = "a"))
        saved(record(purchase("HEYTEA GRAND ST", 815), Instant.parse("2026-10-24T23:00:00Z"), key = "b"))
        saved(record(purchase("REMI FLOWER & COFF", 653), Instant.parse("2026-10-24T19:34:00Z"), key = "c"))

        val oct = ledger.monthSummary(YearMonth.of(2026, 10), zone)
        assertEquals(19350 + 815 + 653, oct.totalCents)
        assertEquals(815, oct.previousMonthTotalCents)
        assertEquals(listOf("Shopping", "Coffee & Drinks"), oct.byCategory.map { it.categoryName })
        assertEquals(listOf("H&M", "HeyTea", "Remi Flower & Coffee"), oct.byMerchant.map { it.merchantName })
    }
}
