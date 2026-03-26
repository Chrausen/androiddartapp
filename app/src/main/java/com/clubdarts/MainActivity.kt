package com.clubdarts

import android.content.Context
import android.content.res.Configuration
import android.os.Build
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

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences(GeneralSettingsViewModel.PREFS_NAME, Context.MODE_PRIVATE)
        val lang = prefs.getString(GeneralSettingsViewModel.KEY_LANGUAGE, "en") ?: "en"
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
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
