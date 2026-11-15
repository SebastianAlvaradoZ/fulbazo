package com.vapsa.fulbazo.ui.match.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vapsa.fulbazo.di.AppContainer
import com.vapsa.fulbazo.domain.model.MatchLevel
import com.vapsa.fulbazo.domain.model.NewMatch
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.CourtRepository
import com.vapsa.fulbazo.domain.repository.FriendRepository
import com.vapsa.fulbazo.domain.repository.GroupRepository
import com.vapsa.fulbazo.domain.repository.InvitationRepository
import com.vapsa.fulbazo.domain.repository.MatchRepository
import com.vapsa.fulbazo.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class CreateMatchViewModel(
    private val authRepository: AuthRepository,
    courtRepository: CourtRepository,
    private val matchRepository: MatchRepository,
    playerRepository: PlayerRepository,
    friendRepository: FriendRepository,
    groupRepository: GroupRepository,
    private val invitationRepository: InvitationRepository,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
    initialCourtId: String? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateMatchUiState(courtId = initialCourtId))
    val uiState: StateFlow<CreateMatchUiState> = _uiState.asStateFlow()

    init {
        val userId = authRepository.currentUser.value?.id
        viewModelScope.launch {
            combine(
                courtRepository.observeCourts(),
                playerRepository.observePlayers(),
                userId?.let { friendRepository.observeFriendIds(it) } ?: flowOf(emptySet()),
                userId?.let { groupRepository.observeGroups(it) } ?: flowOf(emptyList()),
            ) { courts, players, friendIds, groups ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        courts = courts,
                        friends = friendIds.mapNotNull { players[it] }.sortedBy { it.nickname.lowercase() },
                        groups = groups,
                    )
                }
            }.collect {}
        }
    }

    /** Solo acepta losas de la lista cargada. */
    fun onCourtSelected(courtId: String) = updateForm {
        if (courts.none { it.id == courtId }) {
            this
        } else {
            copy(courtId = courtId, errors = errors - CreateMatchError.COURT_REQUIRED)
        }
    }

    fun onDateChange(date: LocalDate) = updateForm {
        copy(date = date, errors = errors - CreateMatchError.DATE_REQUIRED - CreateMatchError.DATE_IN_PAST)
    }

    fun onTimeChange(time: LocalTime) = updateForm {
        copy(time = time, errors = errors - CreateMatchError.TIME_REQUIRED - CreateMatchError.DATE_IN_PAST)
    }

    fun onSlotsChange(slots: Int) = updateForm {
        copy(slots = slots.coerceIn(NewMatch.MIN_SLOTS, NewMatch.MAX_SLOTS))
    }

    fun onLevelChange(level: MatchLevel) = updateForm { copy(level = level) }

    fun onDescriptionChange(text: String) = updateForm {
        copy(description = text, errors = errors - CreateMatchError.DESCRIPTION_TOO_LONG)
    }

    fun onToggleFriend(friendId: String) = updateForm {
        copy(invitedFriendIds = invitedFriendIds.toggle(friendId))
    }

    fun onToggleGroup(groupId: String) = updateForm {
        copy(invitedGroupIds = invitedGroupIds.toggle(groupId))
    }

    fun createMatch() {
        val state = _uiState.value
        if (state.isSaving) return
        val hostId = authRepository.currentUser.value?.id ?: return

        val errors = validate(state)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }

        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            val newMatch = NewMatch(
                courtId = requireNotNull(state.courtId),
                dateTime = LocalDateTime.of(requireNotNull(state.date), requireNotNull(state.time)),
                slots = state.slots,
                level = state.level,
                description = state.description.trim(),
            )
            val match = matchRepository.createMatch(hostId, newMatch).getOrElse {
                _uiState.update { it.copy(isSaving = false, saveFailed = true) }
                return@launch
            }
            val invitees = state.inviteeIds - hostId
            if (invitees.isNotEmpty()) {
                // Si falla el envío, el partido ya existe: se informa igual como creado.
                invitationRepository.sendInvitations(match.id, hostId, invitees)
            }
            _uiState.update { it.copy(isSaving = false, createdMatchId = match.id) }
        }
    }

    /** La vista llama a esto después de navegar o mostrar el error. */
    fun onResultHandled() {
        _uiState.update { it.copy(createdMatchId = null, saveFailed = false) }
    }

    private fun validate(state: CreateMatchUiState): Set<CreateMatchError> = buildSet {
        if (state.courtId == null) add(CreateMatchError.COURT_REQUIRED)
        if (state.date == null) add(CreateMatchError.DATE_REQUIRED)
        if (state.time == null) add(CreateMatchError.TIME_REQUIRED)
        if (state.date != null && state.time != null &&
            !LocalDateTime.of(state.date, state.time).isAfter(now())
        ) {
            add(CreateMatchError.DATE_IN_PAST)
        }
        if (state.description.trim().length > NewMatch.MAX_DESCRIPTION_LENGTH) {
            add(CreateMatchError.DESCRIPTION_TOO_LONG)
        }
    }

    private fun updateForm(transform: CreateMatchUiState.() -> CreateMatchUiState) {
        _uiState.update { it.transform().copy(saveFailed = false) }
    }

    companion object {
        /** [courtId] permite abrir el formulario con una losa ya elegida. */
        fun factory(courtId: String? = null): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CreateMatchViewModel(
                    authRepository = AppContainer.authRepository,
                    courtRepository = AppContainer.courtRepository,
                    matchRepository = AppContainer.matchRepository,
                    playerRepository = AppContainer.playerRepository,
                    friendRepository = AppContainer.friendRepository,
                    groupRepository = AppContainer.groupRepository,
                    invitationRepository = AppContainer.invitationRepository,
                    initialCourtId = courtId,
                )
            }
        }
    }
}

private fun Set<String>.toggle(id: String): Set<String> = if (id in this) this - id else this + id
