package com.clubdarts

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.clubdarts.data.repository.SettingsRepository
import com.clubdarts.ui.LocalSoundEffectsService
import com.clubdarts.ui.navigation.ClubDartsNavHost
import com.clubdarts.ui.settings.GeneralSettingsViewModel
import com.clubdarts.ui.theme.ClubDartsTheme
import com.clubdarts.util.SoundEffectsService
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var soundEffectsService: SoundEffectsService

    // Resolved before super.attachBaseContext() so getResources() can use it.
    private var localeOverride: Locale? = null

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences(GeneralSettingsViewModel.PREFS_NAME, Context.MODE_PRIVATE)
        val lang = prefs.getString(GeneralSettingsViewModel.KEY_LANGUAGE, "en") ?: "en"
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        localeOverride = locale
        // Pass newBase unmodified so Hilt's context chain stays intact.
        // On Android 13+, wrapping with createConfigurationContext() here breaks
        // Hilt's applicationContext resolution and causes a crash at injection time.
        super.attachBaseContext(newBase)
    }

    override fun getResources(): Resources {
        val locale = localeOverride ?: return super.getResources()
        val config = Configuration(super.getResources().configuration)
        config.setLocales(LocaleList(locale))
        return createConfigurationContext(config).resources
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        soundEffectsService.init()
        setContent {
            CompositionLocalProvider(LocalSoundEffectsService provides soundEffectsService) {
                ClubDartsTheme {
                    ClubDartsNavHost(settingsRepository = settingsRepository)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundEffectsService.shutdown()
    }
}
