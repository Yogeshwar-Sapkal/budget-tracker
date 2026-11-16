package com.yogeshwar.budgettracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    vm: BudgetViewModel,
    listening: Boolean,
    onFixListening: () -> Unit,
    onReview: () -> Unit,
    onSortMerchants: () -> Unit,
) {
    val home = vm.home
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MonthSwitcher(home?.monthTitle ?: "", vm.canGoNext, vm::previousMonth, vm::nextMonth)
        }
        item { ListeningChip(listening, onFixListening) }
        if (home == null) return@LazyColumn

        item {
            SurfaceCard {
                Column(Modifier.padding(16.dp)) {
                    Text("Spent in ${home.monthTitle.substringBefore(' ')}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(home.totalText, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                    Column(Modifier.padding(top = 12.dp)) {
                        BarRow(home.monthTitle.substringBefore(' '), home.currentBar, home.totalText)
                        BarRow(home.previousMonthName, home.previousBar, home.previousTotalText, muted = true)
                    }
                }
            }
        }
        if (home.reviewCount > 0) {
            item {
                val n = home.reviewCount
                Banner(if (n == 1) "1 notification couldn't be read" else "$n notifications couldn't be read", "Review", onReview)
            }
        }
        if (home.uncategorizedCount > 0) {
            item {
                val n = home.uncategorizedCount
                Banner(if (n == 1) "1 merchant has no category" else "$n merchants have no category", "Sort", onSortMerchants)
            }
        }
        if (home.isEmpty) {
            item {
                Text(
                    "Nothing recorded this month yet. New bank alerts appear here by themselves, or add one yourself.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            }
        } else {
            item { SectionLabel("Where it went") }
            item {
                SurfaceCard {
                    home.categories.forEach { c ->
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(c.name, fontWeight = FontWeight.SemiBold)
                                Text(c.amountText, fontWeight = FontWeight.SemiBold)
                            }
                            LinearProgressIndicator(
                                progress = { c.fraction },
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
