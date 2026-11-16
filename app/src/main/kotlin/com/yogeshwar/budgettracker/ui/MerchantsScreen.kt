package com.yogeshwar.budgettracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yogeshwar.budgettracker.ledger.MerchantTotal
import com.yogeshwar.budgettracker.ledger.presentation.formatCents

@Composable
fun MerchantsScreen(vm: BudgetViewModel) {
    var selected by remember { mutableStateOf<MerchantTotal?>(null) }
    val overview = vm.merchants

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { MonthSwitcher(vm.home?.monthTitle ?: "", vm.canGoNext, vm::previousMonth, vm::nextMonth) }

        if (overview.needCategory.isNotEmpty()) {
            item { SectionLabel("Needs a category") }
            item {
                SurfaceCard {
                    overview.needCategory.forEachIndexed { i, m ->
                        if (i > 0) HorizontalDivider()
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(m.merchantName, fontWeight = FontWeight.SemiBold)
                                Text(formatCents(m.totalCents), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = { selected = m }) { Text("Choose category") }
                        }
                    }
                }
            }
        }

        item { SectionLabel("Most spent") }
        if (overview.ranked.isEmpty()) {
            item { Text("No categorized spending this month.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp)) }
        } else {
            item {
                SurfaceCard {
                    overview.ranked.forEachIndexed { i, m ->
                        if (i > 0) HorizontalDivider()
                        Row(
                            Modifier.fillMaxWidth().clickable { selected = m }.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(m.merchantName, fontWeight = FontWeight.SemiBold)
                                Text(m.categoryName ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(formatCents(m.totalCents), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    selected?.let { m ->
        MerchantDialog(vm, m, onClose = { selected = null })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MerchantDialog(vm: BudgetViewModel, merchant: MerchantTotal, onClose: () -> Unit) {
    var name by remember { mutableStateOf(merchant.merchantName) }
    var categoryId by remember { mutableStateOf(vm.categories.firstOrNull { it.name == merchant.categoryName }?.id) }
    var newCategory by remember { mutableStateOf("") }
    var merging by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Edit merchant") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.categories.forEach { c ->
                        FilterChip(
                            selected = categoryId == c.id && newCategory.isBlank(),
                            onClick = { categoryId = c.id; newCategory = "" },
                            label = { Text(c.name) },
                        )
                    }
                }
                OutlinedTextField(value = newCategory, onValueChange = { newCategory = it }, label = { Text("Or make a new category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { merging = true }) { Text("Merge into another merchant…") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.saveMerchant(merchant.merchantId, name, categoryId, newCategory)
                onClose()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    )

    if (merging) MergeDialog(vm, merchant, onCancel = { merging = false }, onMerged = onClose)
}

@Composable
private fun MergeDialog(vm: BudgetViewModel, merchant: MerchantTotal, onCancel: () -> Unit, onMerged: () -> Unit) {
    var target by remember { mutableStateOf<Long?>(null) }
    val others = vm.allMerchants.filter { it.id != merchant.merchantId }
    val targetName = others.firstOrNull { it.id == target }?.displayName

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Merge ${merchant.merchantName} into…") },
        text = {
            Column {
                Text(
                    "All its purchases, and future alerts for it, move to the merchant you pick. This can't be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                LazyColumn(Modifier.height(260.dp).padding(top = 8.dp)) {
                    items(others, key = { it.id }) { m ->
                        Row(
                            Modifier.fillMaxWidth().clickable { target = m.id }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = target == m.id, onClick = { target = m.id })
                            Text(m.displayName, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = target != null,
                onClick = { target?.let { vm.mergeMerchants(merchant.merchantId, it); onMerged() } },
            ) { Text(if (targetName != null) "Merge into $targetName" else "Merge") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}
