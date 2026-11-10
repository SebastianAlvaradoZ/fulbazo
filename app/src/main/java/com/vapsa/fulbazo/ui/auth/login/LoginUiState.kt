package com.vapsa.fulbazo.ui.auth.login

import com.vapsa.fulbazo.domain.model.AuthError
import com.vapsa.fulbazo.domain.validation.FieldError

/**
 * Estado completo de la pantalla de inicio de sesión.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: FieldError? = null,
    val passwordError: FieldError? = null,
    val isLoading: Boolean = false,
    val authError: AuthError? = null,
    val isLoggedIn: Boolean = false,
)
