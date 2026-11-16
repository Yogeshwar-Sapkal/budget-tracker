package com.yogeshwar.budgettracker.parser

import java.math.BigDecimal

internal object Money {
    /** "2,000.00" -> 200000. */
    fun parseCents(text: String): Long =
        BigDecimal(text.replace(",", "")).movePointRight(2).longValueExact()
}
