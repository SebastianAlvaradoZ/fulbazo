package com.vapsa.fulbazo.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.AuthError
import com.vapsa.fulbazo.domain.validation.FieldError
import com.vapsa.fulbazo.domain.validation.UserValidator

/**
 * Traduce los errores del dominio a textos de `strings.xml`.
 */
@Composable
fun FieldError.message(): String = when (this) {
    FieldError.REQUIRED -> stringResource(R.string.error_required)
    FieldError.INVALID_NAME -> stringResource(R.string.error_invalid_name)
    FieldError.INVALID_NICKNAME -> stringResource(
        R.string.error_invalid_nickname,
        UserValidator.NICKNAME_MIN_LENGTH,
        UserValidator.NICKNAME_MAX_LENGTH,
    )
    FieldError.INVALID_PHONE -> stringResource(R.string.error_invalid_phone)
    FieldError.INVALID_EMAIL -> stringResource(R.string.error_invalid_email)
    FieldError.PASSWORD_TOO_SHORT -> stringResource(
        R.string.error_password_too_short,
        UserValidator.PASSWORD_MIN_LENGTH,
    )
    FieldError.PASSWORD_MISMATCH -> stringResource(R.string.error_password_mismatch)
    FieldError.BIRTH_DATE_IN_FUTURE -> stringResource(R.string.error_birth_date_in_future)
    FieldError.TOO_YOUNG -> stringResource(R.string.error_too_young, UserValidator.MIN_AGE_YEARS)
}

@Composable
fun AuthError.message(): String = when (this) {
    AuthError.INVALID_CREDENTIALS -> stringResource(R.string.error_invalid_credentials)
    AuthError.EMAIL_ALREADY_IN_USE -> stringResource(R.string.error_email_in_use)
    AuthError.NICKNAME_ALREADY_IN_USE -> stringResource(R.string.error_nickname_in_use)
    AuthError.NETWORK -> stringResource(R.string.error_network)
    AuthError.UNKNOWN -> stringResource(R.string.error_unknown)
}
