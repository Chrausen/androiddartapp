package com.clubdarts.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clubdarts.R
import com.clubdarts.ui.theme.Accent
import com.clubdarts.ui.theme.Surface
import com.clubdarts.ui.theme.TextTertiary

sealed class BottomNavItem(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    object Game     : BottomNavItem("game",     R.string.nav_game,     Icons.Default.RadioButtonUnchecked)
    object Stats    : BottomNavItem("stats",    R.string.nav_stats,    Icons.Default.BarChart)
    object Training : BottomNavItem("training", R.string.nav_training, Icons.Default.FitnessCenter)
    object History  : BottomNavItem("history",  R.string.nav_history,  Icons.Default.History)
    object Rankings : BottomNavItem("rankings", R.string.nav_rankings, Icons.Default.EmojiEvents)
    object Players  : BottomNavItem("players",  R.string.nav_players,  Icons.Default.Group)
    object Settings : BottomNavItem("settings", R.string.nav_settings, Icons.Default.Settings)
}

val baseBottomNavItems = listOf(
    BottomNavItem.Game,
    BottomNavItem.Players,
    BottomNavItem.Stats,
    BottomNavItem.Training,
    BottomNavItem.History,
    BottomNavItem.Settings
)

val rankingBottomNavItems = listOf(
    BottomNavItem.Game,
    BottomNavItem.Rankings,
    BottomNavItem.Players,
    BottomNavItem.Stats,
    BottomNavItem.Training,
    BottomNavItem.History,
    BottomNavItem.Settings
)

@Composable
fun ClubDartsBottomNav(
    currentRoute: String,
    rankingEnabled: Boolean,
    onNavigate: (String) -> Unit
) {
    val items = if (rankingEnabled) rankingBottomNavItems else baseBottomNavItems

    NavigationBar(
        containerColor = Surface,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val isActive = currentRoute.startsWith(item.route.substringBefore("/"))
                || (item is BottomNavItem.Game && (currentRoute.startsWith("game/")))

            NavigationBarItem(
                selected = isActive,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(item.labelRes)
                    )
                },
                label = { Text(stringResource(item.labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Accent,
                    selectedTextColor = Accent,
                    unselectedIconColor = TextTertiary,
                    unselectedTextColor = TextTertiary,
                    indicatorColor = Surface
                )
            )
        }
    }
}
