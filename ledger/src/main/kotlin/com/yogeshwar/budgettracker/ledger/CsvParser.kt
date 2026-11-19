package com.yogeshwar.budgettracker.ledger

/** Reads CSV (RFC 4180): quoted fields, doubled quotes, line breaks inside quotes, CRLF or LF. */
internal object CsvParser {
    fun parse(input: String): List<List<String>> {
        val text = input.removePrefix("﻿")
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var started = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') { field.append('"'); i++ } else inQuotes = false
                } else field.append(c)
            } else {
                when (c) {
                    '"' -> { inQuotes = true; started = true }
                    ',' -> { row.add(field.toString()); field.clear(); started = true }
                    '\r' -> {}
                    '\n' -> { row.add(field.toString()); field.clear(); rows.add(row); row = mutableListOf(); started = false }
                    else -> { field.append(c); started = true }
                }
            }
            i++
        }
        if (started || field.isNotEmpty() || row.isNotEmpty()) { row.add(field.toString()); rows.add(row) }
        return rows.filterNot { it.size == 1 && it[0].isEmpty() }
    }
}
