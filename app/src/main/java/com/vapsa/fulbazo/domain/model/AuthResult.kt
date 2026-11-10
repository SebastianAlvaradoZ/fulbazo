package com.vapsa.fulbazo.domain.model

/**
 * Resultado de una operación de autenticación.
 */
sealed interface AuthResult {
    data class Success(val user: User) : AuthResult
    data class Failure(val error: AuthError) : AuthResult
}

/**
 * Errores posibles al iniciar sesión o registrarse.
 */
enum class AuthError {
    INVALID_CREDENTIALS,
    EMAIL_ALREADY_IN_USE,
    NICKNAME_ALREADY_IN_USE,
    NETWORK,
    UNKNOWN,
}
