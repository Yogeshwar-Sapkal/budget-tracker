package com.yogeshwar.budgettracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.time.YearMonth
import java.time.ZoneId

/** Placeholder screen: notification access status plus this month's saved total. */
class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var totals: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply { textSize = 20f }
        totals = TextView(this).apply { textSize = 18f; setPadding(0, 32, 0, 32) }
        val button = Button(this).apply {
            text = "Open notification settings"
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        }
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 96, 48, 48)
                addView(status)
                addView(totals)
                addView(button)
            },
        )
    }

    override fun onResume() {
        super.onResume()
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true
        status.text = if (enabled) "Listening to your bank alerts" else "Not listening. Turn on notification access."

        val app = application as BudgetApp
        app.io.execute {
            val month = YearMonth.now()
            val summary = app.ledger.monthSummary(month, ZoneId.systemDefault())
            val text = "$month: ${dollars(summary.totalCents)} spent\n" +
                "Last month: ${dollars(summary.previousMonthTotalCents)}"
            runOnUiThread { totals.text = text }
        }
    }

    private fun dollars(cents: Long) = "$%d.%02d".format(cents / 100, kotlin.math.abs(cents % 100))
}
