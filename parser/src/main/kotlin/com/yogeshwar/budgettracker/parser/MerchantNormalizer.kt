package com.yogeshwar.budgettracker.parser

data class NormalizedMerchant(
    /** Cleaned text with the original casing, e.g. "REMI FLOWER & COFF". */
    val display: String,
    /** Lower-case matching key. Two notifications for the same merchant share a key. */
    val key: String,
)

object MerchantNormalizer {
    /** Processor prefixes such as "SQ *" (Square) and "TST*" (Toast). */
    private val prefix = Regex("""^[A-Za-z]{2,4}\s?\*\s*""")

    /** Store or terminal numbers glued to the end: "Walmart2000153590447", "WAL-MART #1234". */
    private val trailingStoreNumber = Regex("""[\s#]*\d{4,}$""")

    private val whitespace = Regex("""\s+""")

    fun normalize(raw: String): NormalizedMerchant {
        val display = raw
            .trim()
            .replace(prefix, "")
            .replace(trailingStoreNumber, "")
            .replace(whitespace, " ")
            .trim()
        return NormalizedMerchant(display = display, key = display.lowercase())
    }
}
