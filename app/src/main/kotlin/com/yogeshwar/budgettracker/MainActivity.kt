package com.yogeshwar.budgettracker

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yogeshwar.budgettracker.ui.AppRoot
import com.yogeshwar.budgettracker.ui.BudgetTheme
import com.yogeshwar.budgettracker.ui.BudgetViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: BudgetViewModel by viewModels()
    private var listening by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listening = isListening()
        setContent {
            BudgetTheme {
                AppRoot(
                    vm = viewModel,
                    listening = listening,
                    openNotificationSettings = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        listening = isListening()
        viewModel.refresh() // picks up anything saved while the app was in the background
    }

    private fun isListening(): Boolean =
        Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true
}
