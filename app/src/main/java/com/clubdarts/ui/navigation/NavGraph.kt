package com.clubdarts.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.*
import androidx.navigation.compose.*
import com.clubdarts.data.repository.SettingsRepository
import com.clubdarts.ui.game.GameResultScreen
import com.clubdarts.ui.game.GameSetupScreen
import com.clubdarts.ui.game.GameViewModel
import com.clubdarts.ui.game.LiveGameScreen
import com.clubdarts.ui.history.HistoryScreen
import com.clubdarts.ui.history.MatchDetailScreen
import com.clubdarts.ui.players.PlayersScreen
import com.clubdarts.ui.rankings.RankingsScreen
import com.clubdarts.ui.settings.RankingSettingsScreen
import com.clubdarts.ui.settings.SettingsScreen
import com.clubdarts.ui.settings.TtsSettingsScreen
import com.clubdarts.ui.stats.StatsScreen
import javax.inject.Inject

@Composable
fun ClubDartsNavHost(
    settingsRepository: SettingsRepository
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "game"

    val rankingEnabled by settingsRepository.observeRankingEnabled()
        .collectAsStateWithLifecycle(initialValue = false)

    Scaffold(
        bottomBar = {
            ClubDartsBottomNav(
                currentRoute = currentRoute,
                rankingEnabled = rankingEnabled,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo("game") {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "game",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Game tab — flat routes, no nested navigation graph wrapper.
            composable("game") {
                val gameViewModel: GameViewModel =
                    hiltViewModel(LocalContext.current as ComponentActivity)
                GameSetupScreen(
                    onStartGame = {
                        navController.navigate("game/live") {
                            launchSingleTop = true
                        }
                    },
                    gameViewModel = gameViewModel
                )
            }
            composable("game/live") {
                val gameViewModel: GameViewModel =
                    hiltViewModel(LocalContext.current as ComponentActivity)
                LiveGameScreen(
                    onGameFinished = {
                        navController.navigate("game/result") {
                            launchSingleTop = true
                        }
                    },
                    onBack = {
                        navController.navigate("game") {
                            popUpTo("game") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    viewModel = gameViewModel
                )
            }
            composable("game/result") {
                val gameViewModel: GameViewModel =
                    hiltViewModel(LocalContext.current as ComponentActivity)
                GameResultScreen(
                    onNewGame = {
                        navController.navigate("game") {
                            popUpTo("game") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onDone = {
                        navController.navigate("game") {
                            popUpTo("game") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    viewModel = gameViewModel
                )
            }

            composable("stats") {
                StatsScreen(
                    onNavigateToMatchDetail = { gameId ->
                        navController.navigate("history/detail/$gameId")
                    }
                )
            }
            composable("history") {
                HistoryScreen(
                    onNavigateToDetail = { gameId ->
                        navController.navigate("history/detail/$gameId")
                    }
                )
            }
            composable(
                "history/detail/{gameId}",
                arguments = listOf(navArgument("gameId") { type = NavType.LongType })
            ) { backStackEntry ->
                val gameId = backStackEntry.arguments?.getLong("gameId") ?: return@composable
                MatchDetailScreen(
                    gameId = gameId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("rankings") {
                RankingsScreen()
            }
            composable("players") {
                PlayersScreen()
            }
            composable("settings") {
                SettingsScreen(
                    onNavigateToTtsScores = {
                        navController.navigate("settings/tts")
                    },
                    onNavigateToRankingSettings = {
                        navController.navigate("settings/ranking")
                    }
                )
            }
            composable("settings/tts") {
                TtsSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings/ranking") {
                RankingSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
