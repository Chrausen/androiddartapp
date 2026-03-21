package com.clubdarts.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.data.model.CheckoutRule
import com.clubdarts.data.model.Player
import com.clubdarts.data.repository.GameConfig
import com.clubdarts.ui.game.components.PlayerAvatar
import com.clubdarts.ui.game.components.PlayerPickerSheet
import com.clubdarts.ui.players.PlayersViewModel
import com.clubdarts.ui.theme.*

@Composable
fun GameSetupScreen(
    onStartGame: () -> Unit,
    gameViewModel: GameViewModel = hiltViewModel(),
    playersViewModel: PlayersViewModel = hiltViewModel()
) {
    val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()
    val playersState by playersViewModel.uiState.collectAsStateWithLifecycle()

    var startScore by remember(uiState.setupDefaults.startScore) {
        mutableIntStateOf(uiState.setupDefaults.startScore)
    }
    var checkoutRule by remember(uiState.setupDefaults.checkoutRule) {
        mutableStateOf(uiState.setupDefaults.checkoutRule)
    }
    var legsToWin by remember(uiState.setupDefaults.legsToWin) {
        mutableIntStateOf(uiState.setupDefaults.legsToWin)
    }
    var randomOrder by remember(uiState.setupDefaults.randomOrder) {
        mutableStateOf(uiState.setupDefaults.randomOrder)
    }
    var selectedPlayers by remember { mutableStateOf<List<Player>>(emptyList()) }
    var showPlayerPicker by remember { mutableStateOf(false) }
    var settingsExpanded by remember { mutableStateOf(false) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableIntStateOf(0) }
    val rowSpacingPx = with(LocalDensity.current) { 16.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()
    val returnAnim = remember { Animatable(0f) }
    var returningPlayerId by remember { mutableStateOf<Long?>(null) }

    // Restore selected players when returning to this screen after a tab switch
    LaunchedEffect(uiState.setupSelectedPlayerIds, playersState.players) {
        if (selectedPlayers.isEmpty() && uiState.setupSelectedPlayerIds.isNotEmpty() && playersState.players.isNotEmpty()) {
            val map = playersState.players.associateBy { it.id }
            val restored = uiState.setupSelectedPlayerIds.mapNotNull { map[it] }
            if (restored.isNotEmpty()) selectedPlayers = restored
        }
    }

    // Helper: update local state and persist to ViewModel in one call
    val setPlayers: (List<Player>) -> Unit = { players ->
        selectedPlayers = players
        gameViewModel.updateSetupSelectedPlayers(players.map { it.id })
    }
    val chevronRotation by animateFloatAsState(
        targetValue = if (settingsExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "chevron"
    )

    // Load recent players into picker
    val recentPlayers = remember(uiState.setupDefaults.recentPlayerIds, playersState.players) {
        val ids = uiState.setupDefaults.recentPlayerIds
        val map = playersState.players.associateBy { it.id }
        ids.mapNotNull { map[it] }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "New game",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            }

            // Game settings (collapsible group)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { settingsExpanded = !settingsExpanded }
                ) {
                    AnimatedVisibility(
                        visible = !settingsExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Starting score", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(startScore.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Checkout", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    checkoutRule.name.lowercase().replaceFirstChar { c -> c.uppercaseChar() },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Legs to win", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(legsToWin.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = settingsExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            SectionLabel("Starting score")
                            SegmentedRow(
                                options = listOf(201, 301, 401, 501, 701),
                                selected = startScore,
                                onSelect = { startScore = it },
                                label = { it.toString() }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionLabel("Checkout rule")
                            SegmentedRow(
                                options = CheckoutRule.values().toList(),
                                selected = checkoutRule,
                                onSelect = { checkoutRule = it },
                                label = { it.name.lowercase().replaceFirstChar { c -> c.uppercaseChar() } }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionLabel("Legs to win")
                            SegmentedRow(
                                options = listOf(1, 3, 5, 7, 9),
                                selected = legsToWin,
                                onSelect = { legsToWin = it },
                                label = { it.toString() }
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (settingsExpanded) "Collapse settings" else "Expand settings",
                            tint = TextTertiary,
                            modifier = Modifier
                                .size(28.dp)
                                .rotate(chevronRotation)
                        )
                    }
                }
            }

            // Players section header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Players ", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                        Text(
                            "${selectedPlayers.size} selected",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Random order", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Checkbox(
                            checked = randomOrder,
                            onCheckedChange = { randomOrder = it },
                            colors = CheckboxDefaults.colors(checkedColor = Accent)
                        )
                    }
                }
            }

            // Selected players list
            item {
                val rowTotal = (rowHeightPx + rowSpacingPx).takeIf { it > 0f } ?: 80f
                val dropTargetIndex = if (draggingIndex >= 0) {
                    (draggingIndex + (dragOffsetY / rowTotal).roundToInt())
                        .coerceIn(0, selectedPlayers.size - 1)
                } else -1

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    selectedPlayers.forEachIndexed { index, player ->
                        key(player.id) {
                            val isDragging = index == draggingIndex
                            val isReturning = player.id == returningPlayerId
                            val targetOffset = when {
                                draggingIndex < 0 -> 0f
                                isDragging -> 0f
                                draggingIndex < dropTargetIndex && index in (draggingIndex + 1..dropTargetIndex) -> -rowTotal
                                draggingIndex > dropTargetIndex && index in (dropTargetIndex..draggingIndex - 1) -> rowTotal
                                else -> 0f
                            }
                            val animatedOffset by animateFloatAsState(
                                targetValue = targetOffset,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "item_offset"
                            )
                            val visualOffset = when {
                                isDragging -> dragOffsetY
                                isReturning -> returnAnim.value
                                else -> animatedOffset
                            }

                            SelectedPlayerRow(
                                modifier = Modifier
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .offset { IntOffset(0, visualOffset.roundToInt()) },
                                isDragging = isDragging,
                                player = player,
                                index = index,
                                isFirst = index == 0,
                                randomOrder = randomOrder,
                                onRemove = { setPlayers(selectedPlayers.filter { it.id != player.id }) },
                                onSizeChanged = { h -> if (rowHeightPx == 0) rowHeightPx = h },
                                onDragStart = { draggingIndex = index; dragOffsetY = 0f },
                                onDrag = { dy -> dragOffsetY += dy },
                                onDragEnd = {
                                    val rowTotalEnd = (rowHeightPx + rowSpacingPx).takeIf { it > 0f } ?: 80f
                                    val shift = (dragOffsetY / rowTotalEnd).roundToInt()
                                    val target = (draggingIndex + shift)
                                        .coerceIn(0, selectedPlayers.size - 1)
                                    // Remainder: how far the card is from its new slot's position
                                    val remainder = dragOffsetY - shift * rowTotalEnd
                                    val droppedId = selectedPlayers.getOrNull(draggingIndex)?.id
                                    if (target != draggingIndex) {
                                        val newList = selectedPlayers.toMutableList()
                                        newList.add(target, newList.removeAt(draggingIndex))
                                        setPlayers(newList)
                                    }
                                    draggingIndex = -1
                                    dragOffsetY = 0f
                                    // Animate the card from its visual drop position to its slot
                                    if (droppedId != null) {
                                        returningPlayerId = droppedId
                                        coroutineScope.launch {
                                            returnAnim.snapTo(remainder)
                                            returnAnim.animateTo(
                                                0f,
                                                spring(stiffness = Spring.StiffnessMedium)
                                            )
                                            returningPlayerId = null
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Add player button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(1.dp, Border2, RoundedCornerShape(10.dp))
                        .clickable { showPlayerPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = TextSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add player", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        }

        // Start button — sticky at bottom, always visible
        Button(
            onClick = {
                val orderedPlayers = if (randomOrder) selectedPlayers.shuffled() else selectedPlayers
                val config = GameConfig(
                    startScore = startScore,
                    checkoutRule = checkoutRule,
                    legsToWin = legsToWin,
                    isSolo = orderedPlayers.size == 1,
                    playerIds = orderedPlayers.map { it.id }
                )
                gameViewModel.startGame(config)
                onStartGame()
            },
            enabled = selectedPlayers.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                disabledContainerColor = Surface3,
                contentColor = Background,
                disabledContentColor = TextTertiary
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "Start game",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showPlayerPicker) {
        PlayerPickerSheet(
            allPlayers = playersState.players,
            recentPlayers = recentPlayers,
            selectedPlayerIds = selectedPlayers.map { it.id }.toSet(),
            onPlayerSelected = { player ->
                if (player.id !in selectedPlayers.map { it.id }) {
                    setPlayers(selectedPlayers + player)
                }
            },
            onDismiss = { showPlayerPicker = false }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun <T> SegmentedRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2, RoundedCornerShape(10.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { i, option ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
                    .background(
                        color = if (option == selected) Accent else androidx.compose.ui.graphics.Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (option == selected) Background else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SelectedPlayerRow(
    player: Player,
    index: Int,
    isFirst: Boolean,
    randomOrder: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    onSizeChanged: (Int) -> Unit = {},
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { onSizeChanged(it.height) }
            .then(if (isDragging) Modifier.shadow(6.dp, RoundedCornerShape(10.dp)) else Modifier)
            .background(
                color = if (isDragging) Surface3 else if (isFirst) AccentDim else Surface2,
                shape = RoundedCornerShape(10.dp)
            )
            .then(
                if (isFirst && !isDragging) Modifier.border(1.dp, Accent, RoundedCornerShape(10.dp)) else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!randomOrder) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag",
                tint = TextTertiary,
                modifier = Modifier
                    .size(20.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { currentOnDragStart() },
                            onDrag = { change, amount ->
                                change.consume()
                                currentOnDrag(amount.y)
                            },
                            onDragEnd = { currentOnDragEnd() },
                            onDragCancel = { currentOnDragEnd() }
                        )
                    }
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        PlayerAvatar(name = player.name, size = 36.dp)
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(player.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(
                text = if (randomOrder) "Order randomised on start" else "Throws ${index + 1}${ordinalSuffix(index + 1)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun ordinalSuffix(n: Int): String = when {
    n % 100 in 11..13 -> "th"
    n % 10 == 1 -> "st"
    n % 10 == 2 -> "nd"
    n % 10 == 3 -> "rd"
    else -> "th"
}
