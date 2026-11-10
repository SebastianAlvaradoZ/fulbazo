package com.vapsa.fulbazo.domain.model

import java.time.LocalDate

/**
 * Usuario registrado en Fulbazo.
 *
 * El [nickname] (apodo) es el nombre principal que verán los demás jugadores.
 */
data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val nickname: String,
    val phone: String,
    val email: String,
    val birthDate: LocalDate,
) {
    val fullName: String get() = "$firstName $lastName"
}
