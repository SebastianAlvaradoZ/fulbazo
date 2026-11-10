package com.vapsa.fulbazo.ui.friends.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vapsa.fulbazo.di.AppContainer
import com.vapsa.fulbazo.domain.model.FriendGroup
import com.vapsa.fulbazo.domain.model.PlayerSummary
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.FriendRepository
import com.vapsa.fulbazo.domain.repository.GroupRepository
import com.vapsa.fulbazo.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GroupNameError { REQUIRED, TOO_LONG }

data class GroupEditorUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val name: String = "",
    val friends: List<PlayerSummary> = emptyList(),
    val memberIds: Set<String> = emptySet(),
    val nameError: GroupNameError? = null,
    val membersMissing: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    /** `true` cuando se guardó o eliminó: la vista vuelve atrás. */
    val isDone: Boolean = false,
)

/**
 * Crear o editar un grupo de amigos. Con [groupId] nulo se crea uno nuevo.
 */
class GroupEditorViewModel(
    private val groupId: String?,
    authRepository: AuthRepository,
    playerRepository: PlayerRepository,
    friendRepository: FriendRepository,
    private val groupRepository: GroupRepository,
) : ViewModel() {

    private val ownerId: String? = authRepository.currentUser.value?.id

    private val _uiState = MutableStateFlow(GroupEditorUiState(isNew = groupId == null))
    val uiState: StateFlow<GroupEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (ownerId == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            val group = groupId?.let { id ->
                groupRepository.observeGroups(ownerId).first().firstOrNull { it.id == id }
            }
            combine(
                playerRepository.observePlayers(),
                friendRepository.observeFriendIds(ownerId),
            ) { players, friendIds ->
                friendIds.mapNotNull { players[it] }.sortedBy { it.nickname.lowercase() }
            }.collect { friends ->
                _uiState.update { state ->
                    val firstLoad = state.isLoading
                    state.copy(
                        isLoading = false,
                        friends = friends,
                        name = if (firstLoad && group != null) group.name else state.name,
                        memberIds = if (firstLoad && group != null) group.memberIds.toSet() else state.memberIds,
                    )
                }
            }
        }
    }

    fun onNameChange(text: String) {
        _uiState.update { it.copy(name = text, nameError = null) }
    }

    fun onToggleMember(friendId: String) {
        _uiState.update {
            val members = if (friendId in it.memberIds) it.memberIds - friendId else it.memberIds + friendId
            it.copy(memberIds = members, membersMissing = false)
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || ownerId == null) return
        val name = state.name.trim()
        val nameError = when {
            name.isEmpty() -> GroupNameError.REQUIRED
            name.length > FriendGroup.MAX_NAME_LENGTH -> GroupNameError.TOO_LONG
            else -> null
        }
        val membersMissing = state.memberIds.isEmpty()
        if (nameError != null || membersMissing) {
            _uiState.update { it.copy(nameError = nameError, membersMissing = membersMissing) }
            return
        }

        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            val members = state.memberIds.toList()
            val result = if (groupId == null) {
                groupRepository.createGroup(ownerId, name, members).map { }
            } else {
                groupRepository.updateGroup(groupId, name, members)
            }
            _uiState.update { it.copy(isSaving = false, isDone = result.isSuccess, saveFailed = result.isFailure) }
        }
    }

    fun delete() {
        val id = groupId ?: return
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            val result = groupRepository.deleteGroup(id)
            _uiState.update { it.copy(isSaving = false, isDone = result.isSuccess, saveFailed = result.isFailure) }
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(saveFailed = false) }
    }

    companion object {
        fun factory(groupId: String?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                GroupEditorViewModel(
                    groupId,
                    AppContainer.authRepository,
                    AppContainer.playerRepository,
                    AppContainer.friendRepository,
                    AppContainer.groupRepository,
                )
            }
        }
    }
}
