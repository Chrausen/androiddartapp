package com.clubdarts

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.clubdarts.data.repository.SettingsRepository
import com.clubdarts.ui.navigation.ClubDartsNavHost
import com.clubdarts.ui.settings.GeneralSettingsViewModel
import com.clubdarts.ui.theme.ClubDartsTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences(GeneralSettingsViewModel.PREFS_NAME, Context.MODE_PRIVATE)
        val lang = prefs.getString(GeneralSettingsViewModel.KEY_LANGUAGE, "en") ?: "en"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClubDartsTheme {
                ClubDartsNavHost(settingsRepository = settingsRepository)
            }
        }
    }
}
