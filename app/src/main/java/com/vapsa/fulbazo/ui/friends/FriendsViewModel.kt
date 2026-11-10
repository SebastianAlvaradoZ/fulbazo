package com.vapsa.fulbazo.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vapsa.fulbazo.di.AppContainer
import com.vapsa.fulbazo.domain.model.FriendRequest
import com.vapsa.fulbazo.domain.model.PlayerRelation
import com.vapsa.fulbazo.domain.model.PlayerSearchResult
import com.vapsa.fulbazo.domain.model.PlayerSummary
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.FriendRepository
import com.vapsa.fulbazo.domain.repository.GroupRepository
import com.vapsa.fulbazo.domain.repository.PlayerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FriendRequestItem(val requestId: String, val player: PlayerSummary)

data class GroupItem(val id: String, val name: String, val memberNicknames: List<String>)

/**
 * Estado de la pantalla Amigos: amigos, solicitudes recibidas, grupos y búsqueda.
 */
data class FriendsUiState(
    val isLoading: Boolean = true,
    val friends: List<PlayerSummary> = emptyList(),
    val incomingRequests: List<FriendRequestItem> = emptyList(),
    val groups: List<GroupItem> = emptyList(),
    val query: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<PlayerSearchResult> = emptyList(),
    /** Jugadores o solicitudes con una acción en curso, para desactivar sus botones. */
    val busyIds: Set<String> = emptySet(),
    val actionFailed: Boolean = false,
) {
    val isSearchActive: Boolean get() = query.isNotBlank()
}

class FriendsViewModel(
    authRepository: AuthRepository,
    private val playerRepository: PlayerRepository,
    private val friendRepository: FriendRepository,
    groupRepository: GroupRepository,
    private val searchDebounceMillis: Long = 300,
) : ViewModel() {

    private val userId: String? = authRepository.currentUser.value?.id

    private val query = MutableStateFlow("")
    private val searchHits = MutableStateFlow<List<PlayerSummary>>(emptyList())
    private val isSearching = MutableStateFlow(false)
    private val busyIds = MutableStateFlow<Set<String>>(emptySet())
    private val actionFailed = MutableStateFlow(false)
    private var searchJob: Job? = null

    private val socialData = combine(
        playerRepository.observePlayers(),
        userId?.let { friendRepository.observeFriendIds(it) } ?: flowOf(emptySet()),
        userId?.let { friendRepository.observePendingRequests(it) } ?: flowOf(emptyList()),
        userId?.let { groupRepository.observeGroups(it) } ?: flowOf(emptyList()),
    ) { players, friendIds, requests, groups ->
        SocialData(players, friendIds, requests, groups.map { group ->
            GroupItem(group.id, group.name, group.memberIds.mapNotNull { players[it]?.nickname })
        })
    }

    private val searchState = combine(query, searchHits, isSearching) { q, hits, searching ->
        Triple(q, hits, searching)
    }

    val uiState: StateFlow<FriendsUiState> = combine(
        socialData,
        searchState,
        busyIds,
        actionFailed,
    ) { data, (q, hits, searching), busy, failed ->
        FriendsUiState(
            isLoading = false,
            friends = data.friendIds.mapNotNull { data.players[it] }.sortedBy { it.nickname.lowercase() },
            incomingRequests = data.requests
                .filter { it.toUserId == userId }
                .mapNotNull { request -> data.players[request.fromUserId]?.let { FriendRequestItem(request.id, it) } },
            groups = data.groups,
            query = q,
            isSearching = searching,
            searchResults = hits.map { PlayerSearchResult(it, data.relationWith(it.id)) },
            busyIds = busy,
            actionFailed = failed,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FriendsUiState(),
    )

    fun onQueryChange(text: String) {
        query.value = text
        searchJob?.cancel()
        if (text.isBlank() || userId == null) {
            searchHits.value = emptyList()
            isSearching.value = false
            return
        }
        isSearching.value = true
        searchJob = viewModelScope.launch {
            // Espera a que el usuario deje de escribir antes de buscar.
            delay(searchDebounceMillis)
            searchHits.value = playerRepository.searchByNickname(text, excludeUserId = userId)
            isSearching.value = false
        }
    }

    fun sendRequest(playerId: String) = runAction(playerId) {
        friendRepository.sendRequest(requireNotNull(userId), playerId)
    }

    fun respondToRequest(requestId: String, accept: Boolean) = runAction(requestId) {
        friendRepository.respondToRequest(requestId, accept)
    }

    fun removeFriend(friendId: String) = runAction(friendId) {
        friendRepository.removeFriend(requireNotNull(userId), friendId)
    }

    fun onErrorShown() {
        actionFailed.value = false
    }

    private fun runAction(id: String, action: suspend () -> Result<Unit>) {
        if (userId == null || id in busyIds.value) return
        busyIds.update { it + id }
        viewModelScope.launch {
            val result = action()
            busyIds.update { it - id }
            if (result.isFailure) actionFailed.value = true
        }
    }

    private data class SocialData(
        val players: Map<String, PlayerSummary>,
        val friendIds: Set<String>,
        val requests: List<FriendRequest>,
        val groups: List<GroupItem>,
    ) {
        fun relationWith(playerId: String): PlayerRelation = when {
            playerId in friendIds -> PlayerRelation.FRIEND
            requests.any { it.toUserId == playerId } -> PlayerRelation.REQUEST_SENT
            requests.any { it.fromUserId == playerId } -> PlayerRelation.REQUEST_RECEIVED
            else -> PlayerRelation.NONE
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                FriendsViewModel(
                    AppContainer.authRepository,
                    AppContainer.playerRepository,
                    AppContainer.friendRepository,
                    AppContainer.groupRepository,
                )
            }
        }
    }
}
