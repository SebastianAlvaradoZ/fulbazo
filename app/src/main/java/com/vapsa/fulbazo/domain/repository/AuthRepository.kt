package com.vapsa.fulbazo.domain.repository

import com.vapsa.fulbazo.domain.model.AuthResult
import com.vapsa.fulbazo.domain.model.NewUser
import com.vapsa.fulbazo.domain.model.ProfileUpdate
import com.vapsa.fulbazo.domain.model.User
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de autenticación. Los ViewModels dependen de esta interfaz y no
 * de una implementación concreta (falsa ahora, Firebase Auth más adelante).
 */
interface AuthRepository {

    /** Usuario con sesión iniciada, o `null` si no hay sesión. */
    val currentUser: StateFlow<User?>

    suspend fun login(email: String, password: String): AuthResult

    suspend fun register(newUser: NewUser): AuthResult

    /** Actualiza los datos del usuario con sesión. Falla si el apodo ya lo usa otro jugador. */
    suspend fun updateProfile(update: ProfileUpdate): AuthResult

    /** Cierra la sesión: [currentUser] pasa a `null`. */
    suspend fun logout()
}
