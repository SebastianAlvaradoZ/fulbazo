package com.vapsa.fulbazo.domain.model

import java.time.LocalDate

/**
 * Datos necesarios para registrar un usuario nuevo.
 */
data class NewUser(
    val firstName: String,
    val lastName: String,
    val nickname: String,
    val phone: String,
    val email: String,
    val birthDate: LocalDate,
    val district: String,
    val password: String,
)
