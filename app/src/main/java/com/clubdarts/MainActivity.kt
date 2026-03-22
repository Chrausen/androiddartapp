package com.clubdarts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.clubdarts.data.repository.SettingsRepository
import com.clubdarts.ui.navigation.ClubDartsNavHost
import com.clubdarts.ui.theme.ClubDartsTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClubDartsTheme {
                ClubDartsNavHost(settingsRepository = settingsRepository)
            }
        }
    }
}
