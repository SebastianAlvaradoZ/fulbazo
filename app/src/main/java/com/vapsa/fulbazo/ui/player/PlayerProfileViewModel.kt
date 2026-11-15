package com.vapsa.fulbazo.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vapsa.fulbazo.domain.model.CommunityStats
import com.vapsa.fulbazo.domain.model.MatchStatus
import com.vapsa.fulbazo.domain.model.PlayerRelation
import com.vapsa.fulbazo.domain.model.PlayerSummary
import com.vapsa.fulbazo.domain.model.SelfAssessment
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.CourtRepository
import com.vapsa.fulbazo.domain.repository.FriendRepository
import com.vapsa.fulbazo.domain.repository.InvitationRepository
import com.vapsa.fulbazo.domain.repository.MatchRepository
import com.vapsa.fulbazo.domain.repository.PlayerRepository
import com.vapsa.fulbazo.domain.repository.StatsRepository
import com.vapsa.fulbazo.ui.match.MatchItem
import com.vapsa.fulbazo.ui.match.toItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

enum class PlayerProfileMessage { REQUEST_SENT, REQUEST_ACCEPTED, INVITED }

data class PlayerProfileUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val loadFailed: Boolean = false,
    /** Es el perfil del propio usuario: la vista redirige a la pestaña Perfil. */
    val isMe: Boolean = false,
    val player: PlayerSummary? = null,
    val self: SelfAssessment? = null,
    val community: CommunityStats = CommunityStats(),
    val relation: PlayerRelation = PlayerRelation.NONE,
    /** Solicitud de amistad que este jugador me envió, si existe. */
    val incomingRequestId: String? = null,
    /** Partidos (de cualquier fecha) en los que ambos estuvimos inscritos. */
    val matchesTogether: Int = 0,
    /** Mis partidos próximos como anfitrión a los que puedo invitarlo. */
    val invitableMatches: List<MatchItem> = emptyList(),
    val isBusy: Boolean = false,
    val actionFailed: Boolean = false,
    val message: PlayerProfileMessage? = null,
)

/**
 * Perfil público de otro jugador: datos, estadísticas, amistad e invitación a partidos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = PlayerProfileViewModel.Factory::class)
class PlayerProfileViewModel(
    private val playerId: String,
    authRepository: AuthRepository,
    playerRepository: PlayerRepository,
    statsRepository: StatsRepository,
    private val friendRepository: FriendRepository,
    matchRepository: MatchRepository,
    courtRepository: CourtRepository,
    private val invitationRepository: InvitationRepository,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) : ViewModel() {

    /** Constructor que usa Hilt; las pruebas usan el principal para fijar la hora y los tiempos. */
    @AssistedInject constructor(
        @Assisted playerId: String,
        authRepository: AuthRepository,
        playerRepository: PlayerRepository,
        statsRepository: StatsRepository,
        friendRepository: FriendRepository,
        matchRepository: MatchRepository,
        courtRepository: CourtRepository,
        invitationRepository: InvitationRepository,
    ) : this(
        playerId = playerId,
        authRepository = authRepository,
        playerRepository = playerRepository,
        statsRepository = statsRepository,
        friendRepository = friendRepository,
        matchRepository = matchRepository,
        courtRepository = courtRepository,
        invitationRepository = invitationRepository,
        now = { LocalDateTime.now() },
    )

    private val myId: String? = authRepository.currentUser.value?.id

    private val reloadTrigger = MutableStateFlow(0)
    private val isBusy = MutableStateFlow(false)
    private val actionFailed = MutableStateFlow(false)
    private val message = MutableStateFlow<PlayerProfileMessage?>(null)

    private val social = combine(
        myId?.let { friendRepository.observeFriendIds(it) } ?: flowOf(emptySet()),
        myId?.let { friendRepository.observePendingRequests(it) } ?: flowOf(emptyList()),
        invitationRepository.observeReceived(playerId),
    ) { friendIds, requests, invitationsToPlayer ->
        Triple(friendIds, requests, invitationsToPlayer)
    }

    private val content = reloadTrigger.flatMapLatest {
        combine(
            playerRepository.observePlayers(),
            statsRepository.observeProfile(playerId),
            matchRepository.observeMatches(),
            courtRepository.observeCourts(),
            social,
        ) { players, stats, matches, courts, (friendIds, requests, invitationsToPlayer) ->
            val player = players[playerId]
                ?: return@combine PlayerProfileUiState(isLoading = false, notFound = true)
            val incoming = requests.firstOrNull { it.fromUserId == playerId && it.toUserId == myId }
            val relation = when {
                playerId in friendIds -> PlayerRelation.FRIEND
                requests.any { it.fromUserId == myId && it.toUserId == playerId } -> PlayerRelation.REQUEST_SENT
                incoming != null -> PlayerRelation.REQUEST_RECEIVED
                else -> PlayerRelation.NONE
            }
            val alreadyInvitedTo = invitationsToPlayer.filter { it.fromUserId == myId }.map { it.matchId }.toSet()
            val courtsById = courts.associateBy { it.id }

            PlayerProfileUiState(
                isLoading = false,
                isMe = playerId == myId,
                player = player,
                self = stats.selfAssessment,
                community = stats.community,
                relation = relation,
                incomingRequestId = incoming?.id,
                matchesTogether = matches.count { myId != null && myId in it.playerIds && playerId in it.playerIds },
                invitableMatches = matches
                    .filter {
                        it.hostId == myId &&
                            it.status == MatchStatus.OPEN &&
                            it.dateTime.isAfter(now()) &&
                            playerId !in it.playerIds &&
                            it.id !in alreadyInvitedTo
                    }
                    .sortedBy { it.dateTime }
                    .map { it.toItem(courtsById, players, myId) },
            )
        }.catch { emit(PlayerProfileUiState(isLoading = false, loadFailed = true)) }
    }

    val uiState: StateFlow<PlayerProfileUiState> = combine(
        content,
        isBusy,
        actionFailed,
        message,
    ) { state, busy, failed, currentMessage ->
        state.copy(isBusy = busy, actionFailed = failed, message = currentMessage)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerProfileUiState(),
    )

    fun retry() {
        reloadTrigger.update { it + 1 }
    }

    fun sendFriendRequest() = runAction(PlayerProfileMessage.REQUEST_SENT) {
        friendRepository.sendRequest(requireNotNull(myId), playerId)
    }

    fun acceptFriendRequest() {
        val requestId = uiState.value.incomingRequestId ?: return
        runAction(PlayerProfileMessage.REQUEST_ACCEPTED) { friendRepository.respondToRequest(requestId, accept = true) }
    }

    /** Invita al jugador a uno de mis partidos próximos como anfitrión. */
    fun inviteToMatch(matchId: String) {
        if (uiState.value.invitableMatches.none { it.matchId == matchId }) return
        runAction(PlayerProfileMessage.INVITED) {
            invitationRepository.sendInvitations(matchId, requireNotNull(myId), listOf(playerId))
        }
    }

    fun onMessageShown() {
        message.value = null
    }

    fun onErrorShown() {
        actionFailed.value = false
    }

    private fun runAction(successMessage: PlayerProfileMessage, action: suspend () -> Result<Unit>) {
        if (myId == null || isBusy.value) return
        isBusy.value = true
        viewModelScope.launch {
            val result = action()
            isBusy.value = false
            if (result.isSuccess) message.value = successMessage else actionFailed.value = true
        }
    }

    /** Hilt crea el ViewModel con el [playerId] que recibe la pantalla. */
    @AssistedFactory
    interface Factory {
        fun create(playerId: String): PlayerProfileViewModel
    }
}
