package com.vapsa.fulbazo.ui.invitations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vapsa.fulbazo.domain.model.InvitationStatus
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.CourtRepository
import com.vapsa.fulbazo.domain.repository.InvitationRepository
import com.vapsa.fulbazo.domain.repository.MatchRepository
import com.vapsa.fulbazo.domain.repository.PlayerRepository
import com.vapsa.fulbazo.ui.match.MatchItem
import com.vapsa.fulbazo.ui.match.toItem
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
import javax.inject.Inject

data class InvitationItem(
    val invitationId: String,
    val fromNickname: String,
    val status: InvitationStatus,
    /** `null` si el partido ya no existe. */
    val match: MatchItem?,
)

enum class InvitationActionError { MATCH_FULL, FAILED }

data class InvitationsUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val pending: List<InvitationItem> = emptyList(),
    val answered: List<InvitationItem> = emptyList(),
    val busyIds: Set<String> = emptySet(),
    val actionError: InvitationActionError? = null,
) {
    val isEmpty: Boolean get() = pending.isEmpty() && answered.isEmpty()
}

@HiltViewModel
class InvitationsViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val invitationRepository: InvitationRepository,
    private val matchRepository: MatchRepository,
    courtRepository: CourtRepository,
    playerRepository: PlayerRepository,
) : ViewModel() {

    private val userId: String? = authRepository.currentUser.value?.id
    private val busyIds = MutableStateFlow<Set<String>>(emptySet())
    private val actionError = MutableStateFlow<InvitationActionError?>(null)

    private val items = combine(
        userId?.let { invitationRepository.observeReceived(it) } ?: flowOf(emptyList()),
        matchRepository.observeMatches(),
        courtRepository.observeCourts(),
        playerRepository.observePlayers(),
    ) { invitations, matches, courts, players ->
        val matchesById = matches.associateBy { it.id }
        val courtsById = courts.associateBy { it.id }
        invitations.map { invitation ->
            InvitationItem(
                invitationId = invitation.id,
                fromNickname = players[invitation.fromUserId]?.nickname ?: invitation.fromUserId,
                status = invitation.status,
                match = matchesById[invitation.matchId]?.toItem(courtsById, players, userId),
            )
        }
    }

    val uiState: StateFlow<InvitationsUiState> = combine(items, busyIds, actionError) { list, busy, error ->
        InvitationsUiState(
            isLoading = false,
            pending = list.filter { it.status == InvitationStatus.PENDING },
            answered = list.filter { it.status != InvitationStatus.PENDING },
            busyIds = busy,
            actionError = error,
        )
    }
        .catch { emit(InvitationsUiState(isLoading = false, loadFailed = true)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InvitationsUiState(),
        )

    /** Aceptar une al jugador al partido; si está lleno, la invitación sigue pendiente. */
    fun respond(invitationId: String, accept: Boolean) {
        val user = userId ?: return
        if (invitationId in busyIds.value) return
        val matchId = uiState.value.pending.firstOrNull { it.invitationId == invitationId }?.match?.matchId

        busyIds.update { it + invitationId }
        viewModelScope.launch {
            if (accept) {
                val joined = matchId?.let { matchRepository.joinMatch(it, user) }
                if (joined == null || joined.isFailure) {
                    actionError.value = InvitationActionError.MATCH_FULL
                    busyIds.update { it - invitationId }
                    return@launch
                }
            }
            val result = invitationRepository.respond(invitationId, accept)
            if (result.isFailure) actionError.value = InvitationActionError.FAILED
            busyIds.update { it - invitationId }
        }
    }

    fun onErrorShown() {
        actionError.value = null
    }
}
