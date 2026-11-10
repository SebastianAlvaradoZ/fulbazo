package com.vapsa.fulbazo.ui.auth.register

import com.vapsa.fulbazo.domain.model.AuthError
import com.vapsa.fulbazo.domain.validation.FieldError
import java.time.LocalDate

/**
 * Estado completo de la pantalla de registro.
 */
data class RegisterUiState(
    val photoUri: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val nickname: String = "",
    val birthDate: LocalDate? = null,
    val district: String? = null,
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val errors: RegisterErrors = RegisterErrors(),
    val isLoading: Boolean = false,
    val authError: AuthError? = null,
    val isRegistered: Boolean = false,
)

/**
 * Error de validación de cada campo; `null` significa que el campo es válido.
 */
data class RegisterErrors(
    val firstName: FieldError? = null,
    val lastName: FieldError? = null,
    val nickname: FieldError? = null,
    val birthDate: FieldError? = null,
    val district: FieldError? = null,
    val phone: FieldError? = null,
    val email: FieldError? = null,
    val password: FieldError? = null,
    val confirmPassword: FieldError? = null,
) {
    val hasErrors: Boolean
        get() = listOf(
            firstName, lastName, nickname, birthDate, district, phone, email, password,
            confirmPassword,
        ).any { it != null }
}
