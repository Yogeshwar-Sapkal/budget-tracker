package com.yogeshwar.budgettracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReviewScreen(vm: BudgetViewModel, onAdd: (reviewId: Long) -> Unit, onClose: () -> Unit) {
    val format = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US)
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onClose) { Text("Back") }
                Text("Couldn't read these", style = MaterialTheme.typography.titleLarge)
                Text("", modifier = Modifier.padding(horizontal = 24.dp))
            }
        }
        if (vm.reviewItems.isEmpty()) {
            item { Text("Nothing to review.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp)) }
        } else {
            item {
                Text(
                    "The app wasn't sure about these notifications. Tell it what to do with each one.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(vm.reviewItems, key = { it.id }) { r ->
            SurfaceCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "${if (r.source.name == "CHASE") "Chase" else "Capital One"} · ${r.receivedAt.atZone(ZoneId.systemDefault()).format(format)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(r.body)
                    OutlinedButton(
                        onClick = { vm.dismissReview(r.id) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text("Ignore it, it's not spending") }
                    Button(
                        onClick = { onAdd(r.id) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text("Add it as an expense") }
                }
            }
        }
    }
}
