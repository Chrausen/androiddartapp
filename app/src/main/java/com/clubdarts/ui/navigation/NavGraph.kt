package com.clubdarts.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.clubdarts.ui.game.GameResultScreen
import com.clubdarts.ui.game.GameSetupScreen
import com.clubdarts.ui.game.GameViewModel
import com.clubdarts.ui.game.LiveGameScreen
import com.clubdarts.ui.history.HistoryScreen
import com.clubdarts.ui.history.MatchDetailScreen
import com.clubdarts.ui.players.PlayersScreen
import com.clubdarts.ui.settings.SettingsScreen
import com.clubdarts.ui.settings.TtsSettingsScreen
import com.clubdarts.ui.stats.StatsScreen

@Composable
fun ClubDartsNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "game"

    Scaffold(
        bottomBar = {
            ClubDartsBottomNav(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        // "game" is now a flat leaf destination (not a nested graph),
                        // so popUpTo("game") always finds it on the back stack and
                        // saveState/restoreState work correctly across tab switches.
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
            // The "game" route IS the setup screen (the tab's home destination).
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
            composable("players") {
                PlayersScreen()
            }
            composable("settings") {
                SettingsScreen(
                    onNavigateToTtsScores = {
                        navController.navigate("settings/tts")
                    }
                )
            }
            composable("settings/tts") {
                TtsSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
