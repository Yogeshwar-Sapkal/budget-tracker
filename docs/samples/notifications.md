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

- Date/time shown: Sat Oct 24, 13:52
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

## S6: Capital One purchase, no Wallet receipt

- Date/time shown: Sun Oct 25, 16:23
- Title: `Savor Credit Card...••••`
- Body: `Your purchase for $193.50 at H&M was approved.`
- Not a tap-to-pay, so there is no Google Wallet duplicate.

## S7: Chase credit card purchase

- Date/time shown: Mon Oct 26, 14:24
- Title: `Chase`
- Body: `Chase Freedom Unlimited Visa: You made a $14.68 transaction with Walmart2000153590447 on Oct 26, 2026 at 2:24 PM ET.`
- The merchant carries a long numeric suffix (`Walmart` + store/terminal id).

## S8: Capital One purchase, no Wallet receipt

- Date/time shown: Tue Oct 27, 18:11
- Title: `Savor Credit Card...••••`
- Body: `Your purchase for $26.65 at FOOD STORY NATURAL MAR was approved.`
- Merchant is truncated (`NATURAL MAR`).

## S9: Chase, external transfer (threshold alert)

- Date/time shown: Wed Oct 28, 05:21
- Title: `Chase`
- Body: `Chase account ••••: Your $2,000.00 external transfer to FIDELITY CMA XXXXXXX on Oct 28, 2026 at 5:21 AM ET was more than the $1.00 limit in your Alerts settings.`
- Same template as S1 with a different transaction kind. The destination is the owner's own brokerage account, so this is an Ignored Transaction.
- The same screenshot also shows a WhatsApp "Backup in progress" notification. It is unrelated noise from a non-source app.

## S10: Chase credit card purchase, delivered twice

- Date/time shown: Mon Nov 2, 17:48 (two identical notifications)
- Title: `Chase`
- Body (both): `Chase Freedom Unlimited Visa: You made a $3.25 transaction with PATH TAPP PAYGO CP on Nov 2, 2026 at 5:48 PM ET.`
- Samsung groups the two under a "Chase" group header. They have identical text and the same minute, so it is unknown whether this is one fare delivered twice or two real fares.

## Findings that affect design

1. **Capital One tap-to-pay also fires a Google Wallet notification** (S2 to S5). Wallet is ignored (decided), because Capital One reports every purchase. Capital One purchases that were not tap-to-pay (S6, S8) show no Wallet notification at all.
2. **Chase uses two templates** (and Capital One one):
   - Card transaction: `Chase <card name>: You made a $<amount> transaction with <merchant> on <date> at <time> ET.` (S7, S10). Includes card name, merchant, date, time, timezone.
   - Threshold alert: `Chase account <last4>: Your $<amount> <kind> [to <destination>] on <date> at <time> ET was more than the $<limit> limit in your Alerts settings.` (S1, S9). `<kind>` seen so far: `ATM withdrawal`, `external transfer`.
   - Capital One: `Your purchase for $<amount> at <merchant> was approved.` with the card in the title.
3. **Merchant text is messy.** Prefixes (`SQ *`, `TST*`), truncation (`COFF`, `MAR`, `GARDEN`) and numeric suffixes (`Walmart2000153590447`) all occur. Matching needs normalization, backed by manual correction.
4. **Chase threshold alerts depend on the owner's own $1.00 alert limit.** Charges under that limit won't alert, and changing the limit changes what is captured.
5. **Chase notifications can arrive grouped and duplicated** (S10). A group summary notification could also be delivered to the listener and must not be counted as an extra Transaction.
6. **Capital One "approved" means authorized, not settled.** Declines, refunds and reversals have not been sampled.
7. **Capital One bodies carry no date or time**, so the arrival time is the only timestamp. Chase bodies carry an explicit ET timestamp.
8. **Unsampled kinds** (Chase deposits and purchases from the checking account, refunds, card payments, declines) will land in Needs Review in version 1.
