package com.yogeshwar.budgettracker.ledger.presentation

import com.yogeshwar.budgettracker.ledger.CategoryTotal
import com.yogeshwar.budgettracker.ledger.Expense
import com.yogeshwar.budgettracker.ledger.ExpenseItem
import com.yogeshwar.budgettracker.ledger.MonthSummary
import com.yogeshwar.budgettracker.parser.NotificationSource
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class PresentationTest {
    private val zone = ZoneId.of("America/New_York")

    // ---- money ---------------------------------------------------------

    @Test
    fun `formats cents as dollars`() {
        assertEquals("\$343.31", formatCents(34331))
        assertEquals("\$0.05", formatCents(5))
        assertEquals("\$1,234.56", formatCents(123456))
        assertEquals("-\$10.00", formatCents(-1000))
        assertEquals("\$0.00", formatCents(0))
    }

    @Test
    fun `parses what a person types`() {
        assertEquals(1250L, parseCents("12.50"))
        assertEquals(1250L, parseCents("12.5"))
        assertEquals(1200L, parseCents("12"))
        assertEquals(1250L, parseCents(" \$12.50 "))
        assertEquals(123456L, parseCents("1,234.56"))
        assertEquals(50L, parseCents(".50"))
    }

    @Test
    fun `rejects amounts that are not a positive dollar figure`() {
        for (bad in listOf("", "abc", "0", "0.00", "-5", "12.345", "1.2.3", "\$")) {
            assertNull(parseCents(bad), "'$bad' should be rejected")
        }
    }

    // ---- form ----------------------------------------------------------

    @Test
    fun `valid form`() {
        val r = validateExpenseForm("12.50", "  Corner Deli ")
        assertIs<FormResult.Valid>(r)
        assertEquals(1250, r.amountCents)
        assertEquals("Corner Deli", r.merchantName)
    }

    @Test
    fun `invalid form explains each problem in plain words`() {
        val r = validateExpenseForm("", " ")
        assertIs<FormResult.Invalid>(r)
        assertEquals("Enter an amount like 12.50", r.amountError)
        assertEquals("Enter where you spent it", r.merchantError)
        val onlyAmount = validateExpenseForm("x", "Deli")
        assertIs<FormResult.Invalid>(onlyAmount)
        assertNull(onlyAmount.merchantError)
    }

    // ---- home ----------------------------------------------------------

    private fun summary(total: Long, prev: Long, vararg cats: Pair<String, Long>) = MonthSummary(
        month = YearMonth.of(2026, 10),
        totalCents = total,
        previousMonthTotalCents = prev,
        byCategory = cats.map { CategoryTotal(null, it.first, it.second) },
        byMerchant = emptyList(),
    )

    @Test
    fun `home model has titles totals and comparison bars`() {
        val m = homeModel(summary(34331, 41280, "Shopping" to 19350, "Cash" to 3500), reviewCount = 1, uncategorizedCount = 2)
        assertEquals("October 2026", m.monthTitle)
        assertEquals("\$343.31", m.totalText)
        assertEquals("September", m.previousMonthName)
        assertEquals("\$412.80", m.previousTotalText)
        assertEquals(1f, m.previousBar)
        assertEquals(34331f / 41280f, m.currentBar, 0.0001f)
        assertEquals(listOf("Shopping", "Cash"), m.categories.map { it.name })
        assertEquals(1f, m.categories[0].fraction)
        assertEquals(3500f / 19350f, m.categories[1].fraction, 0.0001f)
        assertEquals(1, m.reviewCount)
        assertEquals(2, m.uncategorizedCount)
        assertEquals(false, m.isEmpty)
    }

    @Test
    fun `an empty month is flagged and has no bars`() {
        val m = homeModel(summary(0, 0), 0, 0)
        assertEquals(true, m.isEmpty)
        assertEquals(0f, m.currentBar)
        assertEquals(0f, m.previousBar)
    }

    // ---- expense list --------------------------------------------------

    private fun item(
        id: Long,
        merchant: String,
        cents: Long,
        at: String,
        category: String? = null,
        account: String? = null,
        dup: Long? = null,
    ) = ExpenseItem(
        Expense(id, 1, cents, Instant.parse(at), NotificationSource.CAPITAL_ONE, account, dup),
        merchant, null, category,
    )

    @Test
    fun `groups by local day newest first with friendly headings`() {
        val today = LocalDate.of(2026, 11, 2)
        val items = listOf(
            item(1, "Mommy Pais", 4464, "2026-10-24T23:20:00Z", "Food & Dining", "1111"),
            item(2, "PATH", 325, "2026-11-02T22:48:00Z", "Transport", "Chase Freedom Unlimited Visa"),
            item(3, "PATH", 325, "2026-11-02T22:48:10Z", "Transport", "Chase Freedom Unlimited Visa", dup = 2),
            item(4, "Corner Deli", 500, "2026-11-01T15:00:00Z"),
        )
        val groups = expenseGroups(items, zone, today)
        assertEquals(listOf("Today", "Yesterday", "Sat, Oct 24"), groups.map { it.heading })
        assertEquals(listOf(3L, 2L), groups[0].rows.map { it.id })
        assertEquals(true, groups[0].rows[0].isLikelyDuplicate)
        assertEquals(false, groups[0].rows[1].isLikelyDuplicate)
        assertEquals("Transport · Chase Freedom Unlimited Visa", groups[0].rows[0].subtitle)
        assertEquals("Uncategorized", groups[1].rows[0].subtitle)
        assertEquals("Food & Dining · ••1111", groups[2].rows[0].subtitle)
        assertEquals("\$44.64", groups[2].rows[0].amountText)
    }

    @Test
    fun `a day in another year shows the year`() {
        val groups = expenseGroups(listOf(item(1, "A", 100, "2025-12-31T18:00:00Z")), zone, LocalDate.of(2026, 1, 5))
        assertEquals("Wed, Dec 31, 2025", groups.single().heading)
    }

    @Test
    fun `search matches merchant name or amount`() {
        val today = LocalDate.of(2026, 11, 2)
        val items = listOf(
            item(1, "HeyTea", 815, "2026-11-02T20:00:00Z"),
            item(2, "H&M", 19350, "2026-11-02T19:00:00Z"),
        )
        assertEquals(listOf(1L), expenseGroups(items, zone, today, "tea").flatMap { it.rows }.map { it.id })
        assertEquals(listOf(2L), expenseGroups(items, zone, today, "193.5").flatMap { it.rows }.map { it.id })
        assertEquals(listOf(1L), expenseGroups(items, zone, today, "\$8.15").flatMap { it.rows }.map { it.id })
        assertEquals(emptyList(), expenseGroups(items, zone, today, "zzz"))
    }

    @Test
    fun `refunds show as negative`() {
        val g = expenseGroups(listOf(item(1, "H&M", -2500, "2026-11-02T20:00:00Z")), zone, LocalDate.of(2026, 11, 2))
        assertEquals("-\$25.00", g.single().rows.single().amountText)
        assertEquals(true, g.single().rows.single().isRefund)
    }
}
