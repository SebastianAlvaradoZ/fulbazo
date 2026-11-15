package com.vapsa.fulbazo.ui.match.rate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vapsa.fulbazo.domain.model.FieldSkill
import com.vapsa.fulbazo.domain.model.GoalkeeperSkill
import com.vapsa.fulbazo.domain.model.MatchRole
import com.vapsa.fulbazo.domain.model.MatchStatus
import com.vapsa.fulbazo.domain.model.PlayerRating
import com.vapsa.fulbazo.domain.model.PlayerSummary
import com.vapsa.fulbazo.domain.model.SkillScale
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.CourtRepository
import com.vapsa.fulbazo.domain.repository.MatchRepository
import com.vapsa.fulbazo.domain.repository.PlayerRepository
import com.vapsa.fulbazo.domain.repository.RatingRepository
import com.vapsa.fulbazo.ui.match.MatchItem
import com.vapsa.fulbazo.ui.match.toItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class TeammateItem(
    val player: PlayerSummary,
    /** Rol con el que ya lo calificaste, o `null` si aún no. */
    val ratedAs: MatchRole?,
)

/** Calificación que se está editando en la hoja inferior. */
data class RatingDraft(
    val player: PlayerSummary,
    val role: MatchRole = MatchRole.FIELD,
    val fieldSkills: Map<FieldSkill, Int> = FieldSkill.entries.associateWith { SkillScale.DEFAULT },
    val goalkeeperSkills: Map<GoalkeeperSkill, Int> = GoalkeeperSkill.entries.associateWith { SkillScale.DEFAULT },
)

data class RatePlayersUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val loadFailed: Boolean = false,
    /** El partido no terminó o el usuario no jugó en él. */
    val notAllowed: Boolean = false,
    val match: MatchItem? = null,
    val teammates: List<TeammateItem> = emptyList(),
    val draft: RatingDraft? = null,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    /** Apodo del último jugador calificado, para mostrar la confirmación. */
    val savedNickname: String? = null,
) {
    val ratedCount: Int get() = teammates.count { it.ratedAs != null }
}

/**
 * Evaluar a los compañeros de un partido ya jugado.
 */
@HiltViewModel(assistedFactory = RatePlayersViewModel.Factory::class)
class RatePlayersViewModel(
    private val matchId: String,
    authRepository: AuthRepository,
    matchRepository: MatchRepository,
    courtRepository: CourtRepository,
    playerRepository: PlayerRepository,
    private val ratingRepository: RatingRepository,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) : ViewModel() {

    /** Constructor que usa Hilt; las pruebas usan el principal para fijar la hora y los tiempos. */
    @AssistedInject constructor(
        @Assisted matchId: String,
        authRepository: AuthRepository,
        matchRepository: MatchRepository,
        courtRepository: CourtRepository,
        playerRepository: PlayerRepository,
        ratingRepository: RatingRepository,
    ) : this(
        matchId = matchId,
        authRepository = authRepository,
        matchRepository = matchRepository,
        courtRepository = courtRepository,
        playerRepository = playerRepository,
        ratingRepository = ratingRepository,
        now = { LocalDateTime.now() },
    )

    private val userId: String? = authRepository.currentUser.value?.id

    private val draft = MutableStateFlow<RatingDraft?>(null)
    private val isSaving = MutableStateFlow(false)
    private val saveFailed = MutableStateFlow(false)
    private val savedNickname = MutableStateFlow<String?>(null)

    /** Últimas calificaciones dadas en este partido, para rellenar la hoja al editar. */
    private var myRatings: List<PlayerRating> = emptyList()

    private val content = combine(
        matchRepository.observeMatches(),
        courtRepository.observeCourts(),
        playerRepository.observePlayers(),
        userId?.let { ratingRepository.observeRatingsBy(it, matchId) } ?: flowOf(emptyList()),
    ) { matches, courts, players, ratings ->
        myRatings = ratings
        val match = matches.firstOrNull { it.id == matchId }
            ?: return@combine RatePlayersUiState(isLoading = false, notFound = true)
        val finished = match.status == MatchStatus.FINISHED || !match.dateTime.isAfter(now())
        val played = userId != null && userId in match.playerIds
        if (!finished || !played) return@combine RatePlayersUiState(isLoading = false, notAllowed = true)

        val ratedRoles = ratings.associate { it.ratedId to it.role }
        RatePlayersUiState(
            isLoading = false,
            match = match.toItem(courts.associateBy { it.id }, players, userId),
            teammates = match.playerIds
                .filter { it != userId }
                .mapNotNull { id -> players[id]?.let { TeammateItem(it, ratedRoles[id]) } },
        )
    }.catch { emit(RatePlayersUiState(isLoading = false, loadFailed = true)) }

    val uiState: StateFlow<RatePlayersUiState> = combine(
        content,
        draft,
        isSaving,
        saveFailed,
        savedNickname,
    ) { state, currentDraft, saving, failed, saved ->
        state.copy(draft = currentDraft, isSaving = saving, saveFailed = failed, savedNickname = saved)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RatePlayersUiState(),
    )

    /** Abre la hoja para calificar; si ya lo calificaste, la rellena con esos valores. */
    fun openRating(playerId: String) {
        val teammate = uiState.value.teammates.firstOrNull { it.player.id == playerId } ?: return
        val previous = myRatings.firstOrNull { it.ratedId == playerId }
        val defaults = RatingDraft(teammate.player)
        draft.value = if (previous == null) {
            defaults
        } else {
            defaults.copy(
                role = previous.role,
                fieldSkills = defaults.fieldSkills + previous.fieldSkills,
                goalkeeperSkills = defaults.goalkeeperSkills + previous.goalkeeperSkills,
            )
        }
    }

    fun dismissRating() {
        if (!isSaving.value) draft.value = null
    }

    fun onRoleChange(role: MatchRole) {
        draft.update { it?.copy(role = role) }
    }

    fun onFieldSkillChange(skill: FieldSkill, value: Int) {
        draft.update { it?.copy(fieldSkills = it.fieldSkills + (skill to SkillScale.clamp(value))) }
    }

    fun onGoalkeeperSkillChange(skill: GoalkeeperSkill, value: Int) {
        draft.update { it?.copy(goalkeeperSkills = it.goalkeeperSkills + (skill to SkillScale.clamp(value))) }
    }

    fun saveRating() {
        val current = draft.value ?: return
        val rater = userId ?: return
        if (isSaving.value) return

        // Solo se guardan las habilidades del rol elegido.
        val rating = PlayerRating(
            matchId = matchId,
            raterId = rater,
            ratedId = current.player.id,
            role = current.role,
            fieldSkills = if (current.role == MatchRole.FIELD) current.fieldSkills else emptyMap(),
            goalkeeperSkills = if (current.role == MatchRole.GOALKEEPER) current.goalkeeperSkills else emptyMap(),
        )
        isSaving.value = true
        saveFailed.value = false
        viewModelScope.launch {
            val result = ratingRepository.submitRating(rating)
            isSaving.value = false
            if (result.isSuccess) {
                draft.value = null
                savedNickname.value = current.player.nickname
            } else {
                saveFailed.value = true
            }
        }
    }

    fun onMessageShown() {
        savedNickname.value = null
        saveFailed.value = false
    }

    /** Hilt crea el ViewModel con el [matchId] que recibe la pantalla. */
    @AssistedFactory
    interface Factory {
        fun create(matchId: String): RatePlayersViewModel
    }
}
