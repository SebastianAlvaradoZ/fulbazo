package com.vapsa.fulbazo.domain.model

/**
 * Datos necesarios para registrar un usuario nuevo.
 */
data class NewUser(
    val firstName: String,
    val lastName: String,
    val nickname: String,
    val phone: String,
    val email: String,
    val password: String,
)
