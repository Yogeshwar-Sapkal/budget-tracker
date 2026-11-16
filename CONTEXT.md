# Budget Tracker

A personal app that turns bank and card notifications on one phone into a record of spending, so the owner can see how much was spent this month and last month and cut useless spending.

## Language

**Notification Source**:
A bank app whose notifications the tracker reads. Currently Chase and Capital One.
_Avoid_: Provider, integration, bank feed

**Transaction**:
Any money movement a Notification Source reports, whether or not it counts toward spending.
_Avoid_: Payment, charge, entry

**Expense**:
A Transaction that counts toward spending: a purchase, a tap-to-pay purchase, a recurring charge, or an ATM withdrawal. A refund is a negative Expense.
_Avoid_: Spend, purchase, cost

**Ignored Transaction**:
A Transaction that is deliberately not an Expense, such as a credit card bill payment or a transfer between the owner's own accounts.
_Avoid_: Skipped, filtered

**Merchant**:
The place where an Expense was made, shown under one cleaned display name. An ATM withdrawal has the Merchant "Cash".
_Avoid_: Vendor, payee, store, place

**Raw Descriptor**:
The merchant text exactly as a Notification Source reports it, such as `TST*MOMMY PAIS`. A Merchant can have many Raw Descriptors.
_Avoid_: Alias, merchant string

**Category**:
A group of Merchants for budgeting, such as Coffee & Drinks. The owner assigns a Category to a Merchant once, and it applies to all of that Merchant's Expenses. Every new Merchant starts Uncategorized.
_Avoid_: Tag, type, bucket

**Needs Review**:
The holding list for notifications the tracker could not read as a Transaction. Nothing is silently dropped.
_Avoid_: Errors, failed, unparsed

**Month**:
A calendar month in the phone's local timezone. An Expense belongs to the month of the notification's arrival time, which the owner can edit.
_Avoid_: Billing cycle, statement period
