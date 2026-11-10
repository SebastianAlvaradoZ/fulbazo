package com.vapsa.fulbazo.ui.invitations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vapsa.fulbazo.di.AppContainer
import com.vapsa.fulbazo.domain.model.InvitationStatus
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.InvitationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Cantidad de invitaciones pendientes, para el contador de la campanita.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InvitationBadgeViewModel(
    authRepository: AuthRepository,
    invitationRepository: InvitationRepository,
) : ViewModel() {

    val pendingCount: StateFlow<Int> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(0)
            } else {
                invitationRepository.observeReceived(user.id)
                    .map { list -> list.count { it.status == InvitationStatus.PENDING } }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                InvitationBadgeViewModel(AppContainer.authRepository, AppContainer.invitationRepository)
            }
        }
    }
}
