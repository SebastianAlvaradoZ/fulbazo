package com.vapsa.fulbazo.ui.match.teams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vapsa.fulbazo.domain.ai.LocalTeamBalancer
import com.vapsa.fulbazo.domain.model.PlayerProfile
import com.vapsa.fulbazo.domain.model.PlayerSummary
import com.vapsa.fulbazo.domain.model.TeamPlayer
import com.vapsa.fulbazo.domain.model.TeamSplit
import com.vapsa.fulbazo.domain.repository.AiRepository
import com.vapsa.fulbazo.domain.repository.MatchRepository
import com.vapsa.fulbazo.domain.repository.PlayerRepository
import com.vapsa.fulbazo.domain.repository.StatsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Un jugador ya ubicado en un equipo, listo para mostrarse. */
data class TeamMember(val id: String, val nickname: String, val isGoalkeeper: Boolean)

sealed interface TeamsUiState {
    data object Idle : TeamsUiState

    data object Loading : TeamsUiState

    data class Ready(
        val teamA: List<TeamMember>,
        val teamB: List<TeamMember>,
        val explanation: String,
        /** `false` si es el reparto automático de respaldo (sin IA). */
        val generatedByAi: Boolean,
    ) : TeamsUiState

    /** Gemini falló; se puede reintentar o usar el reparto automático. */
    data object Error : TeamsUiState
}

/**
 * "Armar equipos con IA": envía a Gemini las estadísticas de los jugadores del
 * partido y muestra los dos equipos que propone. No se guarda: es una sugerencia.
 */
@HiltViewModel(assistedFactory = TeamBuilderViewModel.Factory::class)
class TeamBuilderViewModel @AssistedInject constructor(
    @Assisted private val matchId: String,
    private val matchRepository: MatchRepository,
    private val playerRepository: PlayerRepository,
    private val statsRepository: StatsRepository,
    private val aiRepository: AiRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TeamsUiState>(TeamsUiState.Idle)
    val uiState: StateFlow<TeamsUiState> = _uiState.asStateFlow()

    /** Jugadores de la última vez, para el reparto de respaldo sin volver a cargarlos. */
    private var lastPlayers: List<TeamPlayer> = emptyList()
    private var job: Job? = null

    fun buildTeams() {
        if (_uiState.value == TeamsUiState.Loading) return
        _uiState.value = TeamsUiState.Loading
        job = viewModelScope.launch {
            val players = try {
                loadPlayers()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = TeamsUiState.Error
                return@launch
            }
            lastPlayers = players
            if (players.size < MIN_PLAYERS) {
                _uiState.value = TeamsUiState.Error
                return@launch
            }
            _uiState.value = aiRepository.buildTeams(players).fold(
                onSuccess = { it.toUiState(players) },
                onFailure = { TeamsUiState.Error },
            )
        }
    }

    /** Reparto automático por nivel, marcado como "sin IA". */
    fun useLocalSplit() {
        if (lastPlayers.size < MIN_PLAYERS) return
        _uiState.value = LocalTeamBalancer.split(lastPlayers).toUiState(lastPlayers)
    }

    fun dismiss() {
        job?.cancel()
        _uiState.value = TeamsUiState.Idle
    }

    private suspend fun loadPlayers(): List<TeamPlayer> = coroutineScope {
        val match = matchRepository.observeMatches().first().first { it.id == matchId }
        val directory = playerRepository.observePlayers().first()
        match.playerIds.map { id ->
            async { toTeamPlayer(id, directory[id], statsRepository.observeProfile(id).first()) }
        }.awaitAll()
    }

    private fun toTeamPlayer(id: String, summary: PlayerSummary?, profile: PlayerProfile): TeamPlayer {
        val self = profile.selfAssessment
        val field = profile.community.asFieldPlayer
        return TeamPlayer(
            id = id,
            nickname = summary?.nickname ?: id,
            mainPosition = self?.mainPosition,
            isGoalkeeper = self?.isGoalkeeper == true,
            selfFieldLevel = self?.fieldOverall,
            communityFieldLevel = field?.overall,
            communityVotes = field?.votes ?: 0,
            goalkeeperLevel = profile.community.asGoalkeeper?.overall ?: self?.goalkeeperOverall,
        )
    }

    private fun TeamSplit.toUiState(players: List<TeamPlayer>): TeamsUiState.Ready {
        val byId = players.associateBy { it.id }
        fun members(ids: List<String>) = ids.mapNotNull { byId[it] }.map { TeamMember(it.id, it.nickname, it.isGoalkeeper) }
        return TeamsUiState.Ready(members(teamA), members(teamB), explanation, generatedByAi)
    }

    companion object {
        const val MIN_PLAYERS = 2
    }

    /** Hilt crea el ViewModel con el [matchId] del partido. */
    @AssistedFactory
    interface Factory {
        fun create(matchId: String): TeamBuilderViewModel
    }
}
