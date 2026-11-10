package com.vapsa.fulbazo.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserValidatorTest {

    @Test
    fun name_acceptsAccentsAndSpaces() {
        assertNull(UserValidator.validateName("José María"))
        assertNull(UserValidator.validateName("Núñez"))
    }

    @Test
    fun name_rejectsBlankAndDigits() {
        assertEquals(FieldError.REQUIRED, UserValidator.validateName("  "))
        assertEquals(FieldError.INVALID_NAME, UserValidator.validateName("Juan3"))
        assertEquals(FieldError.INVALID_NAME, UserValidator.validateName("J"))
    }

    @Test
    fun nickname_checksLengthAndCharacters() {
        assertNull(UserValidator.validateNickname("el_pibe.10"))
        assertEquals(FieldError.INVALID_NICKNAME, UserValidator.validateNickname("ab"))
        assertEquals(FieldError.INVALID_NICKNAME, UserValidator.validateNickname("a".repeat(21)))
        assertEquals(FieldError.INVALID_NICKNAME, UserValidator.validateNickname("con espacio"))
    }

    @Test
    fun phone_requiresPeruvianMobile() {
        assertNull(UserValidator.validatePhone("987654321"))
        assertNull(UserValidator.validatePhone("987 654 321"))
        assertEquals(FieldError.INVALID_PHONE, UserValidator.validatePhone("887654321"))
        assertEquals(FieldError.INVALID_PHONE, UserValidator.validatePhone("98765432"))
        assertEquals(FieldError.REQUIRED, UserValidator.validatePhone(""))
    }

    @Test
    fun email_validatesFormat() {
        assertNull(UserValidator.validateEmail("jugador@fulbazo.com"))
        assertEquals(FieldError.INVALID_EMAIL, UserValidator.validateEmail("jugador@fulbazo"))
        assertEquals(FieldError.INVALID_EMAIL, UserValidator.validateEmail("jugador.com"))
    }

    @Test
    fun password_requiresMinimumLengthAndMatch() {
        assertNull(UserValidator.validatePassword("123456"))
        assertEquals(FieldError.PASSWORD_TOO_SHORT, UserValidator.validatePassword("12345"))
        assertNull(UserValidator.validatePasswordConfirmation("abcdef", "abcdef"))
        assertEquals(
            FieldError.PASSWORD_MISMATCH,
            UserValidator.validatePasswordConfirmation("abcdef", "abcdeg"),
        )
    }
}
