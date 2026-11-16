# Budget Tracker

A personal Android app that turns Chase and Capital One notifications into a record of spending.
See [CONTEXT.md](CONTEXT.md) for the vocabulary, [docs/adr](docs/adr) for decisions,
[docs/samples](docs/samples/notifications.md) for real notification text, and
[docs/ui](docs/ui/design.md) for the screen design.

## Layout

| Module | What it is | Needs |
|---|---|---|
| `parser` | Pure Kotlin. Notification text in, Expense / Ignored / Needs Review out. Fully tested against the real samples. | JDK 17+ |
| `ledger` | Pure Kotlin. The rules: saving expenses, merchants and categories, duplicate flagging, month totals. Tested with an in-memory store. | JDK 17+ |
| `app` | Android app. The notification listener, a Room database behind the ledger, and a placeholder screen. | Android SDK |

## Run the tests

```bash
./gradlew :parser:test :ledger:test
```

## Build the app

Set `ANDROID_HOME` (or open the folder in Android Studio, which writes `local.properties`), then:

```bash
./gradlew :app:assembleDebug
```

Install on the phone and, in Android settings, allow notification access. If the toggle is greyed out,
open Settings, Apps, Budget Tracker, the three-dot menu, and choose "Allow restricted settings".
