package com.yogeshwar.budgettracker

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.yogeshwar.budgettracker.parser.NotificationParser
import com.yogeshwar.budgettracker.parser.ParseResult
import com.yogeshwar.budgettracker.parser.RawNotification

/**
 * Slice 1: reads notifications and runs them through the parser. Nothing is stored yet.
 *
 * Logs only the sending package and the kind of result, never amounts or merchants,
 * so bank details do not end up in logcat. The package log is how we confirm the
 * real Chase and Capital One package names on the phone.
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
        Log.d(TAG, "from ${sbn.packageName}: ${result::class.simpleName}")
        if (result is ParseResult.NeedsReview) Log.d(TAG, "needs review (${result.source})")
    }

    private companion object {
        const val TAG = "BudgetTracker"
    }
}
