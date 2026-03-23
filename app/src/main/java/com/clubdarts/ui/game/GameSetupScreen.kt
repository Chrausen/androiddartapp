package com.clubdarts.ui.game

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.R
import com.clubdarts.data.model.CheckoutRule
import com.clubdarts.data.model.GameType
import com.clubdarts.data.model.Player
import com.clubdarts.data.repository.GameConfig
import com.clubdarts.ui.game.components.PlayerAvatar
import com.clubdarts.ui.game.components.PlayerPickerSheet
import com.clubdarts.ui.players.PlayersViewModel
import com.clubdarts.ui.theme.*
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun GameSetupScreen(
    onStartGame: () -> Unit,
    gameViewModel: GameViewModel = hiltViewModel(),
    playersViewModel: PlayersViewModel = hiltViewModel()
) {
    val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()
    val playersState by playersViewModel.uiState.collectAsStateWithLifecycle()

    // Ranked config is locked to ranking settings (from ViewModel/Settings)
    val rankedStartScore = uiState.rankedStartScore
    val rankedCheckoutRule = uiState.rankedCheckoutRule
    val rankedLegsToWin = uiState.rankedLegsToWin

    var startScore by remember(uiState.setupDefaults.startScore) {
        mutableIntStateOf(uiState.setupDefaults.startScore)
    }
    var checkoutRule by remember(uiState.setupDefaults.checkoutRule) {
        mutableStateOf(uiState.setupDefaults.checkoutRule)
    }
    var legsToWin by remember(uiState.setupDefaults.legsToWin) {
        mutableIntStateOf(uiState.setupDefaults.legsToWin)
    }

    // Game mode state
    var gameMode by remember(uiState.setupDefaults.gameMode) {
        mutableStateOf(uiState.setupDefaults.gameMode)
    }

    // Game type state (X01 / Cricket)
    var gameType by remember(uiState.setupGameType) {
        mutableStateOf(uiState.setupGameType)
    }

    // Ranked/casual toggle — synced from ViewModel so it survives "New game" repeats
    var isRanked by remember { mutableStateOf(uiState.isRanked) }

    LaunchedEffect(uiState.isRanked) { isRanked = uiState.isRanked }

    // Single mode state
    var randomOrder by remember(uiState.setupDefaults.randomOrder) {
        mutableStateOf(uiState.setupDefaults.randomOrder)
    }
    var selectedPlayers by remember { mutableStateOf<List<Player>>(emptyList()) }

    // Teams mode state
    var teamAPlayers by remember { mutableStateOf<List<Player>>(emptyList()) }
    var teamBPlayers by remember { mutableStateOf<List<Player>>(emptyList()) }
    var showPlayerPicker by remember { mutableStateOf(false) }
    var settingsExpanded by remember { mutableStateOf(false) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (settingsExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "chevron"
    )

    // Restore single-mode selected players when returning from tab switch
    LaunchedEffect(uiState.setupSelectedPlayerIds, playersState.players) {
        if (selectedPlayers.isEmpty() && uiState.setupSelectedPlayerIds.isNotEmpty() && playersState.players.isNotEmpty()) {
            val map = playersState.players.associateBy { it.id }
            val restored = uiState.setupSelectedPlayerIds.mapNotNull { map[it] }
            if (restored.isNotEmpty()) selectedPlayers = restored
        }
    }

    // Restore team players when returning from tab switch
    LaunchedEffect(uiState.setupTeamAPlayerIds, uiState.setupTeamBPlayerIds, playersState.players) {
        val map = playersState.players.associateBy { it.id }
        if (teamAPlayers.isEmpty() && uiState.setupTeamAPlayerIds.isNotEmpty()) {
            teamAPlayers = uiState.setupTeamAPlayerIds.mapNotNull { map[it] }
        }
        if (teamBPlayers.isEmpty() && uiState.setupTeamBPlayerIds.isNotEmpty()) {
            teamBPlayers = uiState.setupTeamBPlayerIds.mapNotNull { map[it] }
        }
    }

    // Sync game mode from ViewModel on first load
    LaunchedEffect(uiState.setupGameMode) {
        gameMode = uiState.setupGameMode
    }

    // Sync game type from ViewModel
    LaunchedEffect(uiState.setupGameType) {
        gameType = uiState.setupGameType
    }

    // When ranked mode is toggled off, also update ViewModel
    LaunchedEffect(isRanked) {
        gameViewModel.setRanked(isRanked)
    }

    // Helpers to persist state across tab switches
    val setSelectedPlayers: (List<Player>) -> Unit = { players ->
        selectedPlayers = players
        gameViewModel.updateSetupSelectedPlayers(players.map { it.id })
    }
    val setTeamPlayers: (List<Player>, List<Player>) -> Unit = { a, b ->
        teamAPlayers = a
        teamBPlayers = b
        gameViewModel.updateSetupTeamPlayers(a.map { it.id }, b.map { it.id })
    }

    // Load recent players
    val recentPlayers = remember(uiState.setupDefaults.recentPlayerIds, playersState.players) {
        val ids = uiState.setupDefaults.recentPlayerIds
        val map = playersState.players.associateBy { it.id }
        ids.mapNotNull { map[it] }
    }

    // Already-selected IDs for the picker (union of both teams or single list)
    val pickerSelectedIds = remember(gameMode, selectedPlayers, teamAPlayers, teamBPlayers) {
        if (gameMode == GameMode.TEAMS)
            (teamAPlayers.map { it.id } + teamBPlayers.map { it.id }).toSet()
        else
            selectedPlayers.map { it.id }.toSet()
    }

    // All players selected (for enable check)
    // Ranked requires at least 2 players
    val hasPlayers = if (isRanked) {
        selectedPlayers.size >= 2
    } else if (gameMode == GameMode.TEAMS) {
        teamAPlayers.isNotEmpty() && teamBPlayers.isNotEmpty()
    } else {
        selectedPlayers.isNotEmpty()
    }

    // Pre-resolved labels for SegmentedRow lambdas (can't call stringResource inside non-composable lambdas)
    val gameSingleLabel = stringResource(R.string.game_mode_single)
    val gameTeamsLabel = stringResource(R.string.game_mode_teams)
    val casualLabel = stringResource(R.string.game_casual)
    val rankedLabel = stringResource(R.string.game_ranked)
    val x01Label = stringResource(R.string.game_type_x01)
    val cricketLabel = stringResource(R.string.game_type_cricket)

    // Max players: Cricket supports up to 4; X01 is unlimited
    val maxPlayers = if (!isRanked && gameType == GameType.CRICKET) 4 else Int.MAX_VALUE
    val totalPlayerCount = if (gameMode == GameMode.TEAMS)
        teamAPlayers.size + teamBPlayers.size
    else
        selectedPlayers.size
    val canAddMorePlayers = totalPlayerCount < maxPlayers

    // Effective config values (ranked uses locked settings, casual uses user choice)
    val effectiveStartScore = if (isRanked) rankedStartScore else startScore
    val effectiveCheckoutRule = if (isRanked) rankedCheckoutRule else checkoutRule
    val effectiveLegsToWin = if (isRanked) rankedLegsToWin else legsToWin

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
                    text = stringResource(R.string.game_new_game),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            }

            // Match type toggle (Ranked / Casual) — only shown when ranking is enabled
            if (uiState.rankingEnabled) {
                item {
                    SectionLabel(stringResource(R.string.game_match_type))
                    SegmentedRow(
                        options = listOf(false, true),
                        selected = isRanked,
                        onSelect = { ranked ->
                            isRanked = ranked
                            if (ranked) {
                                gameMode = GameMode.SINGLE
                                gameViewModel.updateSetupGameMode(GameMode.SINGLE)
                            }
                        },
                        label = { if (!it) casualLabel else rankedLabel }
                    )
                }
            }

            // Game settings — locked summary shown for ranked, collapsible for casual
            item {
                SectionLabel(stringResource(R.string.game_options_label))
                Spacer(modifier = Modifier.height(6.dp))
                if (isRanked) {
                    // Locked game mode summary for ranked
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.game_starting_score), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(rankedStartScore.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Checkout", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                rankedCheckoutRule.name.lowercase().replaceFirstChar { c -> c.uppercaseChar() },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.game_legs_to_win), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(rankedLegsToWin.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.game_mode_display), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(stringResource(R.string.game_ranked), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Accent)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { settingsExpanded = !settingsExpanded }
                    ) {
                        // Collapsed summary
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
                                    Text(stringResource(R.string.game_mode_label), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text(
                                        if (gameMode == GameMode.TEAMS) gameTeamsLabel else gameSingleLabel,
                                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(stringResource(R.string.game_type_label), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text(
                                        if (gameType == GameType.CRICKET) cricketLabel else x01Label,
                                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary
                                    )
                                }
                                if (gameType == GameType.X01) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(stringResource(R.string.game_starting_score), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        Text(startScore.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Checkout", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        Text(
                                            checkoutRule.name.lowercase().replaceFirstChar { c -> c.uppercaseChar() },
                                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(stringResource(R.string.game_legs_to_win), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text(legsToWin.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                        }

                        // Expanded options
                        AnimatedVisibility(
                            visible = settingsExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                // Game mode: Single / Teams
                                SectionLabel(stringResource(R.string.game_mode_label))
                                SegmentedRow(
                                    options = GameMode.values().toList(),
                                    selected = gameMode,
                                    onSelect = { newMode ->
                                        if (newMode != gameMode) {
                                            if (newMode == GameMode.TEAMS) {
                                                val mid = (selectedPlayers.size + 1) / 2
                                                setTeamPlayers(selectedPlayers.take(mid), selectedPlayers.drop(mid))
                                            } else {
                                                setSelectedPlayers(teamAPlayers + teamBPlayers)
                                            }
                                            gameMode = newMode
                                            gameViewModel.updateSetupGameMode(newMode)
                                        }
                                    },
                                    label = { if (it == GameMode.SINGLE) gameSingleLabel else gameTeamsLabel }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Game type: X01 / Cricket
                                SectionLabel(stringResource(R.string.game_type_label))
                                SegmentedRow(
                                    options = listOf(GameType.X01, GameType.CRICKET),
                                    selected = gameType,
                                    onSelect = { newType ->
                                        gameType = newType
                                        gameViewModel.updateSetupGameType(newType)
                                    },
                                    label = { if (it == GameType.X01) x01Label else cricketLabel }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // X01-only options
                                if (gameType == GameType.X01) {
                                    SectionLabel(stringResource(R.string.game_starting_score))
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
                                }

                                // Legs to win (both game types)
                                SectionLabel(stringResource(R.string.game_legs_to_win))
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
                                contentDescription = if (settingsExpanded) stringResource(R.string.live_collapse_settings) else stringResource(R.string.live_expand_settings),
                                tint = TextTertiary,
                                modifier = Modifier
                                    .size(28.dp)
                                    .rotate(chevronRotation)
                            )
                        }
                    }
                }
            }

            // Players section — ranked forces single mode
            if (isRanked || gameMode == GameMode.SINGLE) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.game_players_section) + " ", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                            Text(
                                stringResource(R.string.game_selected_count, selectedPlayers.size),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isRanked && selectedPlayers.size < 2) Amber else TextPrimary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.game_random_order), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Checkbox(
                                checked = randomOrder,
                                onCheckedChange = { randomOrder = it },
                                colors = CheckboxDefaults.colors(checkedColor = Accent)
                            )
                        }
                    }
                }

                item {
                    SinglePlayerList(
                        selectedPlayers = selectedPlayers,
                        randomOrder = randomOrder,
                        onPlayersChanged = { newList -> setSelectedPlayers(newList) }
                    )
                }

                if (canAddMorePlayers) {
                    item {
                        AddPlayerButton(onClick = { showPlayerPicker = true })
                    }
                }
            } else {
                // ---- Teams mode ----
                item {
                    Text(stringResource(R.string.game_teams_section), style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                }

                item {
                    TeamsPlayerSection(
                        teamAPlayers = teamAPlayers,
                        teamBPlayers = teamBPlayers,
                        onTeamsChanged = setTeamPlayers
                    )
                }

                if (canAddMorePlayers) {
                    item {
                        AddPlayerButton(onClick = { showPlayerPicker = true })
                    }
                }
            }
        }

        // Start button — sticky at bottom
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // Ranked validation hint
            if (isRanked && selectedPlayers.size < 2) {
                Text(
                    text = stringResource(R.string.game_ranked_min_players),
                    style = MaterialTheme.typography.labelSmall,
                    color = Amber,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Button(
                onClick = {
                    if (isRanked) {
                        val orderedPlayers = if (randomOrder) selectedPlayers.shuffled() else selectedPlayers
                        val config = GameConfig(
                            startScore = rankedStartScore,
                            checkoutRule = rankedCheckoutRule,
                            legsToWin = rankedLegsToWin,
                            isSolo = false,
                            playerIds = orderedPlayers.map { it.id },
                            isRanked = true,
                            gameType = GameType.X01
                        )
                        gameViewModel.startGame(config)
                    } else if (gameMode == GameMode.TEAMS) {
                        val interleaved = interleaveTeams(teamAPlayers, teamBPlayers)
                        val assignments = teamAPlayers.associate { it.id to 0 } + teamBPlayers.associate { it.id to 1 }
                        val config = GameConfig(
                            startScore = startScore,
                            checkoutRule = checkoutRule,
                            legsToWin = legsToWin,
                            isSolo = false,
                            playerIds = interleaved.map { it.id },
                            isTeamGame = true,
                            teamAssignments = assignments,
                            gameType = gameType
                        )
                        if (gameType == GameType.CRICKET) {
                            gameViewModel.startCricketGame(config)
                        } else {
                            gameViewModel.startGame(config)
                        }
                    } else {
                        val orderedPlayers = if (randomOrder) selectedPlayers.shuffled() else selectedPlayers
                        val config = GameConfig(
                            startScore = startScore,
                            checkoutRule = checkoutRule,
                            legsToWin = legsToWin,
                            isSolo = orderedPlayers.size == 1,
                            playerIds = orderedPlayers.map { it.id },
                            gameType = gameType
                        )
                        if (gameType == GameType.CRICKET) {
                            gameViewModel.startCricketGame(config)
                        } else {
                            gameViewModel.startGame(config)
                        }
                    }
                    onStartGame()
                },
                enabled = hasPlayers,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRanked) Accent else Accent,
                    disabledContainerColor = Surface3,
                    contentColor = Background,
                    disabledContentColor = TextTertiary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (isRanked) stringResource(R.string.game_start_ranked) else stringResource(R.string.game_start),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showPlayerPicker) {
        PlayerPickerSheet(
            allPlayers = playersState.players,
            recentPlayers = recentPlayers,
            selectedPlayerIds = pickerSelectedIds,
            onCreatePlayer = { name -> playersViewModel.insertPlayerByName(name) },
            onPlayerSelected = { player ->
                if (isRanked) {
                    if (player.id !in selectedPlayers.map { it.id }) {
                        setSelectedPlayers(selectedPlayers + player)
                    }
                } else if (gameMode == GameMode.TEAMS) {
                    val allIds = teamAPlayers.map { it.id } + teamBPlayers.map { it.id }
                    if (player.id !in allIds && allIds.size < maxPlayers) {
                        if (teamAPlayers.size <= teamBPlayers.size) {
                            setTeamPlayers(teamAPlayers + player, teamBPlayers)
                        } else {
                            setTeamPlayers(teamAPlayers, teamBPlayers + player)
                        }
                    }
                } else {
                    if (player.id !in selectedPlayers.map { it.id } && selectedPlayers.size < maxPlayers) {
                        setSelectedPlayers(selectedPlayers + player)
                    }
                }
            },
            onDismiss = { showPlayerPicker = false }
        )
    }
}

// ---- Two-column teams layout ----
@Composable
private fun TeamsPlayerSection(
    teamAPlayers: List<Player>,
    teamBPlayers: List<Player>,
    onTeamsChanged: (List<Player>, List<Player>) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Team A (Red)
        TeamColumn(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.result_team_a),
            labelColor = Red,
            players = teamAPlayers,
            onPlayersReordered = { newList -> onTeamsChanged(newList, teamBPlayers) },
            onMoveToOther = { player ->
                onTeamsChanged(
                    teamAPlayers.filter { it.id != player.id },
                    teamBPlayers + player
                )
            }
        )

        // Team B (Blue)
        TeamColumn(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.result_team_b),
            labelColor = Blue,
            players = teamBPlayers,
            onPlayersReordered = { newList -> onTeamsChanged(teamAPlayers, newList) },
            onMoveToOther = { player ->
                onTeamsChanged(
                    teamAPlayers + player,
                    teamBPlayers.filter { it.id != player.id }
                )
            }
        )
    }
}

@Composable
private fun TeamColumn(
    label: String,
    labelColor: Color,
    players: List<Player>,
    onPlayersReordered: (List<Player>) -> Unit,
    onMoveToOther: (Player) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableIntStateOf(0) }
    val rowSpacingPx = with(LocalDensity.current) { 8.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()
    val returnAnim = remember { Animatable(0f) }
    var returningPlayerId by remember { mutableStateOf<Long?>(null) }

    Column(modifier = modifier) {
        // Team header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(labelColor, RoundedCornerShape(5.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = labelColor
            )
        }

        // Player rows
        val rowTotal = (rowHeightPx + rowSpacingPx).takeIf { it > 0f } ?: 60f
        val dropTargetIndex = if (draggingIndex >= 0) {
            (draggingIndex + (dragOffsetY / rowTotal).roundToInt())
                .coerceIn(0, players.size - 1)
        } else -1

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            players.forEachIndexed { index, player ->
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

                    TeamPlayerRow(
                        modifier = Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .offset { IntOffset(0, visualOffset.roundToInt()) },
                        player = player,
                        teamColor = labelColor,
                        isDragging = isDragging,
                        onMoveToOther = { onMoveToOther(player) },
                        onRemove = { onPlayersReordered(players.filter { it.id != player.id }) },
                        onSizeChanged = { h -> if (rowHeightPx == 0) rowHeightPx = h },
                        onDragStart = { draggingIndex = index; dragOffsetY = 0f },
                        onDrag = { dy -> dragOffsetY += dy },
                        onDragEnd = {
                            val rTotal = (rowHeightPx + rowSpacingPx).takeIf { it > 0f } ?: 60f
                            val shift = (dragOffsetY / rTotal).roundToInt()
                            val target = (draggingIndex + shift).coerceIn(0, players.size - 1)
                            val remainder = dragOffsetY - shift * rTotal
                            val droppedId = players.getOrNull(draggingIndex)?.id
                            if (target != draggingIndex) {
                                val newList = players.toMutableList()
                                newList.add(target, newList.removeAt(draggingIndex))
                                onPlayersReordered(newList)
                            }
                            draggingIndex = -1
                            dragOffsetY = 0f
                            if (droppedId != null) {
                                returningPlayerId = droppedId
                                coroutineScope.launch {
                                    returnAnim.snapTo(remainder)
                                    returnAnim.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                    returningPlayerId = null
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamPlayerRow(
    player: Player,
    teamColor: Color,
    isDragging: Boolean,
    onMoveToOther: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
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
            .then(if (isDragging) Modifier.shadow(6.dp, RoundedCornerShape(8.dp)) else Modifier)
            .background(
                color = if (isDragging) Surface3 else Surface2,
                shape = RoundedCornerShape(8.dp)
            )
            .border(1.dp, teamColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drag handle
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Drag",
            tint = TextTertiary,
            modifier = Modifier
                .size(16.dp)
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
        Spacer(modifier = Modifier.width(4.dp))

        // Player name
        Text(
            text = player.name,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        // Move to other team arrow
        IconButton(
            onClick = onMoveToOther,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = stringResource(R.string.game_move_to_other_team),
                tint = TextTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ---- Single mode player list ----
@Composable
private fun SinglePlayerList(
    selectedPlayers: List<Player>,
    randomOrder: Boolean,
    onPlayersChanged: (List<Player>) -> Unit
) {
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableIntStateOf(0) }
    val rowSpacingPx = with(LocalDensity.current) { 16.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()
    val returnAnim = remember { Animatable(0f) }
    var returningPlayerId by remember { mutableStateOf<Long?>(null) }

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
                    onRemove = { onPlayersChanged(selectedPlayers.filter { it.id != player.id }) },
                    onSizeChanged = { h -> if (rowHeightPx == 0) rowHeightPx = h },
                    onDragStart = { draggingIndex = index; dragOffsetY = 0f },
                    onDrag = { dy -> dragOffsetY += dy },
                    onDragEnd = {
                        val rowTotalEnd = (rowHeightPx + rowSpacingPx).takeIf { it > 0f } ?: 80f
                        val shift = (dragOffsetY / rowTotalEnd).roundToInt()
                        val target = (draggingIndex + shift)
                            .coerceIn(0, selectedPlayers.size - 1)
                        val remainder = dragOffsetY - shift * rowTotalEnd
                        val droppedId = selectedPlayers.getOrNull(draggingIndex)?.id
                        if (target != draggingIndex) {
                            val newList = selectedPlayers.toMutableList()
                            newList.add(target, newList.removeAt(draggingIndex))
                            onPlayersChanged(newList)
                        }
                        draggingIndex = -1
                        dragOffsetY = 0f
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

@Composable
private fun AddPlayerButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.dp, Border2, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Add, contentDescription = null, tint = TextSecondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.game_add_player), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
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

/** Interleave two team player lists for throw order: T1P1, T2P1, T1P2, T2P2, ... */
fun interleaveTeams(teamA: List<Player>, teamB: List<Player>): List<Player> = buildList {
    val max = maxOf(teamA.size, teamB.size)
    for (i in 0 until max) {
        if (i < teamA.size) add(teamA[i])
        if (i < teamB.size) add(teamB[i])
    }
}
