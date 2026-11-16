package com.yogeshwar.budgettracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SetupScreen(onOpenSettings: () -> Unit, onNotNow: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Let Budget Tracker read your bank alerts", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "It only looks at Chase and Capital One. Everything stays on this phone.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Step(1, "Tap the button below", "It opens Android's notification access list.")
        Step(2, "Turn on \"Budget Tracker\"", "Greyed out? Open Settings, Apps, Budget Tracker, the three-dot menu, then \"Allow restricted settings\". Then try again.")
        Step(3, "Come back here", "This screen checks it worked and starts listening.")
        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Open notification settings") }
        TextButton(onClick = onNotNow, modifier = Modifier.fillMaxWidth()) { Text("Not now") }
    }
}

@Composable
private fun Step(number: Int, title: String, detail: String) {
    SurfaceCard {
        Column(Modifier.padding(14.dp)) {
            Text("$number. $title", fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
