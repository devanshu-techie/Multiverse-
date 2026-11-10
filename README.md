# MiniVerse 🎮

One app. Endless mini adventures. — An offline-first Android mini-game collection
built from the **MiniVerse Full-Stack Roadmap** (see `docs/`).

## v0.1 — what works now

- **4 playable games** (pass & play on one phone):
  - ❌ Tic Tac Toe
  - 🔴 Connect Four
  - 🧠 Memory Match (a match keeps your turn)
  - ⚡ Reaction Battle (split-screen, best of 5, seeded delays)
- **Onboarding** — nickname + avatar, 100 MC starter grant
- **MiniCoin wallet** — append-only ledger with unique claim ids
  (`matchId:playerId:reason`), so a reward can never be paid twice
- **Match history** on Home, full coin history in Wallet
- **Save first, animate second** — the result screen only shows values already
  committed to the database in one transaction

## Architecture (matches the roadmap)

```
ui/ feature/*        Compose screens + ViewModels (render state, send events)
engine/              MiniGame contract, GameRegistry, MatchStateMachine
games/*/             Pure-Kotlin rules per game (no Android imports)
data/                Room entities, DAOs, GameRepository (transactions)
```

- The **engine decides everything**; screens never compute winners or coins.
- All randomness enters through the **match seed** (replayable, server-ready).
- Adding a game = one rules file + one board composable + one registry entry.

## Build & run

1. Open the project in **Android Studio** (Koala or newer). Gradle 8.7 /
   AGP 8.5.2 / Kotlin 1.9.24 are configured; Studio downloads them on first sync.
2. Run the `app` configuration on a device or emulator (minSdk 24).
3. Unit tests: `./gradlew :app:testDebugUnitTest` (engine rules + state machine).

> Generated without a compile step in CI yet — if first sync reports a version
> conflict on your Studio version, accept Studio's suggested AGP upgrade.

## Roadmap (from the full PDF)

| Next | Feature |
|---|---|
| v0.2 | Snakes & Ladders, RPS (commit-reveal), achievements, sounds |
| v0.3 | Two-phone play: `MultiplayerTransport` + Wi-Fi Direct / Bluetooth |
| v0.4 | Shop + inventory, Air Hockey (fixed-step physics) |
| v1.5 | Optional cloud: Ktor server, accounts, cloud save, leaderboards |

Reward policy: win **+20 MC**, draw **+10**, loss **+5**; guests on the same
phone play for fun (no wallet) per roadmap decision D5.
