package com.vapsa.fulbazo.domain.validation

import java.time.LocalDate
import java.time.Period

/**
 * Reglas de validación de los datos del usuario. Son funciones puras, sin
 * dependencias de Android, para poder probarlas con tests unitarios.
 * Cada función devuelve `null` si el valor es válido.
 */
object UserValidator {

    const val NICKNAME_MIN_LENGTH = 3
    const val NICKNAME_MAX_LENGTH = 20
    const val PASSWORD_MIN_LENGTH = 6
    const val MIN_AGE_YEARS = 13

    private val nameRegex = Regex("""^\p{L}[\p{L} '\-]{1,39}$""")
    private val nicknameRegex =
        Regex("^[A-Za-z0-9_.]{$NICKNAME_MIN_LENGTH,$NICKNAME_MAX_LENGTH}$")
    private val phoneRegex = Regex("""^9\d{8}$""")
    private val emailRegex = Regex("""^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$""")

    fun validateName(value: String): FieldError? = when {
        value.isBlank() -> FieldError.REQUIRED
        !nameRegex.matches(value.trim()) -> FieldError.INVALID_NAME
        else -> null
    }

    fun validateNickname(value: String): FieldError? = when {
        value.isBlank() -> FieldError.REQUIRED
        !nicknameRegex.matches(value.trim()) -> FieldError.INVALID_NICKNAME
        else -> null
    }

    /** Celular peruano: 9 dígitos que empiezan en 9. Se ignoran los espacios. */
    fun validatePhone(value: String): FieldError? = when {
        value.isBlank() -> FieldError.REQUIRED
        !phoneRegex.matches(normalizePhone(value)) -> FieldError.INVALID_PHONE
        else -> null
    }

    fun validateEmail(value: String): FieldError? = when {
        value.isBlank() -> FieldError.REQUIRED
        !emailRegex.matches(value.trim()) -> FieldError.INVALID_EMAIL
        else -> null
    }

    fun validatePassword(value: String): FieldError? = when {
        value.isEmpty() -> FieldError.REQUIRED
        value.length < PASSWORD_MIN_LENGTH -> FieldError.PASSWORD_TOO_SHORT
        else -> null
    }

    fun validatePasswordConfirmation(password: String, confirmation: String): FieldError? = when {
        confirmation.isEmpty() -> FieldError.REQUIRED
        password != confirmation -> FieldError.PASSWORD_MISMATCH
        else -> null
    }

    /** Exige una fecha pasada y al menos [MIN_AGE_YEARS] años cumplidos a [today]. */
    fun validateBirthDate(value: LocalDate?, today: LocalDate): FieldError? = when {
        value == null -> FieldError.REQUIRED
        value.isAfter(today) -> FieldError.BIRTH_DATE_IN_FUTURE
        Period.between(value, today).years < MIN_AGE_YEARS -> FieldError.TOO_YOUNG
        else -> null
    }

    fun normalizePhone(value: String): String = value.filterNot { it.isWhitespace() }
}
