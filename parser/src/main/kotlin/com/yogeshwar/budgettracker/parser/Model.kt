package com.yogeshwar.budgettracker.parser

import java.time.Instant

/** A bank app whose notifications the tracker reads. */
enum class NotificationSource { CHASE, CAPITAL_ONE }

/** A notification as the Android listener sees it, reduced to what parsing needs. */
data class RawNotification(
    val packageName: String,
    val title: String?,
    val body: String?,
    val isGroupSummary: Boolean = false,
)

enum class TransactionKind { PURCHASE, ATM_WITHDRAWAL, EXTERNAL_TRANSFER }

data class ParsedTransaction(
    val source: NotificationSource,
    val kind: TransactionKind,
    val amountCents: Long,
    /** Merchant text exactly as reported. Null for ATM withdrawals. For transfers, the destination. */
    val rawMerchant: String?,
    /** Card last 4 digits, account last 4, or the card's name, whichever the source reports. */
    val accountLabel: String?,
    /** Date and time stated in the notification body, if it has one (Chase does, Capital One does not). */
    val statedAt: Instant?,
)

enum class IgnoreReason { TRANSFER }

sealed interface ParseResult {
    /** A Transaction that counts toward spending. */
    data class Expense(val transaction: ParsedTransaction) : ParseResult

    /** A Transaction that is deliberately not an Expense. */
    data class Ignored(val transaction: ParsedTransaction, val reason: IgnoreReason) : ParseResult

    /** From a Notification Source but not readable. Goes to the Needs Review list. */
    data class NeedsReview(
        val notification: RawNotification,
        val source: NotificationSource,
        val reason: String,
    ) : ParseResult

    /** Not from a Notification Source, or not a real message. Dropped. */
    data object NotRelevant : ParseResult
}
