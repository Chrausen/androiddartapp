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
import com.clubdarts.ui.stats.StatsScreen

@Composable
fun ClubDartsNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "game/setup"

    Scaffold(
        bottomBar = {
            ClubDartsBottomNav(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        // Use the route string rather than startDestinationId (an integer).
                        // startDestinationId can't be resolved by the NavController when
                        // restoring a nested graph's back stack, causing a fatal
                        // "destination cannot be found" crash on every tab switch to "game".
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
            navigation(startDestination = "game/setup", route = "game") {
                composable("game/setup") {
                    // Activity-scoped ViewModel so the game survives tab switches
                    val gameViewModel: GameViewModel =
                        hiltViewModel(LocalContext.current as ComponentActivity)
                    GameSetupScreen(
                        onStartGame = {
                            navController.navigate("game/live") {
                                popUpTo("game/setup") { inclusive = false }
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
                                popUpTo("game/setup") { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onBack = {
                            navController.navigate("game/setup") {
                                popUpTo("game/setup") { inclusive = true }
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
                            navController.navigate("game/setup") {
                                popUpTo("game/setup") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onDone = {
                            navController.navigate("game/setup") {
                                popUpTo("game/setup") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        viewModel = gameViewModel
                    )
                }
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
        }
    }
}
