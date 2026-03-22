package com.clubdarts.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.clubdarts.ui.theme.Accent
import com.clubdarts.ui.theme.Surface
import com.clubdarts.ui.theme.TextTertiary

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Game     : BottomNavItem("game",     "Game",     Icons.Default.RadioButtonUnchecked)
    object Stats    : BottomNavItem("stats",    "Stats",    Icons.Default.BarChart)
    object History  : BottomNavItem("history",  "History",  Icons.Default.History)
    object Rankings : BottomNavItem("rankings", "Rankings", Icons.Default.EmojiEvents)
    object Players  : BottomNavItem("players",  "Players",  Icons.Default.Group)
    object Settings : BottomNavItem("settings", "Settings", Icons.Default.Settings)
}

val baseBottomNavItems = listOf(
    BottomNavItem.Game,
    BottomNavItem.Stats,
    BottomNavItem.History,
    BottomNavItem.Players,
    BottomNavItem.Settings
)

val rankingBottomNavItems = listOf(
    BottomNavItem.Game,
    BottomNavItem.Stats,
    BottomNavItem.History,
    BottomNavItem.Rankings,
    BottomNavItem.Players,
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label
                        )
                        if (isActive) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Accent)
                            )
                        }
                    }
                },
                label = { Text(item.label) },
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
