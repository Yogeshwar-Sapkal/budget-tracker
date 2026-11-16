package com.yogeshwar.budgettracker.ledger.presentation

import java.util.Locale
import kotlin.math.abs

/** 34331 -> "$343.31", -1000 -> "-$10.00". */
fun formatCents(cents: Long): String {
    val body = String.format(Locale.US, "\$%,d.%02d", abs(cents) / 100, abs(cents) % 100)
    return if (cents < 0) "-$body" else body
}

private val AMOUNT = Regex("""\d+(\.\d{1,2})?|\.\d{1,2}""")

/** Reads what a person types ("12.5", "$1,234.56"). Null unless it is a positive dollar amount. */
fun parseCents(text: String): Long? {
    val t = text.trim().removePrefix("$").replace(",", "")
    if (!AMOUNT.matches(t)) return null
    val cents = java.math.BigDecimal(t).movePointRight(2).toLong()
    return cents.takeIf { it > 0 }
}
