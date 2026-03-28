# Club Darts

HI
A darts scoring app for Android built with Kotlin, Jetpack Compose, Room, and Hilt.

## Requirements

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 17 or higher** (bundled with Android Studio; JDK 21 also works)
- **Android SDK** with API level 35 installed (used as `compileSdk`)
- A physical Android device (API 26+) or an emulator running Android 8.0+

> The app targets Android 13 (API 33) runtime behaviour (`targetSdk = 33`) while
> being compiled against API 35.

## Building the app

### 1. Clone the repository

```bash
git clone https://github.com/Chrausen/androiddartapp.git
cd androiddartapp
```

### 2. Open in Android Studio

1. Launch Android Studio
2. Select **File → Open** and navigate to the cloned directory
3. Android Studio will automatically detect the Gradle project and sync dependencies

### 3. Build from Android Studio

- **Debug build**: Click **Build → Make Project** (or press `Ctrl+F9` / `Cmd+F9`)
- **Release APK**: Click **Build → Build Bundle(s) / APK(s) → Build APK(s)**

### 4. Build from the command line

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (unsigned)
./gradlew assembleRelease
```

> **Note:** `./gradlew` requires `gradle/wrapper/gradle-wrapper.jar` to be present
> in the repository. If it is missing, use a system installation of Gradle 8.6+
> as a drop-in replacement:
> ```bash
> gradle assembleDebug
> ```

The output APK is placed in `app/build/outputs/apk/debug/app-debug.apk`.

## Running the app

### On a physical device

1. Enable **Developer Options** on your Android device:
   - Go to **Settings → About phone** and tap **Build number** 7 times
2. Enable **USB Debugging** in **Settings → Developer Options**
3. Connect your device via USB
4. In Android Studio, select your device from the device dropdown and click **Run ▶** (or press `Shift+F10`)

### On an emulator

1. In Android Studio, open **Device Manager** (the phone icon in the right toolbar)
2. Click **Create Device**, choose a phone profile (e.g. Pixel 6), and select a system image with API 26 or higher
3. Start the emulator, then click **Run ▶** in Android Studio

### From the command line

```bash
# Install the debug APK on a connected device/emulator
./gradlew installDebug

# Then launch the app
adb shell am start -n com.clubdarts/.MainActivity
```

## Running the tests

### Unit tests (no device required)

```bash
./gradlew test
```

### E2E instrumented tests (requires a connected device or emulator)

```bash
./gradlew connectedDebugAndroidTest
```

To run only the standard workflow E2E test:

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.clubdarts.e2e.StandardWorkflowTest
```

## Project structure

```
app/src/main/java/com/clubdarts/
├── ClubDartsApp.kt          # Hilt application class
├── MainActivity.kt          # Single activity entry point
├── data/
│   ├── db/                  # Room database, DAOs, type converters
│   ├── model/               # Entity data classes
│   └── repository/          # Repository layer
├── di/                      # Hilt dependency injection modules
├── ui/
│   ├── game/                # Game setup, live game, result screens + ViewModel
│   ├── history/             # Match history and detail screens
│   ├── navigation/          # NavGraph and bottom navigation
│   ├── players/             # Player management screen
│   ├── stats/               # Statistics screen
│   └── theme/               # Colors, typography, Material3 theme
└── util/
    ├── CheckoutCalculator.kt  # Checkout hint and validation logic
    ├── ScoringEngine.kt       # Pure bust-detection and score-resolution logic
    └── TtsManager.kt          # Text-to-speech announcements
```

## Features

### Game Setup

- **Single-player** and **Teams** game modes (two teams, any number of players per side)
- Configurable **starting score** (201, 301, 501, and more)
- **Checkout rule**: Straight (any finish), Double (must finish on a double), or Triple (must finish on a triple)
- **Legs to win** (best-of-N format)
- **Random player order** toggle
- **Ranked game** toggle (shown when the ranking system is enabled in settings)

### Live Gameplay

- Real-time score tracking for all players
- **Dart numpad**: numbers 0–20 and 25, single/double/triple multiplier buttons, miss button, and per-dart undo
- **Checkout hint** displayed when a player's remaining score is ≤ 170, suggesting a finishing combination
- **Visit history panel** showing the last 20 throws (toggle on/off)
- **Text-to-speech** score announcements after each visit, with a mute toggle
- Bust detection: over-score, landing on 1 with Double/Triple rules, and invalid checkout multiplier are all treated as busts

### Game Result

- Winner announcement with leg summary
- **Elo rating change** displayed for ranked games
- Actions: **Save** the game to history, **Repeat** with the same players and settings, start a **New Game**, or **Discard** without saving

### Player Management

- Add, rename, and delete players
- Avatar generated from player initials
- Deleting a player who is in an active game is blocked

### Match History

- Chronological list of all past games, grouped by date
- Each card shows participants, game format (starting score, checkout rule, legs), game type badge (Solo / Casual / Ranked / Teams), winner, and timestamp
- Tap any game to open a **full match detail** view with a leg-by-leg and throw-by-throw breakdown

### Statistics

**Club-level overview:**
- Total games played, total players, total 180s thrown, highest checkout

**Per-player stats:**
- Average score per visit
- Personal best finish (highest checkout)
- Checkout percentage
- Total 180s and scores 100+
- Bust count

**Two chart views:**
- Top scores histogram — shows the most frequently thrown visit totals
- Score-bucket distribution — breaks down visits into ranges (100–180, 60–99, 40–59, 1–39, busts)

### Rankings (Elo)

- Elo-based leaderboard, visible once a player has played ≥ 5 ranked matches
- Shows rank position, win-loss record, and current Elo rating
- **Configurable K-factor** (32 standard or 64 aggressive) and ranked game format (starting score, checkout rule, legs to win)
- Per-player Elo history and ranked match log
- Reset all ranking data with confirmation

### Settings

| Setting | Options |
|---------|---------|
| Language | English, German |
| Animations | On / Off |
| Delete all data | With confirmation dialog |
| TTS custom phrases | Add/edit/delete per-score announcements; test audio playback |
| Ranking system | Enable/disable; configure K-factor and ranked game format; reset data |
| Debug mode *(debug builds only)* | Generate 20 players and 500 sample games |
