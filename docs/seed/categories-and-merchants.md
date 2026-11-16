# Starter categories and merchants

A starting point only. Categories and merchant assignments can all be changed in the app.

## Categories

A Merchant with no category is shown as "Uncategorized". That is the absence of a category, not a category itself.

| Category | Notes |
|---|---|
| Food & Dining | Restaurants, takeout, pizza, dessert |
| Coffee & Drinks | Cafes, bubble tea, juice |
| Groceries | Supermarkets, corner stores |
| Transport | Transit, rideshare, fuel, parking |
| Shopping | Clothing, electronics, general retail |
| Subscriptions | Streaming, software, memberships |
| Entertainment | Events, games, movies |
| Health | Pharmacy, medical, gym |
| Bills & Utilities | Phone, internet, power, rent |
| Travel | Flights, hotels, trips |
| Cash | ATM withdrawals |
| Other | Anything that fits nowhere else |

## Merchants seen in the sample notifications

| Merchant | Raw descriptors seen | Starting category | Confidence |
|---|---|---|---|
| Remi Flower & Coffee | `REMI FLOWER & COFF`, `SQ *REMI FLOWER & COFF` | Coffee & Drinks | High |
| HeyTea (Grand St) | `HEYTEA GRAND ST` | Coffee & Drinks | High |
| L&B Spumoni Gardens | `L&B SPUMONI GARDEN`, `TST*L&B SPUMONI GARDEN` | Food & Dining | High |
| Mommy Pais | `MOMMY PAIS`, `TST*MOMMY PAIS` | Food & Dining | Guess. Confirm what it is. |
| Cash (ATM) | Chase ATM withdrawal alert has no merchant | Cash | High |
| H&M | `H&M` | Shopping | High |
| Walmart | `Walmart2000153590447` (trailing store id varies) | Shopping | Guess. Could be Groceries. |
| Food Story Natural Market | `FOOD STORY NATURAL MAR` | Groceries | High |
| PATH | `PATH TAPP PAYGO CP` | Transport | High |

## Ignored Transactions seen

| What | Example | Why |
|---|---|---|
| External transfer to own brokerage | `external transfer to FIDELITY CMA XXXXXXX` | Not spending |
