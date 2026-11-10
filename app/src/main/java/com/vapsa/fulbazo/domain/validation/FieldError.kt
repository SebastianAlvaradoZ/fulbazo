package com.vapsa.fulbazo.domain.validation

/**
 * Errores de validación de un campo de formulario.
 */
enum class FieldError {
    REQUIRED,
    INVALID_NAME,
    INVALID_NICKNAME,
    INVALID_PHONE,
    INVALID_EMAIL,
    PASSWORD_TOO_SHORT,
    PASSWORD_MISMATCH,
    BIRTH_DATE_IN_FUTURE,
    TOO_YOUNG,
    INVALID_DISTRICT,
}
