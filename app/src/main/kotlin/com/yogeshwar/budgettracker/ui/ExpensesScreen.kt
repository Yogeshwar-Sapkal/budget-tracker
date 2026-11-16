package com.yogeshwar.budgettracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yogeshwar.budgettracker.ledger.presentation.expenseGroups
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ExpensesScreen(vm: BudgetViewModel, onEdit: (Long) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val groups = remember(vm.items, query) {
        expenseGroups(vm.items, ZoneId.systemDefault(), LocalDate.now(), query)
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            MonthSwitcher(vm.home?.monthTitle ?: "", vm.canGoNext, vm::previousMonth, vm::nextMonth)
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search merchant or amount") },
                singleLine = true,
            )
        }
        if (groups.isEmpty()) {
            item {
                Text(
                    if (query.isBlank()) "No expenses in this month." else "Nothing matches \"$query\".",
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        groups.forEach { group ->
            item { SectionLabel(group.heading) }
            item {
                SurfaceCard {
                    group.rows.forEachIndexed { index, row ->
                        if (index > 0) HorizontalDivider()
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEdit(row.id) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(row.merchantName, fontWeight = FontWeight.SemiBold)
                                    Text(row.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (row.isLikelyDuplicate) Pill("Possible duplicate")
                                }
                                Text(row.amountText, fontWeight = FontWeight.Bold)
                            }
                            if (row.isLikelyDuplicate) {
                                Card(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("This looks the same as another one. Was it two purchases?", color = MaterialTheme.colorScheme.onTertiaryContainer)
                                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(onClick = { vm.keepBoth(row.id) }, modifier = Modifier.weight(1f)) { Text("Yes, keep both") }
                                            Button(onClick = { vm.removeDuplicate(row.id) }, modifier = Modifier.weight(1f)) { Text("No, remove this") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
