# Club Darts — Claude Code Build Instructions

You are building a complete Android app called **Club Darts** from scratch.
Work through every phase in order. Do not stop between phases unless there is a
compile error. After every phase, verify the project compiles before moving on.

---

## Project bootstrap

Create a new Android project with the following settings:
- Package name: `com.clubdarts`
- Min SDK: 26 (Android 8)
- Target SDK: 35
- Language: Kotlin
- Build system: Gradle with Kotlin DSL (`.kts` files)
- No initial activity template — you will create everything manually

---

## Tech stack (exact versions)

Use a `gradle/libs.versions.toml` version catalog with these entries:

```toml
[versions]
agp                     = "8.4.0"
kotlin                  = "2.0.0"
ksp                     = "2.0.0-1.0.21"
coreKtx                 = "1.13.1"
activityCompose         = "1.9.0"
composeBom              = "2024.06.00"
navigationCompose       = "2.7.7"
room                    = "2.6.1"
hilt                    = "2.51.1"
hiltNavigationCompose   = "1.2.0"
coroutines              = "1.8.1"
lifecycle               = "2.8.2"

[libraries]
androidx-core-ktx                   = { group = "androidx.core",             name = "core-ktx",                         version.ref = "coreKtx" }
androidx-activity-compose           = { group = "androidx.activity",          name = "activity-compose",                 version.ref = "activityCompose" }
androidx-compose-bom                = { group = "androidx.compose",           name = "compose-bom",                      version.ref = "composeBom" }
androidx-compose-ui                 = { group = "androidx.compose.ui",        name = "ui" }
androidx-compose-ui-tooling         = { group = "androidx.compose.ui",        name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui",        name = "ui-tooling-preview" }
androidx-compose-material3          = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons     = { group = "androidx.compose.material",  name = "material-icons-extended" }
androidx-navigation-compose         = { group = "androidx.navigation",        name = "navigation-compose",               version.ref = "navigationCompose" }
androidx-room-runtime               = { group = "androidx.room",              name = "room-runtime",                     version.ref = "room" }
androidx-room-ktx                   = { group = "androidx.room",              name = "room-ktx",                         version.ref = "room" }
androidx-room-compiler              = { group = "androidx.room",              name = "room-compiler",                    version.ref = "room" }
androidx-lifecycle-viewmodel        = { group = "androidx.lifecycle",         name = "lifecycle-viewmodel-ktx",          version.ref = "lifecycle" }
androidx-lifecycle-runtime          = { group = "androidx.lifecycle",         name = "lifecycle-runtime-ktx",            version.ref = "lifecycle" }
androidx-lifecycle-compose          = { group = "androidx.lifecycle",         name = "lifecycle-runtime-compose",        version.ref = "lifecycle" }
hilt-android                        = { group = "com.google.dagger",          name = "hilt-android",                     version.ref = "hilt" }
hilt-compiler                       = { group = "com.google.dagger",          name = "hilt-android-compiler",            version.ref = "hilt" }
hilt-navigation-compose             = { group = "androidx.hilt",              name = "hilt-navigation-compose",          version.ref = "hiltNavigationCompose" }
kotlinx-coroutines-android          = { group = "org.jetbrains.kotlinx",     name = "kotlinx-coroutines-android",       version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application",             version.ref = "agp" }
kotlin-android      = { id = "org.jetbrains.kotlin.android",        version.ref = "kotlin" }
kotlin-compose      = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp                 = { id = "com.google.devtools.ksp",              version.ref = "ksp" }
hilt                = { id = "com.google.dagger.hilt.android",       version.ref = "hilt" }
```

---

## AndroidManifest.xml requirements

- Lock orientation to portrait: `android:screenOrientation="portrait"` on the activity
- Application class: `com.clubdarts.ClubDartsApp`
- Single activity: `com.clubdarts.MainActivity`
- No special permissions needed (TTS and Room need none)

---

## Full file structure to create

```
app/src/main/java/com/clubdarts/
├── ClubDartsApp.kt
├── MainActivity.kt
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── Converters.kt
│   │   └── dao/
│   │       ├── PlayerDao.kt
│   │       ├── GameDao.kt
│   │       ├── LegDao.kt
│   │       ├── ThrowDao.kt
│   │       └── AppSettingsDao.kt
│   ├── model/
│   │   ├── Player.kt
│   │   ├── Game.kt
│   │   ├── GamePlayer.kt
│   │   ├── Leg.kt
│   │   ├── Throw.kt
│   │   └── AppSettings.kt
│   └── repository/
│       ├── PlayerRepository.kt
│       ├── GameRepository.kt
│       └── SettingsRepository.kt
├── di/
│   ├── DatabaseModule.kt
│   └── RepositoryModule.kt
├── ui/
│   ├── navigation/
│   │   ├── NavGraph.kt
│   │   └── BottomNav.kt
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Type.kt
│   ├── game/
│   │   ├── GameViewModel.kt
│   │   ├── GameSetupScreen.kt
│   │   ├── LiveGameScreen.kt
│   │   ├── GameResultScreen.kt
│   │   └── components/
│   │       ├── PlayerStrip.kt
│   │       ├── DartNumpad.kt
│   │       ├── VisitHistory.kt
│   │       └── PlayerPickerSheet.kt
│   ├── stats/
│   │   ├── StatsViewModel.kt
│   │   └── StatsScreen.kt
│   ├── history/
│   │   ├── HistoryViewModel.kt
│   │   ├── HistoryScreen.kt
│   │   └── MatchDetailScreen.kt
│   └── players/
│       ├── PlayersViewModel.kt
│       └── PlayersScreen.kt
└── util/
    ├── CheckoutCalculator.kt
    └── TtsManager.kt
```

---

## Data models

### Player.kt
```kotlin
@Entity(tableName = "players")
data class Player(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

### Game.kt
```kotlin
enum class CheckoutRule { STRAIGHT, DOUBLE, TRIPLE }

@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startScore: Int,
    val checkoutRule: CheckoutRule,
    val legsToWin: Int,
    val isSolo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val winnerId: Long? = null
)
```

### GamePlayer.kt
Join table linking players to a game with their throw order (0-based).
```kotlin
@Entity(
    tableName = "game_players",
    primaryKeys = ["gameId", "playerId"],
    foreignKeys = [
        ForeignKey(Game::class,   ["id"], ["gameId"],   onDelete = CASCADE),
        ForeignKey(Player::class, ["id"], ["playerId"], onDelete = CASCADE)
    ]
)
data class GamePlayer(val gameId: Long, val playerId: Long, val throwOrder: Int)
```

### Leg.kt
```kotlin
@Entity(tableName = "legs", foreignKeys = [ForeignKey(Game::class, ["id"], ["gameId"], onDelete = CASCADE)])
data class Leg(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val legNumber: Int,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val winnerId: Long? = null
)
```

### Throw.kt
One row = one visit (up to 3 darts).
- `dartNScore`: the number hit (1–25). 0 = miss.
- `dartNMult`: 1 = single, 2 = double, 3 = triple. 0 if dart not thrown.
- `dartsUsed`: how many darts thrown this visit (1–3). Can be < 3 on checkout.
- `isBust`: visit was invalid, score reverts, turn ends.
- `isCheckoutAttempt`: score at visit start was ≤ 170. Used for checkout %.
```kotlin
@Entity(
    tableName = "throws",
    foreignKeys = [
        ForeignKey(Leg::class,    ["id"], ["legId"],    onDelete = CASCADE),
        ForeignKey(Player::class, ["id"], ["playerId"], onDelete = CASCADE)
    ]
)
data class Throw(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val legId: Long,
    val playerId: Long,
    val visitNumber: Int,
    val dart1Score: Int = 0, val dart1Mult: Int = 0,
    val dart2Score: Int = 0, val dart2Mult: Int = 0,
    val dart3Score: Int = 0, val dart3Mult: Int = 0,
    val dartsUsed: Int,
    val visitTotal: Int,
    val isBust: Boolean = false,
    val isCheckoutAttempt: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

### AppSettings.kt
Key/value store.
```kotlin
@Entity(tableName = "app_settings")
data class AppSettings(@PrimaryKey val key: String, val value: String)

object SettingsKeys {
    const val LAST_START_SCORE   = "last_start_score"
    const val LAST_CHECKOUT_RULE = "last_checkout_rule"
    const val LAST_LEGS_TO_WIN   = "last_legs_to_win"
    const val LAST_RANDOM_ORDER  = "last_random_order"
    const val RECENT_PLAYER_IDS  = "recent_player_ids"  // comma-separated, max 5
}

object SettingsDefaults {
    const val START_SCORE   = "501"
    const val CHECKOUT_RULE = "DOUBLE"
    const val LEGS_TO_WIN   = "1"
    const val RANDOM_ORDER  = "false"
    const val RECENT_IDS    = ""
}
```

---

## Database

### Converters.kt
Write a `Converters` class with `@TypeConverter` methods for:
- `CheckoutRule` ↔ `String`
- `Long?` ↔ `Long` (nullable timestamp)

### AppDatabase.kt
```kotlin
@Database(
    entities = [Player::class, Game::class, GamePlayer::class,
                Leg::class, Throw::class, AppSettings::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun gameDao(): GameDao
    abstract fun legDao(): LegDao
    abstract fun throwDao(): ThrowDao
    abstract fun appSettingsDao(): AppSettingsDao
}
```

---

## DAOs — implement all of these methods

### PlayerDao
- `getAllPlayers(): Flow<List<Player>>` — ORDER BY name ASC
- `getPlayersByIds(ids: List<Long>): List<Player>`
- `getPlayerById(id: Long): Player?`
- `insertPlayer(player: Player): Long`
- `updatePlayer(player: Player)`
- `deletePlayer(player: Player)`
- `getPlayerCount(): Int`

### GameDao
- `insertGame(game: Game): Long`
- `updateGame(game: Game)`
- `insertGamePlayers(list: List<GamePlayer>)`
- `getAllGames(): Flow<List<Game>>` — ORDER BY createdAt DESC
- `getGameById(id: Long): Game?`
- `getActiveGame(): Game?` — winnerId IS NULL, most recent
- `getGamePlayers(gameId: Long): List<GamePlayer>` — ORDER BY throwOrder ASC
- `deleteGamePlayers(gameId: Long)`

### LegDao
- `insertLeg(leg: Leg): Long`
- `updateLeg(leg: Leg)`
- `getLegsForGame(gameId: Long): List<Leg>`
- `getLegsForGameFlow(gameId: Long): Flow<List<Leg>>`
- `getActiveLeg(gameId: Long): Leg?` — winnerId IS NULL
- `getLegById(id: Long): Leg?`

### ThrowDao
- `insertThrow(throw_: Throw): Long`
- `deleteThrow(throw_: Throw)`
- `getThrowsForLeg(legId: Long): List<Throw>`
- `getThrowsForLegFlow(legId: Long): Flow<List<Throw>>`
- `getThrowsForPlayerInLeg(legId: Long, playerId: Long): List<Throw>`
- `getLastThrowInLeg(legId: Long): Throw?` — for undo
- `getAverageForPlayer(playerId: Long): Double?` — excludes busts
- `getHighestFinishForPlayer(playerId: Long): Int?` — checkout attempts only, not bust
- `get180sForPlayer(playerId: Long): Int`
- `getHundredPlusForPlayer(playerId: Long): Int`
- `getCheckoutAttemptsForPlayer(playerId: Long): Int`
- `getSuccessfulCheckoutsForPlayer(playerId: Long): Int` — JOIN with legs WHERE legs.winnerId = playerId
- `getVisitScoreFrequencyForPlayer(playerId: Long): List<ScoreFrequency>` — top 10, GROUP BY visitTotal
- Bucket queries: `getBucketHigh` (100–180), `getBucketMid` (60–99), `getBucketLow` (40–59), `getBucketVeryLow` (1–39), `getBucketBusts`
- `getTotalClub180s(): Int`
- `getClubHighestFinish(): Int?`

Also create `data class ScoreFrequency(val visitTotal: Int, val frequency: Int)` in this file.

### AppSettingsDao
- `get(key: String): AppSettings?`
- `observe(key: String): Flow<AppSettings?>`
- `set(setting: AppSettings)` — INSERT OR REPLACE
- `set(key: String, value: String)` — convenience wrapper
- `delete(key: String)`

---

## Hilt dependency injection

### DatabaseModule.kt
`@Module @InstallIn(SingletonComponent::class)` — provide:
- `AppDatabase` as singleton using `Room.databaseBuilder(context, AppDatabase::class.java, "clubdarts.db").build()`
- All 5 DAOs extracted from the database instance

### RepositoryModule.kt
`@Module @InstallIn(SingletonComponent::class)` — bind all 3 repositories as singletons.

### ClubDartsApp.kt
```kotlin
@HiltAndroidApp
class ClubDartsApp : Application()
```

### MainActivity.kt
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClubDartsTheme {
                ClubDartsNavHost()
            }
        }
    }
}
```

---

## Theme

### Color.kt
Define these exact colors as Compose `Color` objects:
```
Background:   #0E0F11
Surface:      #16181C
Surface2:     #1E2026
Surface3:     #262930
Accent:       #E8FF47   (acid yellow — primary brand color)
AccentDim:    #E8FF47 at 12% alpha
Green:        #3DDB7A
Red:          #FF5C5C
Amber:        #FFB547
Blue:         #5B9EFF
TextPrimary:  #F0F0EE
TextSecondary:#9A9B9F
TextTertiary: #5C5D62
Border:       white at 7% alpha
Border2:      white at 12% alpha
```

### Type.kt
Use **DM Sans** for all body/UI text and **DM Mono** for all numbers, scores,
dart notation (T20, D16), and timestamps. Add Google Fonts dependency or bundle
the fonts in `res/font/`. Define a `Typography` object with appropriate styles.

### Theme.kt
Dark theme only. No light theme. Wrap content in a `MaterialTheme` using the
colors and typography defined above. Set `Surface` color as the window background.

---

## Navigation

### BottomNav.kt
Four tabs. Use `NavigationBar` + `NavigationBarItem` from Material3:
1. **Game** — dartboard circle icon
2. **Stats** — grid/bar chart icon
3. **History** — clock icon
4. **Players** — person group icon

Active tab: accent color (#E8FF47). Inactive: TextTertiary. Small accent dot
indicator beneath active tab label.

### NavGraph.kt
`@Composable fun ClubDartsNavHost()` — uses `rememberNavController()` and
`NavHost`. Routes:
- `game/setup` — GameSetupScreen (default start destination)
- `game/live` — LiveGameScreen
- `game/result` — GameResultScreen
- `stats` — StatsScreen
- `history` — HistoryScreen
- `history/detail/{gameId}` — MatchDetailScreen(gameId)
- `players` — PlayersScreen

The bottom nav is always visible. Navigating to `game/live` or `game/result`
keeps the bottom nav visible but the active tab stays on Game.

---

## Repositories

### SettingsRepository.kt
Inject `AppSettingsDao`. Provide:
- `suspend fun get(key: String, default: String): String`
- `suspend fun set(key: String, value: String)`
- `fun observe(key: String, default: String): Flow<String>`
- Helper methods for typed access:
  - `suspend fun getLastStartScore(): Int`
  - `suspend fun getLastCheckoutRule(): CheckoutRule`
  - `suspend fun getLastLegsToWin(): Int`
  - `suspend fun getLastRandomOrder(): Boolean`
  - `suspend fun getRecentPlayerIds(): List<Long>`
  - `suspend fun setLastGameConfig(score: Int, rule: CheckoutRule, legs: Int, random: Boolean)`
  - `suspend fun addRecentPlayer(playerId: Long)` — prepend to list, deduplicate, keep max 5

### PlayerRepository.kt
Inject `PlayerDao`. Thin wrapper — expose all DAO methods as repository methods.
Add `suspend fun getRecentPlayers(ids: List<Long>): List<Player>` that preserves
the order of the id list (recent-first).

### GameRepository.kt
Inject `GameDao`, `LegDao`, `ThrowDao`. Expose all DAO methods. Also provide
these higher-level operations:
- `suspend fun startGame(config: GameConfig): Long` — inserts Game + GamePlayers, creates first Leg, returns gameId
- `suspend fun finishLeg(legId: Long, winnerId: Long)` — sets winnerId + finishedAt on leg
- `suspend fun finishGame(gameId: Long, winnerId: Long)` — sets winnerId + finishedAt on game
- `suspend fun getFullGameDetail(gameId: Long): GameDetail?` — returns game + players + legs + throws

Define these data classes in the repository file or a separate `GameModels.kt`:
```kotlin
data class GameConfig(
    val startScore: Int,
    val checkoutRule: CheckoutRule,
    val legsToWin: Int,
    val isSolo: Boolean,
    val playerIds: List<Long>   // in throw order
)

data class GameDetail(
    val game: Game,
    val players: List<Player>,
    val legs: List<LegDetail>
)

data class LegDetail(
    val leg: Leg,
    val throws: List<Throw>
)
```

---

## Utility classes

### CheckoutCalculator.kt

```kotlin
object CheckoutCalculator {

    /**
     * Returns a suggested checkout string for the given score and rule,
     * or null if no checkout is possible in 3 darts.
     * Examples: "T20 · T20 · D6", "D20", "Bull"
     */
    fun suggest(score: Int, rule: CheckoutRule): String? { ... }

    /**
     * Returns true if a checkout is theoretically reachable in 1–3 darts
     * for the given rule (used to set isCheckoutAttempt flag).
     */
    fun isCheckoutPossible(score: Int, rule: CheckoutRule): Boolean { ... }

    /**
     * Returns true if the final dart of a visit constitutes a valid finish
     * for the given rule and remaining score.
     * lastDartScore and lastDartMult describe the last dart thrown.
     * remainingAfter is the score AFTER applying that dart (should be 0 to win).
     */
    fun isValidCheckout(
        lastDartScore: Int,
        lastDartMult: Int,
        remainingAfter: Int,
        rule: CheckoutRule
    ): Boolean { ... }
}
```

Implement `suggest()` with a hardcoded table of the most common checkouts
(at minimum: all finishes 2–170 for double out, adjusting for straight/triple).
At minimum cover the top 50 most common finishes. For the rest return a generic
dart suggestion.

### TtsManager.kt

```kotlin
class TtsManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    fun init() {
        tts = TextToSpeech(context) { status ->
            isReady = (status == TextToSpeech.SUCCESS)
            if (isReady) tts?.language = Locale.UK
        }
    }

    fun announce(visitTotal: Int, isBust: Boolean, isCheckout: Boolean) {
        if (!isReady) return
        val text = when {
            isCheckout        -> "Game shot"
            isBust            -> "Bust"
            visitTotal == 180 -> "One hundred and eighty"
            visitTotal == 100 -> "One hundred"
            visitTotal >= 100 -> "One hundred and ${visitTotal - 100}"
            else              -> visitTotal.toString()
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "visit_$visitTotal")
    }

    fun shutdown() { tts?.shutdown(); tts = null; isReady = false }
}
```

---

## Game ViewModel

`GameViewModel` is the most complex ViewModel. Annotate with `@HiltViewModel`.
Inject `GameRepository`, `SettingsRepository`, `PlayerRepository`, `Application`
(for TtsManager).

### State

```kotlin
data class GameUiState(
    val screen: GameScreen = GameScreen.SETUP,
    val config: GameConfig? = null,
    val players: List<Player> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val scores: Map<Long, Int> = emptyMap(),        // playerId -> remaining score
    val legWins: Map<Long, Int> = emptyMap(),        // playerId -> legs won
    val currentDarts: List<DartInput> = emptyList(), // 0, 1 or 2 entries (dart 1, dart 2)
    val pendingMultiplier: Int = 1,                  // 1, 2 or 3 — resets after each dart
    val visitHistory: List<VisitRecord> = emptyList(),
    val currentLegNumber: Int = 1,
    val gameId: Long? = null,
    val legId: Long? = null,
    val winnerId: Long? = null,
    val checkoutHint: String? = null,
    val setupDefaults: SetupDefaults = SetupDefaults()
)

enum class GameScreen { SETUP, LIVE, RESULT }

data class DartInput(val score: Int, val multiplier: Int) {
    val value: Int get() = score * multiplier
    fun label(): String = when {
        score == 0          -> "Miss"
        multiplier == 2     -> "D$score"
        multiplier == 3     -> "T$score"
        else                -> "$score"
    }
}

data class VisitRecord(
    val playerId: Long,
    val playerName: String,
    val dart1: DartInput?,
    val dart2: DartInput?,
    val dart3: DartInput?,
    val total: Int,
    val isBust: Boolean
)

data class SetupDefaults(
    val startScore: Int = 501,
    val checkoutRule: CheckoutRule = CheckoutRule.DOUBLE,
    val legsToWin: Int = 1,
    val randomOrder: Boolean = false,
    val recentPlayerIds: List<Long> = emptyList()
)
```

### Key methods to implement

- `loadSetupDefaults()` — called on init, loads last-used settings from SettingsRepository
- `setMultiplier(mult: Int)` — sets pendingMultiplier, does NOT reset until after a dart is recorded
- `recordDart(score: Int)` — uses current pendingMultiplier, then immediately resets to 1. Appends to currentDarts. If 3rd dart OR miss completes visit → calls `resolveVisit()`. For 25 with Triple pending: ignore (triple bull not valid), show snackbar.
- `recordMiss()` — records dart with score=0, mult=0, resets multiplier to 1
- `resolveVisit()` — calculates visit total, checks bust, checks checkout, persists Throw to DB, advances player turn, updates checkoutHint, calls TTS
- `undoLastDart()` — removes last dart from currentDarts; if currentDarts is empty, deletes last Throw from DB and restores previous player state
- `startGame(config: GameConfig)` — saves config to DB, saves last settings, updates recent players
- `onLegWon(winnerId: Long)` — closes leg in DB, checks if game is over (legWins >= legsToWin), either starts new leg or closes game
- `resetToSetup()` — clears all game state, returns to SETUP screen

### Bust logic
A visit is a bust when:
- Visit total > remaining score
- Remaining score after visit would be 1 (for DOUBLE/TRIPLE — you can't finish on 1)
- Remaining = 0 but the final dart does not satisfy the checkout rule

On bust: visitTotal stored as 0, isBust = true, score reverts, player turn advances.

### Checkout detection
On each visit, after calculating total: if `remainingScore - visitTotal == 0`
AND `CheckoutCalculator.isValidCheckout(lastDart, remainingScore - prevDartsTotal, rule)` → leg won.

---

## Game Setup Screen

File: `GameSetupScreen.kt`

### Layout (single scrollable column, no wizard)

1. **Screen title**: "New game"

2. **Starting score selector**
   Segmented button row: `201 | 301 | 401 | 501 | 701`
   Selected = accent background. Default: last used (501 on first launch).

3. **Checkout rule selector**
   Segmented button row: `Straight | Double | Triple`
   Default: last used (Double).

4. **Legs selector**
   Segmented button row: `1 | 3 | 5 | 7 | 9`
   Default: last used (1).

5. **Players section header row**
   Left: "Players  N selected" (N in TextPrimary weight)
   Right: `Random order` checkbox. When checked, drag handles on player rows hide
   and the subtitle under each player changes to "Order randomised on start".

6. **Selected players list**
   Each row: drag handle (3-line icon, hidden when random order checked) +
   avatar (initials) + name + subtitle ("Throws Nth" or "Order randomised on start") +
   × remove button.
   Active/first player row has accent border. Use `ReorderableColumn` or
   implement drag-to-reorder with `Modifier.draggable`.

7. **"+ Add player" dashed button**
   Opens `PlayerPickerSheet` (bottom sheet).

8. **Start game button** (accent, full width, bottom)
   Disabled when 0 players selected. When tapped: if randomOrder checked, shuffle
   player list before starting.

---

## Player Picker Bottom Sheet

File: `PlayerPickerSheet.kt` — a `ModalBottomSheet`.

### Content

1. **Search bar** at top — filters player list live as user types (case-insensitive,
   matches anywhere in name).

2. **Recent section** — shown when search is empty. Up to 5 most recently played
   players. Players already in the game shown greyed out with "In game" label
   instead of Add button.

3. **All players A–Z** — shown below recent (when search empty) or replaces recent
   (when searching). Players already in game are greyed + "In game".

4. **Add button** on each row — immediately adds to selected list and closes sheet.
   Sheet stays open if user wants to add more (add without dismissing).

---

## Live Game Screen

File: `LiveGameScreen.kt`

### Layout (portrait, no scrolling — everything visible at once)

**Top: Player strip** — horizontal row split between active player (left ~55%)
and waiting players stacked on the right.

Active player panel (accent-tinted background, accent left border):
- Name label + "throwing" badge in accent
- Remaining score in large font (accent color)
- Three dart slot boxes showing current visit darts (D1, D2, D3) — filled as darts are entered
- Average below

Waiting player panels (right side, stacked vertically):
- Name + remaining score (smaller) + average
- 2 players = 2 equal halves; 3+ players = smaller rows

**Checkout hint bar** — thin bar between strip and numpad:
- "Checkout hint" label left, suggested route right (green, monospace)
- Empty/hidden when score > 170 or no checkout possible

**Multiplier row** — 4 buttons: `Single | Double | Triple | Miss`
- Active multiplier highlighted in accent. Auto-resets to Single after each dart.
- Miss immediately records a zero dart and resets multiplier.
- When Double is pending: disable 25 button? No — D25 = 50 which IS valid. Only disable when Triple is pending (T25 doesn't exist).

**Numpad** — 5×4 grid + bottom row:
```
 1   2   3   4   5
 6   7   8   9  10
11  12  13  14  15
16  17  18  19  20
[  25 (bull)  ] [Undo↩]
```
- 25 greyed out when Triple is pending (triple bull doesn't exist).
- Undo button (bottom right): removes last entered dart.

**Visit history** — below numpad, last 3–4 visits:
- Columns: Player | D1 | D2 | D3 | Total
- Bust rows in red background with "BUST" label in total column
- 180s shown with accent color total

---

## Game Result Screen

File: `GameResultScreen.kt`

Shown after game ends (slide up from bottom or navigate to it).

Content:
1. Winner name + "Winner!" in large accent text
2. Final leg score e.g. "2 — 1" for a best-of-3
3. Per-player stat card: name + avg + highest finish + 180s in this game
4. Two buttons: **New game** (returns to setup, pre-fills same players) and **Done** (returns to setup, clears players)

---

## Stats Screen

File: `StatsScreen.kt`

### Empty state (no player selected)

**Club overview** — 2×2 grid of metric cards:
- Total games played
- Total players
- Total 180s
- Club highest finish

**Top averages leaderboard** — all players ranked by avg.
Each row: rank number + avatar + name + average (monospace).
Tapping a row selects that player and shows their stats below.

### Player selected state

**Player header** — avatar + name + back/deselect option.

**Metric cards row** (scrollable horizontal or 2×2 grid):
- 3-dart average
- Highest finish
- Checkout %  (successfulCheckouts / checkoutAttempts, 0% if no attempts)
- 180s
- 100+ visits
- Legs won

**Visit score chart** — "Most thrown visit scores" section:

*Top scores view* (default): horizontal bar chart, top 10 visit totals by frequency.
Bar width proportional to frequency. Color: accent for top 3, dimming for others.
Each bar shows score label (left) and count (right inside bar or next to it).

*Buckets view* (toggle): 5 horizontal bars:
- 100–180 (blue)
- 60–99 (blue, lighter)
- 40–59 (blue, even lighter)
- 1–39 (muted)
- Busts (red)
Each shows count + percentage of total visits.

Toggle between views with two buttons: "Top scores" | "Buckets".

**Game history** — scrollable list of past games this player was in.
Each row: date + opponent(s) + result (W/L) + format + avg.
Tapping navigates to MatchDetailScreen.

---

## History Screen

File: `HistoryScreen.kt`

**Top bar**: "Match history" + Filter button (right).

**Filter chips** (horizontal scrollable row): All | Casual | Solo | 501 | 301
Multiple chips can be active simultaneously.

**Game list** — grouped by date (Today / Yesterday / full date string).
Each card:
- Game title: player names joined by "·" or "vs" for 2 players
- Format line: "501 · Double out · Best of 3 · HH:MM" (monospace for time)
- Type chip: Casual / Solo
- Player rows: avatar (green = winner) + name + legs won + avg
- Winner listed first, others below in muted style

Tapping a card navigates to `history/detail/{gameId}`.

---

## Match Detail Screen

File: `MatchDetailScreen.kt` — receives `gameId: Long`.

Loads `GameDetail` from repository on init.

### Sections

**Back button** + "Match detail" title in top bar.

**Match header card**:
- Date + time + format + type
- For 2 players: large "2 — 1" score with avatars left/right
- For 3+ players: each player on a row with legs won

**Player statistics** — one card per player:
- Avatar + name + Winner badge (if applicable)
- 4 stats in a grid: avg · highest finish · checkout % · 180s
  (all scoped to this game only)

**Leg by leg** — one card per leg:
- "Leg N" header + total visits count
- Grid of player results: winner cell green (Won · N visits · finish: D16),
  losers show visits + "left: NNN"

**Full visit log** — leg selector buttons (Leg 1 | Leg 2 | ...).
Table header: Player | D1 | D2 | D3 | Total
One row per visit, all players interleaved in throw order.
Bust rows in red. 180s in accent. Dart notation in monospace (T20, D16, etc).

---

## Players Screen

File: `PlayersScreen.kt`

Simple list of all players with:
- Avatar (initials, colored consistently per player using name hash)
- Player name
- Total games played (shown as subtitle)
- Edit (pencil icon) and Delete (trash icon) buttons per row

**Add player FAB** — floating action button, opens a dialog with a single text
field for the player name. Validation: name must be non-empty and unique.

**Edit** — same dialog, pre-filled with current name.

**Delete** — confirmation dialog before deleting. Note: deleting a player does NOT
delete their historical throws (foreign key should use NO ACTION or SET NULL for
historical data integrity — adjust schema if needed).

---

## Implementation rules — follow these strictly

1. **All DB operations** must run on a coroutine dispatcher, never on the main
   thread. Use `viewModelScope.launch` in ViewModels. Repositories use `suspend`
   functions; DAOs use `suspend` + `Flow` as appropriate.

2. **State management**: every screen observes a single `UiState` data class
   collected with `collectAsStateWithLifecycle()`. No raw mutable state leaking
   out of ViewModels.

3. **No hardcoded strings** in composables — use `stringResource` or at minimum
   define constants. The app name is "Club Darts".

4. **TtsManager lifecycle**: initialise in `GameViewModel.init {}`, shut down in
   `onCleared()`. Never initialise in a composable.

5. **Navigation**: use `NavController.navigate()` for all transitions. The back
   stack should be managed so pressing back from Live Game goes to Setup (not a
   blank screen). Use `popUpTo` and `launchSingleTop` appropriately.

6. **Error states**: wrap all repository calls in try/catch in ViewModels. Expose
   an optional `errorMessage: String?` in UiState. Show a `Snackbar` when non-null.

7. **Undo correctness**: undo must revert both in-memory state (currentDarts,
   remaining score) and the DB (delete last Throw row if the previous visit was
   already persisted). Do not persist a visit until all 3 darts are entered OR
   a checkout/bust is detected.

8. **Checkout hint** must update after every dart, not just after a full visit.
   After dart 1 and dart 2, recalculate the hint for the remaining score.

9. **Visit persistence timing**: only write to DB after a visit is fully resolved
   (3 darts entered, or bust detected, or checkout). Never write partial visits.
   Use in-memory `currentDarts` list to track the current visit.

10. **Score display**: always show the remaining score, never the score already
    subtracted during the current visit. Subtract only when the visit is resolved.

---

## Final checklist before handing off

After building all phases, verify:

- [ ] App compiles without errors or warnings
- [ ] Portrait orientation is locked (rotate device — nothing changes)
- [ ] Starting a game with 1 player works (solo mode)
- [ ] Starting a game with 4 players works (player strip adapts)
- [ ] Double + 25 = 50 is recorded correctly
- [ ] Triple + 25 is blocked (shows a message, does not record)
- [ ] Bust correctly reverts score and advances to next player
- [ ] Undo removes the last dart, undo again removes the previous dart
- [ ] Undo across a visit boundary restores the previous player's state
- [ ] TTS fires after dart 3 / bust / checkout — not before
- [ ] "One hundred and eighty" is spoken correctly for 180
- [ ] Checkout hint disappears for scores > 170
- [ ] Game result screen shows correct stats
- [ ] History list groups by date correctly
- [ ] Match detail shows per-leg breakdown and full visit log
- [ ] Stats screen checkout % does not divide by zero when no attempts
- [ ] Adding a player with a duplicate name is rejected
- [ ] Last used settings are restored on next app open
- [ ] Recent players list updates after each game and shows max 5

---

## Design reference

The visual design uses a **dark theme with acid yellow accent**:
- Backgrounds: near-black surfaces (#0E0F11, #16181C, #1E2026, #262930)
- Primary accent: #E8FF47 (used for active player, selected buttons, Start CTA)
- Winner/success: #3DDB7A
- Bust/error: #FF5C5C
- Checkout hint: #3DDB7A (green, monospace font)
- All scores and dart notation: DM Mono font
- All UI text: DM Sans font
- Border radius: 10dp (cards), 16dp (large cards), 22dp (screen-level containers)
- Bottom nav: always visible, 4 tabs (Game · Stats · History · Players)
