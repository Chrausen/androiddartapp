package com.clubdarts.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.navigation.compose.*
import com.clubdarts.ui.game.GameResultScreen
import com.clubdarts.ui.game.GameSetupScreen
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
                        popUpTo(navController.graph.startDestinationId) {
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
            startDestination = "game/setup",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("game/setup") {
                GameSetupScreen(
                    onStartGame = {
                        navController.navigate("game/live") {
                            popUpTo("game/setup") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable("game/live") {
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
                    }
                )
            }
            composable("game/result") {
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
                    }
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
        }
    }
}
