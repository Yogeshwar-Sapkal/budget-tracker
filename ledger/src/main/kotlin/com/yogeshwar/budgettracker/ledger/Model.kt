package com.yogeshwar.budgettracker.ledger

import com.yogeshwar.budgettracker.parser.NotificationSource
import java.time.Instant

data class Category(val id: Long = 0, val name: String)

/** The place where an Expense was made. A null [categoryId] means Uncategorized. */
data class Merchant(val id: Long = 0, val displayName: String, val categoryId: Long?)

data class Expense(
    val id: Long = 0,
    val merchantId: Long,
    /** Negative for a refund. */
    val amountCents: Long,
    val occurredAt: Instant,
    /** Null for expenses the owner added by hand. */
    val source: NotificationSource?,
    val accountLabel: String?,
    /** Set when this looks like a repeat of another Expense. It still counts toward totals. */
    val likelyDuplicateOfId: Long? = null,
    /** Identifies the Android notification this came from, so a re-delivered one is not counted twice. */
    val notificationKey: String? = null,
)

data class ReviewItem(
    val id: Long = 0,
    val source: NotificationSource,
    val title: String?,
    val body: String,
    val receivedAt: Instant,
    val reason: String,
    val notificationKey: String? = null,
)

/** A Transaction deliberately kept out of spending, such as a transfer. */
data class IgnoredEntry(
    val id: Long = 0,
    val source: NotificationSource,
    val description: String,
    val amountCents: Long,
    val receivedAt: Instant,
    val reason: String,
    val notificationKey: String? = null,
)
