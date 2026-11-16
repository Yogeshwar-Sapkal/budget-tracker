package com.yogeshwar.budgettracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yogeshwar.budgettracker.ledger.presentation.FormResult
import com.yogeshwar.budgettracker.ledger.presentation.formatCents
import com.yogeshwar.budgettracker.ledger.presentation.validateExpenseForm
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Add an expense, or edit one when [expenseId] is given. [reviewId] marks a Needs Review item being added. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormScreen(
    vm: BudgetViewModel,
    expenseId: Long?,
    reviewId: Long?,
    onClose: () -> Unit,
    onDeleted: () -> Unit,
) {
    val existing = remember(expenseId) { vm.items.firstOrNull { it.expense.id == expenseId } }
    val zone = ZoneId.systemDefault()

    var amount by remember {
        mutableStateOf(existing?.let { formatCents(abs(it.expense.amountCents)).removePrefix("$").replace(",", "") } ?: "")
    }
    var merchant by remember { mutableStateOf(existing?.merchantName ?: "") }
    var isRefund by remember { mutableStateOf((existing?.expense?.amountCents ?: 0) < 0) }
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var occurredAt by remember { mutableStateOf(existing?.expense?.occurredAt ?: Instant.now()) }
    var attempted by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }

    val check = validateExpenseForm(amount, merchant)
    val invalid = check as? FormResult.Invalid

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onClose) { Text("Cancel") }
            Text(if (existing == null) "Add expense" else "Edit expense", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.padding(horizontal = 30.dp))
        }

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            prefix = { Text("$") },
            textStyle = MaterialTheme.typography.headlineMedium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            isError = attempted && invalid?.amountError != null,
            supportingText = { if (attempted) invalid?.amountError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = merchant,
            onValueChange = { merchant = it },
            label = { Text("Where") },
            singleLine = true,
            isError = attempted && invalid?.merchantError != null,
            supportingText = { if (attempted) invalid?.merchantError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("When", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(occurredAt.atZone(zone).format(DateTimeFormatter.ofPattern("EEE, MMM d, h:mm a", Locale.US)), style = MaterialTheme.typography.bodyLarge)
            }
            Row {
                TextButton(onClick = { pickingDate = true }) { Text("Date") }
                TextButton(onClick = { pickingTime = true }) { Text("Time") }
            }
        }

        Text("Category (optional)", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            vm.categories.forEach { c ->
                FilterChip(
                    selected = categoryId == c.id,
                    onClick = { categoryId = if (categoryId == c.id) null else c.id },
                    label = { Text(c.name) },
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().toggleable(value = isRefund, role = Role.Checkbox, onValueChange = { isRefund = it }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = isRefund, onCheckedChange = null)
            Text("This was a refund", modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 12.dp))
        }

        Button(
            onClick = {
                attempted = true
                val ok = check as? FormResult.Valid ?: return@Button
                vm.saveExpense(expenseId, ok.amountCents, isRefund, ok.merchantName, categoryId, occurredAt, reviewId)
                onClose()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Save expense") }

        if (existing != null) {
            TextButton(
                onClick = { vm.deleteExpense(existing.expense.id); onDeleted(); onClose() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Delete expense", color = MaterialTheme.colorScheme.error) }
        }
    }

    if (pickingTime) {
        val current = occurredAt.atZone(zone)
        val state = rememberTimePickerState(initialHour = current.hour, initialMinute = current.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            title = { Text("Time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    occurredAt = current.withHour(state.hour).withMinute(state.minute).withSecond(0).withNano(0).toInstant()
                    pickingTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text("Cancel") } },
        )
    }

    if (pickingDate) {
        val current = occurredAt.atZone(zone)
        val state = rememberDatePickerState(
            initialSelectedDateMillis = current.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val day = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        occurredAt = LocalDateTime.of(day, current.toLocalTime()).atZone(zone).toInstant()
                    }
                    pickingDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}
