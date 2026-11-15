package com.vapsa.fulbazo.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vapsa.fulbazo.di.AppContainer
import com.vapsa.fulbazo.domain.model.PlayerProfile
import com.vapsa.fulbazo.domain.model.User
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.StatsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla de perfil.
 */
data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val stats: PlayerProfile? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val authRepository: AuthRepository,
    statsRepository: StatsRepository,
) : ViewModel() {

    private val _loggedOut = MutableStateFlow(false)

    /** `true` después de cerrar sesión: la vista vuelve al Login. */
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _loggedOut.value = true
        }
    }

    /** Se actualiza solo cuando cambian el usuario o sus estadísticas. */
    val uiState: StateFlow<ProfileUiState> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(ProfileUiState(isLoading = false))
            } else {
                statsRepository.observeProfile(user.id).map { stats ->
                    ProfileUiState(isLoading = false, user = user, stats = stats)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileUiState(),
        )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProfileViewModel(AppContainer.authRepository, AppContainer.statsRepository)
            }
        }
    }
}
