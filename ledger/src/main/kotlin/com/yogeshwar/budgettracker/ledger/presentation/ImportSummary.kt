package com.yogeshwar.budgettracker.ledger.presentation

import com.yogeshwar.budgettracker.ledger.ImportResult

/** One plain sentence about what an import did. */
fun ImportResult.summaryText(): String {
    problem?.let { return it }
    if (imported == 0 && invalid == 0) {
        return if (alreadyThere == 0) "That file had no expenses."
        else "Nothing new: all $alreadyThere ${plural(alreadyThere, "expense")} were already here."
    }
    return buildString {
        append("Imported $imported ${plural(imported, "expense")}")
        if (alreadyThere > 0) append(", $alreadyThere already here")
        if (invalid > 0) append(", $invalid ${plural(invalid, "row")} skipped")
    }
}

private fun plural(n: Int, word: String) = if (n == 1) word else word + "s"
