# Club Darts

A darts scoring app for Android built with Kotlin, Jetpack Compose, Room, and Hilt.

## Requirements

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 17** (bundled with Android Studio)
- **Android SDK** with API level 35 installed
- A physical Android device (API 26+) or an emulator running Android 8.0+

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
    └── TtsManager.kt          # Text-to-speech announcements
```
