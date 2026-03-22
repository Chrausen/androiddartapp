package com.clubdarts.ui.rankings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubdarts.data.model.Player
import com.clubdarts.data.db.dao.PlayerDao
import com.clubdarts.data.repository.EloRepository
import com.clubdarts.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class RankingsUiState(
    val leaderboard: List<Player> = emptyList(),
    val rankingEnabled: Boolean = false
)

@HiltViewModel
class RankingsViewModel @Inject constructor(
    private val playerDao: PlayerDao,
    private val eloRepository: EloRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<RankingsUiState> = combine(
        playerDao.getAllPlayers(),
        settingsRepository.observeRankingEnabled()
    ) { players, rankingEnabled ->
        RankingsUiState(
            leaderboard = eloRepository.getLeaderboard(players),
            rankingEnabled = rankingEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RankingsUiState()
    )
}
