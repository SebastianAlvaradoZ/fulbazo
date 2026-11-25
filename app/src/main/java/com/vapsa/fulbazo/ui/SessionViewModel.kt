package com.vapsa.fulbazo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vapsa.fulbazo.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Con qué pantalla abre la app. */
enum class SessionStart {
    /** Buscando la sesión guardada: se ve el logo. */
    CHECKING,

    /** Hay sesión: abre directo en el Mapa. */
    SIGNED_IN,

    /** No hay sesión: abre en el Login. */
    SIGNED_OUT,
}

/**
 * Inicio de sesión automático. Se resuelve una sola vez por apertura: después, entrar y
 * salir de la cuenta lo maneja la navegación.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _start = MutableStateFlow(SessionStart.CHECKING)
    val start: StateFlow<SessionStart> = _start.asStateFlow()

    init {
        viewModelScope.launch {
            _start.value =
                if (authRepository.restoreSession() != null) SessionStart.SIGNED_IN else SessionStart.SIGNED_OUT
        }
    }
}
