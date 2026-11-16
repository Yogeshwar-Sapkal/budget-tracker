package com.yogeshwar.budgettracker.ledger.presentation

sealed interface FormResult {
    data class Valid(val amountCents: Long, val merchantName: String) : FormResult
    data class Invalid(val amountError: String?, val merchantError: String?) : FormResult
}

fun validateExpenseForm(amountText: String, merchantText: String): FormResult {
    val cents = parseCents(amountText)
    val merchant = merchantText.trim()
    val amountError = if (cents == null) "Enter an amount like 12.50" else null
    val merchantError = if (merchant.isEmpty()) "Enter where you spent it" else null
    return if (cents != null && merchantError == null) FormResult.Valid(cents, merchant)
    else FormResult.Invalid(amountError, merchantError)
}
