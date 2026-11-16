# UI design: version 1

Based on Steve Krug's usability principles: users should never have to stop and wonder. The visual mockup is in [mockup.html](./mockup.html).

## The one question the app answers

"How much have I spent this month, compared with last month?" It opens on that answer.

## Navigation

Three bottom tabs, each with an icon **and** a text label (no mystery icons):

| Tab | Question it answers |
|---|---|
| This month (home) | How am I doing? |
| Expenses | What exactly did I buy? |
| Merchants | Where does my money go, and what is unsorted? |

Each screen has at most one primary green action: **Add expense** (floating button, with words), **Save expense**, **Review**, **Choose category**, **Open notification settings**. A month switcher (‹ ›) appears on the three tab screens.

## Screens

1. **This month.** Listening-status chip, big total, September-versus-October bars, an amber banner only when something needs the owner, and spending by category.
2. **Expenses.** Search, grouped by date, newest first, tap a row to edit. Possible duplicates carry a badge and a plain-words question with two buttons (Yes, keep both / No, remove one). They count toward the total until resolved.
3. **Merchants.** Merchants without a category sit on top with a "Choose category" button. Below them, merchants ranked by spend.
4. **Add / edit expense.** Amount, Where, When, Category chips, a "This was a refund" option, one Save button.
5. **Needs review.** Shows the original notification text with two clear choices: Ignore it, or Add it as an expense.
6. **First-run setup.** Three numbered steps for granting notification access, including the "Allow restricted settings" workaround for sideloaded apps.

## Rules applied

- **Plain words.** "Spent in October", "Add expense", "Couldn't read this". No jargon like "Transaction", "Ignored" or "Merchant normalization" in the UI.
- **Obvious beats clever.** Standard bottom tabs, a labelled add button, familiar list rows.
- **Scannable.** One big number, two bars, short lists. Amounts right-aligned in tabular digits.
- **Only ask when needed.** The amber banner appears only when there is something to do.
- **Big targets.** All taps are 44px or larger, with the add button in the thumb zone.
- **No colour-only meaning.** Every status chip has words. Light and dark themes are both supported.
- **No confirmation dialogs.** Destructive choices are two labelled buttons inline; edits can be undone from a snackbar.

## Not in version 1

Charts beyond the month comparison, budget limits, goals, alerts, accounts and sync.
