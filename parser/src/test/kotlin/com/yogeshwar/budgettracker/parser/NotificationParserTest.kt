package com.yogeshwar.budgettracker.parser

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Fixtures are the real notifications in docs/samples/notifications.md (S1-S10),
 * with account digits replaced by dummy values.
 */
class NotificationParserTest {
    private val parser = NotificationParser()

    private val capitalOnePkg = "com.konylabs.capitalone"
    private val chasePkg = "com.chase.sig.android"
    private val walletPkg = "com.google.android.apps.walletnfcrel"

    private fun capitalOne(body: String) =
        RawNotification(capitalOnePkg, "Savor Credit Card...1111", body)

    private fun chase(body: String) = RawNotification(chasePkg, "Chase", body)

    private fun expense(result: ParseResult): ParsedTransaction {
        assertIs<ParseResult.Expense>(result)
        return result.transaction
    }

    // ---- Capital One ---------------------------------------------------

    @Test
    fun `S2 capital one purchase`() {
        val tx = expense(parser.parse(capitalOne("Your purchase for \$6.53 at REMI FLOWER & COFF was approved.")))
        assertEquals(NotificationSource.CAPITAL_ONE, tx.source)
        assertEquals(TransactionKind.PURCHASE, tx.kind)
        assertEquals(653, tx.amountCents)
        assertEquals("REMI FLOWER & COFF", tx.rawMerchant)
        assertEquals("1111", tx.accountLabel)
        assertNull(tx.statedAt)
    }

    @Test
    fun `S3 S4 S5 S6 S8 capital one purchases`() {
        val cases = listOf(
            "Your purchase for \$14.16 at L&B SPUMONI GARDEN was approved." to (1416L to "L&B SPUMONI GARDEN"),
            "Your purchase for \$8.15 at HEYTEA GRAND ST was approved." to (815L to "HEYTEA GRAND ST"),
            "Your purchase for \$44.64 at MOMMY PAIS was approved." to (4464L to "MOMMY PAIS"),
            "Your purchase for \$193.50 at H&M was approved." to (19350L to "H&M"),
            "Your purchase for \$26.65 at FOOD STORY NATURAL MAR was approved." to (2665L to "FOOD STORY NATURAL MAR"),
        )
        for ((body, expected) in cases) {
            val tx = expense(parser.parse(capitalOne(body)))
            assertEquals(expected.first, tx.amountCents, body)
            assertEquals(expected.second, tx.rawMerchant, body)
        }
    }

    @Test
    fun `thousands separator in amount`() {
        val tx = expense(parser.parse(capitalOne("Your purchase for \$1,234.56 at SOME STORE was approved.")))
        assertEquals(123456, tx.amountCents)
    }

    @Test
    fun `google wallet receipts are not a source`() {
        val n = RawNotification(walletPkg, "SQ *REMI FLOWER & COFF", "\$6.53 with Capital One Savor MasterCard ••1111")
        assertEquals(ParseResult.NotRelevant, parser.parse(n))
    }

    @Test
    fun `unrecognised capital one wording needs review`() {
        val result = parser.parse(capitalOne("Your purchase for \$5.00 at SOMEWHERE was declined."))
        assertIs<ParseResult.NeedsReview>(result)
        assertEquals(NotificationSource.CAPITAL_ONE, result.source)
    }

    // ---- Chase ---------------------------------------------------------

    @Test
    fun `S1 chase atm withdrawal is an expense with no merchant`() {
        val tx = expense(
            parser.parse(
                chase("Chase account 0000: Your \$35.00 ATM withdrawal on Oct 24, 2026 at 1:52 PM ET was more than the \$1.00 limit in your Alerts settings."),
            ),
        )
        assertEquals(NotificationSource.CHASE, tx.source)
        assertEquals(TransactionKind.ATM_WITHDRAWAL, tx.kind)
        assertEquals(3500, tx.amountCents)
        assertNull(tx.rawMerchant)
        assertEquals("0000", tx.accountLabel)
        // 1:52 PM EDT on Oct 24 2026
        assertEquals(Instant.parse("2026-10-24T17:52:00Z"), tx.statedAt)
    }

    @Test
    fun `S7 chase card purchase keeps the raw merchant text`() {
        val tx = expense(
            parser.parse(
                chase("Chase Freedom Unlimited Visa: You made a \$14.68 transaction with Walmart2000153590447 on Oct 26, 2026 at 2:24 PM ET."),
            ),
        )
        assertEquals(TransactionKind.PURCHASE, tx.kind)
        assertEquals(1468, tx.amountCents)
        assertEquals("Walmart2000153590447", tx.rawMerchant)
        assertEquals("Chase Freedom Unlimited Visa", tx.accountLabel)
        assertEquals(Instant.parse("2026-10-26T18:24:00Z"), tx.statedAt)
    }

    @Test
    fun `S9 external transfer is ignored not spending`() {
        val result = parser.parse(
            chase("Chase account 0000: Your \$2,000.00 external transfer to FIDELITY CMA XXXXXXX on Oct 28, 2026 at 5:21 AM ET was more than the \$1.00 limit in your Alerts settings."),
        )
        assertIs<ParseResult.Ignored>(result)
        assertEquals(IgnoreReason.TRANSFER, result.reason)
        assertEquals(200000, result.transaction.amountCents)
        assertEquals(TransactionKind.EXTERNAL_TRANSFER, result.transaction.kind)
    }

    @Test
    fun `S10 identical notifications both parse, duplicate detection is a later step`() {
        val body = "Chase Freedom Unlimited Visa: You made a \$3.25 transaction with PATH TAPP PAYGO CP on Nov 2, 2026 at 5:48 PM ET."
        val a = expense(parser.parse(chase(body)))
        val b = expense(parser.parse(chase(body)))
        assertEquals(a, b)
        assertEquals(325, a.amountCents)
        // After daylight saving ended (Nov 1), 5:48 PM is EST (UTC-5)
        assertEquals(Instant.parse("2026-11-02T22:48:00Z"), a.statedAt)
    }

    @Test
    fun `unsampled chase wording such as a deposit needs review`() {
        val result = parser.parse(
            chase("Chase account 0000: Your \$1,250.00 deposit on Nov 3, 2026 at 9:02 AM ET was more than the \$1.00 limit in your Alerts settings."),
        )
        assertIs<ParseResult.NeedsReview>(result)
        assertEquals(NotificationSource.CHASE, result.source)
    }

    // ---- Filtering -----------------------------------------------------

    @Test
    fun `other apps are not relevant`() {
        val n = RawNotification("com.whatsapp", "Backup in progress", null)
        assertEquals(ParseResult.NotRelevant, parser.parse(n))
    }

    @Test
    fun `group summaries are not relevant so grouped notifications are not double counted`() {
        val n = RawNotification(chasePkg, "Chase", "2 new messages", isGroupSummary = true)
        assertEquals(ParseResult.NotRelevant, parser.parse(n))
    }

    @Test
    fun `source notification with no body is not relevant`() {
        assertEquals(ParseResult.NotRelevant, parser.parse(RawNotification(chasePkg, "Chase", null)))
        assertEquals(ParseResult.NotRelevant, parser.parse(RawNotification(chasePkg, "Chase", "   ")))
    }
}
