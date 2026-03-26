# Club Darts — AI Agent Guide

Android darts scoring app built with **Kotlin · Jetpack Compose · Room · Hilt**.
This file exists so AI agents (and new contributors) can orient themselves quickly without reading every source file.

---

## Build & Test Commands

```bash
# Compile debug APK
./gradlew assembleDebug

# Run all unit tests (JVM, no device needed)
./gradlew test

# Unit tests + enforce ≥80% JaCoCo coverage
./gradlew jacocoUnitTestReport

# Run E2E / instrumented tests (requires connected device or emulator)
./gradlew connectedDebugAndroidTest

# Run a single E2E test class
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.clubdarts.e2e.StandardWorkflowTest
```

> The 80% coverage threshold is enforced by JaCoCo; it excludes UI composables, DI modules,
> Room-generated code, data models, and Android entry points.

---

## Architecture

```
┌─────────────────────────────────────┐
│  Jetpack Compose UI                 │  *Screen.kt  (no business logic)
│    NavGraph  BottomNav              │
└──────────────┬──────────────────────┘
               │ collectAsStateWithLifecycle()
               ▼
┌─────────────────────────────────────┐
│  ViewModel  (MVVM)                  │  *ViewModel.kt
│    MutableStateFlow<UiState>        │  Hilt @HiltViewModel
│    viewModelScope coroutines        │
└──────┬───────────────────┬──────────┘
       │ suspend / Flow    │
       ▼                   ▼
┌─────────────┐   ┌────────────────────┐
│ Repositories│   │  Utility objects   │
│  (Singleton)│   │  ScoringEngine     │
│  Game       │   │  CheckoutCalculator│
│  Player     │   │  EloCalculator     │
│  Settings   │   │  TtsManager        │
│  Elo        │   │  SoundEffectsService│
└──────┬──────┘   └────────────────────┘
       │ Room DAOs
       ▼
┌─────────────────────────────────────┐
│  Room Database  (AppDatabase)       │
│  Tables: Player, Game, GamePlayer,  │
│          Leg, Throw, AppSettings,   │
│          EloMatch, EloMatchEntry    │
└─────────────────────────────────────┘
```

**Dependency injection:** Hilt — `di/DatabaseModule.kt` provides DAOs, `di/RepositoryModule.kt` provides repositories.

---

## Key File Map

| File | Purpose |
|------|---------|
| `ui/game/GameViewModel.kt` | Owns the entire game lifecycle across SETUP → LIVE → RESULT screens. Largest file (855 lines). |
| `ui/game/GameSetupScreen.kt` | Compose screen for configuring a new game (players, rules, ranked toggle). |
| `ui/game/LiveGameScreen.kt` | Active game screen: numpad, player strip, visit history. |
| `ui/game/GameResultScreen.kt` | Winner announcement, save / discard / repeat actions. |
| `util/ScoringEngine.kt` | Pure bust-detection and visit-resolution logic. Well-tested, no Android deps. |
| `util/CheckoutCalculator.kt` | Checkout hints and checkout-validity checks. Contains a manually-verified 140-entry lookup table. |
| `util/EloCalculator.kt` | Pairwise Elo rating maths (stateless). |
| `data/repository/GameRepository.kt` | Game, Leg, and Throw persistence; `GameConfig` data class lives here. |
| `data/repository/PlayerRepository.kt` | Player CRUD. |
| `data/repository/SettingsRepository.kt` | Key-value settings + TTS phrase serialisation (JSON). |
| `data/repository/EloRepository.kt` | Ranked match recording and undo, atomic Room transactions. |
| `data/db/AppDatabase.kt` | Room DB class; schema versions 1–8 with explicit migrations. |
| `ui/navigation/NavGraph.kt` | Navigation graph wiring all top-level routes. |
| `ui/theme/` | Material 3 colours, typography, theme composition. |

---

## Game Flow State Machine

The single `GameViewModel` drives a three-screen flow encoded in `GameScreen` enum:

```
                 startGame(config)
  ┌──────────┐ ──────────────────► ┌──────────┐
  │  SETUP   │                     │   LIVE   │
  └──────────┘ ◄────────────────── └──────────┘
       ▲          abortGame()            │
       │                                 │ onLegWon() — enough legs won
       │                                 ▼
       │                          ┌──────────────┐
       │   discardGame()          │    RESULT    │
       └──────────────────────────└──────────────┘
                                       │
                                       │ repeatGame() → back to SETUP
                                       │   (resets state, preserves player selection)
                                       │ saveGame() / discardGame() → back to SETUP
                                       │ undoLastThrowOnResult() → back to LIVE
```

- **SETUP → LIVE**: `startGame(GameConfig)` creates DB records for Game + first Leg.
- **LIVE → RESULT**: triggered inside `onLegWon()` when `legsWon >= config.legsToWin`. For ranked single-player games, Elo is recorded atomically here.
- **RESULT → LIVE**: `undoLastThrowOnResult()` reverts the checkout throw (and Elo if ranked) and returns to LIVE.
- **RESULT → SETUP**: `repeatGame()` (re-uses same players/config) or `discardGame()`.

---

## Domain Glossary

| Term | Meaning |
|------|---------|
| **visit** | A player's turn: 1–3 darts, totalled as one score deduction. |
| **leg** | A single game of X01. A match can be best-of-N legs. |
| **bust** | A visit that would reduce the score below 0 (or to 1 for Double/Triple out). Score is not changed. |
| **checkout** | A visit that reduces the score to exactly 0 with a valid finishing dart. |
| **Double Out** | Standard rule: the final dart must land on a double segment. |
| **Triple Out** | Final dart must land on a triple. |
| **Straight Out** | Any dart can finish. |
| **D / T notation** | `D14` = double 14 (28 pts), `T20` = triple 20 (60 pts), `Bull` = bullseye (50 pts). |
| **Elo** | Rating system (default K-factor 32). New players start at 1000. Leaderboard requires ≥5 matches. |
| **ranked game** | A single-player-mode game with the ranked toggle on. Elo changes are recorded; game is auto-saved. |

---

## Naming Conventions

- `*Screen.kt` — top-level `@Composable` that collects ViewModel state. No business logic.
- `*ViewModel.kt` — MVVM ViewModel; private `_uiState: MutableStateFlow<*UiState>`, public `uiState: StateFlow<*UiState>`.
- `*Repository.kt` — data-layer singleton; `Flow<T>` for observations, `suspend fun` for mutations.
- `*Dao.kt` — Room DAO interface under `data/db/dao/`.
- `*UiState` — immutable data class; update via `_uiState.update { it.copy(...) }`.
- Hilt entry points: `@HiltViewModel` on ViewModels, `@Singleton` on repositories.

---

## Common Task Guides

### Add a new setting

1. Add key constant to `data/model/SettingsKeys` (string constant object).
2. Add default to `data/model/SettingsDefaults`.
3. Add `get*` / `set*` (and `observe*` if reactive) methods to `SettingsRepository`.
4. Expose the value from the relevant ViewModel, then use it in the Compose screen.
5. Add a UI control in the appropriate settings screen (`GeneralSettingsScreen`, `RankingSettingsScreen`, or `TtsSettingsScreen`).

### Modify scoring / bust logic

- All bust and checkout validation lives in `util/ScoringEngine.kt` and `util/CheckoutCalculator.kt`.
- Both are pure Kotlin objects — edit and run `./gradlew test` to verify against the existing unit test suites.

### Add a game statistic

1. Add a query to the appropriate DAO (`ThrowDao`, `GameDao`, or `LegDao`).
2. Expose it via `GameRepository` or `PlayerRepository`.
3. Compute/aggregate in `StatsViewModel`.
4. Render in `StatsScreen`.

### Add a new Elo / ranking rule

1. Adjust the algorithm in `util/EloCalculator.kt` (pure, unit-tested).
2. Expose the new config value through `SettingsRepository` + `RankingSettingsScreen`.
3. Pass the value into `EloRepository.recordMatch()`.

---

## Test Layout

```
app/src/test/                         ← JVM unit tests (no device)
  util/ScoringEngineTest.kt
  util/CheckoutCalculatorTest.kt
  util/EloCalculatorTest.kt
  data/repository/PlayerRepositoryTest.kt
  data/repository/SettingsRepositoryTest.kt
  data/repository/EloRepositoryUndoTest.kt

app/src/androidTest/                  ← Instrumented / E2E (requires device)
  e2e/StandardWorkflowTest.kt         ← Full x01 game: add players → setup → play → save
  e2e/UndoLastThrowTest.kt            ← Undo mechanics
  di/TestDatabaseModule.kt            ← In-memory Room DB override
  HiltTestRunner.kt
```
