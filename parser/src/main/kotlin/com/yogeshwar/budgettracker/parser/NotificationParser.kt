package com.yogeshwar.budgettracker.parser

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Turns a notification into a [ParseResult].
 *
 * [sourcePackages] decides which apps count as a Notification Source. Everything else is
 * [ParseResult.NotRelevant]. Wallet apps are deliberately not sources: Capital One reports
 * every purchase itself.
 */
class NotificationParser(
    private val sourcePackages: Map<String, NotificationSource> = DEFAULT_SOURCE_PACKAGES,
) {
    fun parse(n: RawNotification): ParseResult {
        val source = sourcePackages[n.packageName] ?: return ParseResult.NotRelevant
        if (n.isGroupSummary) return ParseResult.NotRelevant
        val body = n.body?.trim().orEmpty()
        if (body.isEmpty()) return ParseResult.NotRelevant

        val tx = when (source) {
            NotificationSource.CHASE -> parseChase(body)
            NotificationSource.CAPITAL_ONE -> parseCapitalOne(n.title, body)
        } ?: return ParseResult.NeedsReview(n, source, "Unrecognised ${source.name} notification")

        return when (tx.kind) {
            TransactionKind.PURCHASE, TransactionKind.ATM_WITHDRAWAL -> ParseResult.Expense(tx)
            TransactionKind.EXTERNAL_TRANSFER -> ParseResult.Ignored(tx, IgnoreReason.TRANSFER)
        }
    }

    // "Your purchase for $6.53 at REMI FLOWER & COFF was approved."   title: "Savor Credit Card...3455"
    private fun parseCapitalOne(title: String?, body: String): ParsedTransaction? {
        val m = CAPITAL_ONE_PURCHASE.matchEntire(body) ?: return null
        return ParsedTransaction(
            source = NotificationSource.CAPITAL_ONE,
            kind = TransactionKind.PURCHASE,
            amountCents = Money.parseCents(m.groupValues[1]),
            rawMerchant = m.groupValues[2],
            accountLabel = title?.let { CARD_LAST4.find(it)?.groupValues?.get(1) },
            statedAt = null,
        )
    }

    private fun parseChase(body: String): ParsedTransaction? {
        // "Chase Freedom Unlimited Visa: You made a $14.68 transaction with Walmart... on Oct 26, 2026 at 2:24 PM ET."
        CHASE_CARD_PURCHASE.matchEntire(body)?.let { m ->
            return ParsedTransaction(
                source = NotificationSource.CHASE,
                kind = TransactionKind.PURCHASE,
                amountCents = Money.parseCents(m.groupValues[2]),
                rawMerchant = m.groupValues[3],
                accountLabel = m.groupValues[1],
                statedAt = parseEt(m.groupValues[4], m.groupValues[5]),
            )
        }

        // "Chase account 6566: Your $35.00 ATM withdrawal on Oct 24, 2026 at 1:52 PM ET was more than the $1.00 limit ..."
        CHASE_ALERT.matchEntire(body)?.let { m ->
            val what = m.groupValues[3]
            val (kind, merchant) = when {
                what.equals("ATM withdrawal", ignoreCase = true) -> TransactionKind.ATM_WITHDRAWAL to null
                what.startsWith("external transfer to ", ignoreCase = true) ->
                    TransactionKind.EXTERNAL_TRANSFER to what.substring("external transfer to ".length)
                else -> return null // deposits, payments, ... not sampled yet
            }
            return ParsedTransaction(
                source = NotificationSource.CHASE,
                kind = kind,
                amountCents = Money.parseCents(m.groupValues[2]),
                rawMerchant = merchant,
                accountLabel = m.groupValues[1],
                statedAt = parseEt(m.groupValues[4], m.groupValues[5]),
            )
        }
        return null
    }

    private fun parseEt(date: String, time: String): Instant? = try {
        LocalDateTime.parse("$date $time", ET_FORMAT).atZone(EASTERN).toInstant()
    } catch (_: Exception) {
        null
    }

    companion object {
        /**
         * Chase's package name is confirmed on a real phone (a purchase was recorded).
         * Capital One's is still a best guess: if its alerts never appear, check
         * `adb logcat -s BudgetTracker`, which logs the sending package of every notification.
         */
        val DEFAULT_SOURCE_PACKAGES: Map<String, NotificationSource> = mapOf(
            "com.chase.sig.android" to NotificationSource.CHASE,
            "com.konylabs.capitalone" to NotificationSource.CAPITAL_ONE,
        )

        private const val AMOUNT = """\$([\d,]+\.\d{2})"""
        private const val DATE = """([A-Z][a-z]{2} \d{1,2}, \d{4})"""
        private const val TIME = """(\d{1,2}:\d{2} [AP]M)"""

        private val CAPITAL_ONE_PURCHASE = Regex("""Your purchase for $AMOUNT at (.+) was approved\.""")
        private val CARD_LAST4 = Regex("""\.{3}(\d{4})\s*$""")

        private val CHASE_CARD_PURCHASE =
            Regex("""(.+?): You made a $AMOUNT transaction with (.+?) on $DATE at $TIME ET\.""")
        private val CHASE_ALERT =
            Regex("""Chase account (\d{4}): Your $AMOUNT (.+?) on $DATE at $TIME ET was more than the \$[\d,]+\.\d{2} limit in your Alerts settings\.""")

        private val ET_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US)
        private val EASTERN = ZoneId.of("America/New_York")
    }
}
