package com.yogeshwar.budgettracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MonthSwitcher(title: String, canGoNext: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.semantics { contentDescription = "Previous month" }) {
            Text("‹", style = MaterialTheme.typography.headlineMedium)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        IconButton(
            onClick = onNext,
            enabled = canGoNext,
            modifier = Modifier.semantics { contentDescription = "Next month" },
        ) {
            Text("›", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

/** A coloured message with one clear button, used only when something needs the owner. */
@Composable
fun Banner(text: String, actionLabel: String, onAction: () -> Unit, warning: Boolean = true) {
    val container = if (warning) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
    val content = if (warning) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Card(colors = CardDefaults.cardColors(containerColor = container, contentColor = content), shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text, modifier = Modifier.weight(1f).padding(end = 8.dp), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun ListeningChip(listening: Boolean, onFix: () -> Unit) {
    val container = if (listening) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val content = if (listening) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
    Row(
        modifier = Modifier
            .background(container, RoundedCornerShape(50))
            .then(if (listening) Modifier else Modifier.clickable(onClick = onFix))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(content, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(
            if (listening) "Listening to Chase and Capital One" else "Not listening. Tap to fix",
            color = content,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
    )
}

@Composable
fun BarRow(label: String, fraction: Float, amount: String, muted: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, modifier = Modifier.width(92.dp), style = MaterialTheme.typography.bodyMedium)
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.weight(1f).height(10.dp),
            color = if (muted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
        )
        Text(amount, modifier = Modifier.width(84.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun Pill(text: String) {
    Text(
        text,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun SurfaceCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) { Column { content() } }
}
