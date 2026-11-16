package com.yogeshwar.budgettracker

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.yogeshwar.budgettracker.parser.NotificationParser
import com.yogeshwar.budgettracker.parser.RawNotification
import java.time.Instant

/**
 * Reads notifications, parses them, and saves the result.
 *
 * Logs only the sending package and the kind of result, never amounts or merchants,
 * so bank details do not end up in logcat.
 */
class ExpenseNotificationListener : NotificationListenerService() {
    private val parser = NotificationParser()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val raw = RawNotification(
            packageName = sbn.packageName,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            body = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString(),
            isGroupSummary = sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
        )
        val result = parser.parse(raw)
        val receivedAt = Instant.ofEpochMilli(sbn.postTime)
        val key = "${sbn.key}|${sbn.postTime}"
        val app = application as BudgetApp

        // Called on the main thread, so hand the database work to the background thread.
        app.io.execute {
            try {
                val outcome = app.ledger.record(result, receivedAt, key)
                Log.d(TAG, "from ${sbn.packageName}: ${result::class.simpleName} -> ${outcome::class.simpleName}")
            } catch (e: Exception) {
                Log.e(TAG, "could not save notification from ${sbn.packageName}", e)
            }
        }
    }

    private companion object {
        const val TAG = "BudgetTracker"
    }
}
