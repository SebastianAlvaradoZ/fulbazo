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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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
    authRepository: AuthRepository,
    statsRepository: StatsRepository,
) : ViewModel() {

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
