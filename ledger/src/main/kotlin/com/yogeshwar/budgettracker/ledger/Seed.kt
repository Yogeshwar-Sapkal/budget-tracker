package com.yogeshwar.budgettracker.ledger

/** Starter data from docs/seed/categories-and-merchants.md. Everything here can be changed in the app. */
internal object Seed {
    /** "Uncategorized" is not stored: it simply means a Merchant has no category yet. */
    val categories = listOf(
        "Food & Dining", "Coffee & Drinks", "Groceries", "Transport", "Shopping", "Subscriptions",
        "Entertainment", "Health", "Bills & Utilities", "Travel", "Cash", "Other",
    )

    class SeedMerchant(val rawNames: List<String>, val displayName: String, val category: String)

    val merchants = listOf(
        SeedMerchant(listOf("REMI FLOWER & COFF"), "Remi Flower & Coffee", "Coffee & Drinks"),
        SeedMerchant(listOf("HEYTEA GRAND ST"), "HeyTea", "Coffee & Drinks"),
        SeedMerchant(listOf("L&B SPUMONI GARDEN"), "L&B Spumoni Gardens", "Food & Dining"),
        SeedMerchant(listOf("MOMMY PAIS"), "Mommy Pais", "Food & Dining"),
        SeedMerchant(listOf("H&M"), "H&M", "Shopping"),
        SeedMerchant(listOf("Walmart"), "Walmart", "Shopping"),
        SeedMerchant(listOf("FOOD STORY NATURAL MAR"), "Food Story Natural Market", "Groceries"),
        SeedMerchant(listOf("PATH TAPP PAYGO CP"), "PATH", "Transport"),
        SeedMerchant(listOf(Ledger.CASH_MERCHANT), "Cash", "Cash"),
    )
}
