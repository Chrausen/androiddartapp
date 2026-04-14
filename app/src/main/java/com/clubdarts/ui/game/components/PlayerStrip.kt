package com.clubdarts.ui.game.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubdarts.data.model.Player
import com.clubdarts.ui.game.DartInput
import com.clubdarts.ui.theme.*

@Composable
fun PlayerStrip(
    players: List<Player>,
    currentPlayerIndex: Int,
    scores: Map<Long, Int>,
    legWins: Map<Long, Int>,
    currentDarts: List<DartInput>,
    modifier: Modifier = Modifier,
    // Team mode params (optional — default to single mode behaviour)
    isTeamGame: Boolean = false,
    teamAssignments: Map<Long, Int> = emptyMap(),   // playerId → 0 or 1
    teamScores: Map<Int, Int> = emptyMap(),          // teamIndex → remaining score
    teamLegWins: Map<Int, Int> = emptyMap(),         // teamIndex → legs won
    currentTeamIndex: Int = -1,                      // team whose turn it is (-1 for non-team)
    teamPlayerIndexes: Map<Int, Int> = emptyMap(),   // teamIndex → current player pos within team
    animationsEnabled: Boolean = true,
    pendingBoardDartValue: Int = 0,
    playerVisitTotals: Map<Long, Int> = emptyMap(),
    playerVisitCounts: Map<Long, Int> = emptyMap(),
) {
    val orderedPlayers = if (players.isEmpty()) {
        emptyList()
    } else if (isTeamGame && currentTeamIndex >= 0 && teamPlayerIndexes.isNotEmpty()) {
        // Build the team→[playerIndices] map and simulate upcoming turn order
        val teamPlayerIndices = buildMap<Int, List<Int>> {
            val tmp = mutableMapOf<Int, MutableList<Int>>()
            players.forEachIndexed { idx, p ->
                val team = teamAssignments[p.id] ?: return@forEachIndexed
                tmp.getOrPut(team) { mutableListOf() }.add(idx)
            }
            putAll(tmp)
        }
        val pointers = teamPlayerIndexes.toMutableMap()
        var team = currentTeamIndex
        (0 until players.size).map {
            val playerIdx = teamPlayerIndices[team]?.get(pointers[team] ?: 0) ?: 0
            val teamSize = teamPlayerIndices[team]?.size ?: 1
            pointers[team] = ((pointers[team] ?: 0) + 1) % teamSize
            team = 1 - team
            players[playerIdx]
        }
    } else {
        players.indices.map { offset -> players[(currentPlayerIndex + offset) % players.size] }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        orderedPlayers.forEachIndexed { index, player ->
            val isActive = index == 0
            key(index) {
                AnimatedContent(
                    targetState = player,
                    label = "player_slot_$index",
                    modifier = Modifier.fillMaxWidth().clipToBounds(),
                    transitionSpec = {
                        if (animationsEnabled) {
                            slideInVertically(tween(120, delayMillis = 120)) { it } togetherWith
                                    slideOutVertically(tween(120)) { -it }
                        } else {
                            fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        }
                    }
                ) { p ->
                    val teamIdx = teamAssignments[p.id]
                    val teamColor: Color? = if (isTeamGame && teamIdx != null) {
                        if (teamIdx == 0) Red else Blue
                    } else null
                    val displayScore = if (isTeamGame && teamIdx != null) {
                        teamScores[teamIdx] ?: 0
                    } else {
                        scores[p.id] ?: 0
                    }
                    val displayLegWins = if (isTeamGame && teamIdx != null) {
                        teamLegWins[teamIdx] ?: 0
                    } else {
                        legWins[p.id] ?: 0
                    }
                    val teamLabel = if (isTeamGame && teamIdx != null) {
                        if (teamIdx == 0) "Team A" else "Team B"
                    } else null

                    val playerAvg: Double? = run {
                        val count = playerVisitCounts[p.id] ?: 0
                        val total = playerVisitTotals[p.id] ?: 0
                        if (count > 0) total.toDouble() / count else null
                    }

                    if (isActive) {
                        ActivePlayerPanel(
                            player = p,
                            score = displayScore,
                            legWins = displayLegWins,
                            currentDarts = currentDarts,
                            teamColor = teamColor,
                            teamLabel = teamLabel,
                            pendingBoardDartValue = pendingBoardDartValue,
                            playerAvg = playerAvg,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        WaitingPlayerPanel(
                            player = p,
                            score = displayScore,
                            legWins = displayLegWins,
                            teamColor = teamColor,
                            playerAvg = playerAvg,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivePlayerPanel(
    player: Player,
    score: Int,
    legWins: Int,
    currentDarts: List<DartInput>,
    modifier: Modifier = Modifier,
    teamColor: Color? = null,
    teamLabel: String? = null,
    pendingBoardDartValue: Int = 0,
    playerAvg: Double? = null,
) {
    // Use team color (dim) or default accent
    val highlightColor = teamColor ?: Accent
    val highlightBg = teamColor?.copy(alpha = 0.12f) ?: AccentDim

    Box(
        modifier = modifier
            .background(highlightBg, RoundedCornerShape(10.dp))
            .border(1.dp, highlightColor, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        val dartsTotal    = currentDarts.sumOf { it.value } + pendingBoardDartValue
        val liveRemaining = score - dartsTotal
        val isBusting     = liveRemaining < 0

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = highlightColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "throwing",
                        style = MaterialTheme.typography.labelSmall,
                        color = Background,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                if (teamLabel != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        color = highlightColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = teamLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = highlightColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: dart slots
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { i ->
                        DartSlot(dart = currentDarts.getOrNull(i))
                    }
                }
                // Right: avg label + score
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    playerAvg?.let {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "avg",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary
                            )
                            Text(
                                text = "%.1f".format(it),
                                fontSize = 14.sp,
                                fontFamily = DmMono,
                                color = TextTertiary
                            )
                        }
                    }
                    Box {
                        Text(
                            text = "888",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmMono,
                            lineHeight = 46.sp,
                            modifier = Modifier.alpha(0f)
                        )
                        Text(
                            text = if (isBusting) score.toString() else liveRemaining.toString(),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmMono,
                            color = if (isBusting) Red else highlightColor,
                            lineHeight = 46.sp
                        )
                    }
                }
            }
        }

        Text(
            text = "Legs: $legWins",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
private fun DartSlot(dart: DartInput?) {
    Box(
        modifier = Modifier
            .size(44.dp, 30.dp)
            .background(Surface3, RoundedCornerShape(4.dp))
            .border(1.dp, Border2, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (dart != null) {
            Text(
                text = dart.label(),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = DmMono,
                color = if (dart.score == 0) TextTertiary else TextPrimary,
                fontSize = 11.sp
            )
        } else {
            Text(text = "—", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun WaitingPlayerPanel(
    player: Player,
    score: Int,
    legWins: Int,
    modifier: Modifier = Modifier,
    teamColor: Color? = null,
    playerAvg: Double? = null,
) {
    Row(
        modifier = modifier
            .background(Surface2, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (teamColor != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(teamColor, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Column {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1
                )
                Text(
                    text = "Legs: $legWins",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = score.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DmMono,
                color = TextPrimary
            )
            playerAvg?.let {
                Text(
                    text = "avg ${"%.1f".format(it)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = DmMono,
                    color = TextTertiary
                )
            }
        }
    }
}
