# Notification samples

Transcribed from screenshots taken on 2026-10-24 (Samsung, One UI notification shade).
Card and account digits are masked (`••••`); the real notifications show the last 4 digits.
Package names are from memory and must be confirmed on-device from the first captured notifications.

## Sources seen

| Source | Likely package (verify) | Role |
|---|---|---|
| Capital One | `com.konylabs.capitalone` | Card purchase approvals (Savor Credit Card) |
| Chase | `com.chase.sig.android` | Debit/account alerts (threshold-based) |
| Google Wallet | `com.google.android.apps.walletnfcrel` | Tap-to-pay receipt; **duplicates** the Capital One alert |

## S1: Chase, ATM withdrawal (threshold alert)

- Time shown: 13:52
- Title: `Chase`
- Body: `Chase account ••••: Your $35.00 ATM withdrawal on Oct 24, 2026 at 1:52 PM ET was more than the $1.00 limit in your Alerts settings.`
- Observations:
  - No merchant. Transaction kind is "ATM withdrawal".
  - Date and time are in the body, with an `ET` timezone.
  - The alert fires because the amount exceeded a user-set $1.00 alert limit. Other Chase kinds (purchases, deposits) probably use the same "more than the $X limit" template with a different noun. This is unconfirmed; only ATM has been sampled.

## S2: Capital One purchase + Google Wallet receipt

Capital One
- Time shown: 15:34
- Title: `Savor Credit Card...••••`
- Body: `Your purchase for $6.53 at REMI FLOWER & COFF was approved.`

Google Wallet
- Time shown: 15:34
- Title: `SQ *REMI FLOWER & COFF`
- Body: `$6.53 with Capital One Savor MasterCard ••••`

## S3: Capital One purchase + Google Wallet receipt

Capital One
- Time shown: 16:41
- Title: `Savor Credit Card...••••`
- Body: `Your purchase for $14.16 at L&B SPUMONI GARDEN was approved.`

Google Wallet
- Time shown: 16:40
- Title: `TST*L&B SPUMONI GARDEN`
- Body: `$14.16 with Capital One Savor MasterCard ••••`

## S4: Capital One purchase + Google Wallet receipt

Capital One
- Time shown: 19:00
- Title: `Savor Credit Card...••••`
- Body: `Your purchase for $8.15 at HEYTEA GRAND ST was approved.`

Google Wallet
- Time shown: 19:00
- Title: `HEYTEA GRAND ST`
- Body: `$8.15 with Capital One Savor MasterCard ••••`

## S5: Capital One purchase + Google Wallet receipt

Capital One
- Time shown: 19:20
- Title: `Savor Credit Card...••••`
- Body: `Your purchase for $44.64 at MOMMY PAIS was approved.`

Google Wallet
- Time shown: 19:20
- Title: `TST*MOMMY PAIS`
- Body: `$44.64 with Capital One Savor MasterCard ••••`

## Findings that affect design

1. **Every tap-to-pay produces two notifications**, one from Capital One and one from Google Wallet. All four Capital One samples have a matching Wallet notification with the same amount and the same minute (or one minute apart: S3 is 16:40 vs 16:41).
2. **The two sources name the merchant differently.** Wallet gives the raw processor descriptor with a prefix (`SQ *` = Square, `TST*` = Toast). Capital One gives a cleaned, sometimes truncated name (`REMI FLOWER & COFF`). Merchant identity must survive prefixes and truncation.
3. **Capital One has one stable template**: `Your purchase for $<amount> at <merchant> was approved.` The card's last 4 digits are in the title, not the body.
4. **Chase has no merchant**, and carries an explicit date, time and timezone in the body.
5. **Capital One "approved" means authorized, not settled.** We haven't yet seen what happens on a declined purchase, a refund, or a pending charge that later reverses.
6. The notification arrival time (shade timestamp) matches the transaction time within about a minute for these samples.
