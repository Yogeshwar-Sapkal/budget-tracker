package com.yogeshwar.budgettracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List as ListIcon
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.yogeshwar.budgettracker.ledger.presentation.listeningStatus
import java.time.Instant
import kotlinx.coroutines.launch

private sealed interface Overlay {
    data object Setup : Overlay
    data object Review : Overlay
    data class Form(val expenseId: Long?, val reviewId: Long? = null) : Overlay
}

private class Tab(val label: String, val icon: ImageVector)

@Composable
fun AppRoot(vm: BudgetViewModel, listening: Boolean, openNotificationSettings: () -> Unit) {
    val tabs = listOf(
        Tab("This month", Icons.Filled.Home),
        Tab("Expenses", Icons.AutoMirrored.Filled.ListIcon),
        Tab("Merchants", Icons.Filled.ShoppingCart),
    )
    var tab by rememberSaveable { mutableStateOf(0) }
    var overlay by remember { mutableStateOf<Overlay?>(if (listening) null else Overlay.Setup) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val status = remember(listening, vm.lastAlertAt) { listeningStatus(listening, vm.lastAlertAt, Instant.now()) }

    BackHandler(enabled = overlay != null) { overlay = null }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (overlay == null) {
                NavigationBar {
                    tabs.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (overlay == null && tab != 2) {
                ExtendedFloatingActionButton(
                    onClick = { overlay = Overlay.Form(null) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add expense") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (val o = overlay) {
                Overlay.Setup -> SetupScreen(openNotificationSettings, onNotNow = { overlay = null })
                Overlay.Review -> ReviewScreen(vm, onAdd = { overlay = Overlay.Form(null, it) }, onClose = { overlay = null })
                is Overlay.Form -> ExpenseFormScreen(
                    vm = vm,
                    expenseId = o.expenseId,
                    reviewId = o.reviewId,
                    onClose = { overlay = null },
                    onDeleted = {
                        scope.launch {
                            val result = snackbar.showSnackbar("Expense deleted", actionLabel = "Undo")
                            if (result == SnackbarResult.ActionPerformed) vm.undoDelete()
                        }
                    },
                )
                null -> when (tab) {
                    0 -> HomeScreen(
                        vm = vm,
                        status = status,
                        onFixListening = { overlay = Overlay.Setup },
                        onReview = { overlay = Overlay.Review },
                        onSortMerchants = { tab = 2 },
                    )
                    1 -> ExpensesScreen(
                        vm,
                        onEdit = { overlay = Overlay.Form(it) },
                        onExported = { count ->
                            scope.launch {
                                snackbar.showSnackbar(if (count != null) "Exported $count expenses" else "Couldn't save the file")
                            }
                        },
                    )
                    else -> MerchantsScreen(vm)
                }
            }
        }
    }
}
