package com.yogeshwar.budgettracker.ledger.presentation

import java.time.Duration
import java.time.Instant

enum class ListeningStatus {
    LISTENING,

    /** Access is on but no bank alert has arrived for a while. Might be nothing, might be a dead listener. */
    QUIET,

    NOT_LISTENING,
}

fun listeningStatus(
    accessGranted: Boolean,
    lastAlertAt: Instant?,
    now: Instant,
    quietAfter: Duration = Duration.ofDays(3),
): ListeningStatus = when {
    !accessGranted -> ListeningStatus.NOT_LISTENING
    lastAlertAt == null -> ListeningStatus.LISTENING // new install: nothing to compare against
    Duration.between(lastAlertAt, now) >= quietAfter -> ListeningStatus.QUIET
    else -> ListeningStatus.LISTENING
}
