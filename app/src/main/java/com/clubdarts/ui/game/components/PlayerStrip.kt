package com.clubdarts.ui.game.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.clipToBounds
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
    modifier: Modifier = Modifier
) {
    val orderedPlayers = if (players.isEmpty()) emptyList() else {
        players.indices.map { offset -> players[(currentPlayerIndex + offset) % players.size] }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Each position slot is fixed in the layout; only its content animates.
        // The top slot fades the old player upward and the new player in from below.
        orderedPlayers.forEachIndexed { index, player ->
            val isActive = index == 0
            key(index) {
                AnimatedContent(
                    targetState = player,
                    label = "player_slot_$index",
                    modifier = Modifier.fillMaxWidth().clipToBounds(),
                    transitionSpec = {
                        // Exit plays first (120ms). Enter starts only after exit finishes
                        // (delayMillis = 120). Clip keeps the incoming card hidden below
                        // the slot boundary until it slides up into view.
                        slideInVertically(tween(120, delayMillis = 120)) { it } togetherWith
                                slideOutVertically(tween(120)) { -it }
                    }
                ) { p ->
                    if (isActive) {
                        ActivePlayerPanel(
                            player = p,
                            score = scores[p.id] ?: 0,
                            legWins = legWins[p.id] ?: 0,
                            currentDarts = currentDarts,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        WaitingPlayerPanel(
                            player = p,
                            score = scores[p.id] ?: 0,
                            legWins = legWins[p.id] ?: 0,
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
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(AccentDim, RoundedCornerShape(10.dp))
            .border(1.dp, Accent, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
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
                    color = Accent,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "throwing",
                        style = MaterialTheme.typography.labelSmall,
                        color = Background,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = score.toString(),
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DmMono,
                color = Accent,
                lineHeight = 46.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { i ->
                    DartSlot(dart = currentDarts.getOrNull(i))
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
            .size(32.dp, 24.dp)
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
                fontSize = 9.sp
            )
        } else {
            Text(text = "—", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 9.sp)
        }
    }
}

@Composable
private fun WaitingPlayerPanel(
    player: Player,
    score: Int,
    legWins: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Surface2, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
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
        Text(
            text = score.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = DmMono,
            color = TextPrimary
        )
    }
}
