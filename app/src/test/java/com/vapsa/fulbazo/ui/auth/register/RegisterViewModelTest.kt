package com.vapsa.fulbazo.ui.auth.register

import com.vapsa.fulbazo.MainDispatcherRule
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.domain.model.AuthError
import com.vapsa.fulbazo.domain.validation.FieldError
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository(latencyMillis = 0)
    private val today = LocalDate.of(2026, 11, 10)
    private val viewModel = RegisterViewModel(repository, today = { today })

    private fun fillValidForm(email: String = "nuevo@fulbazo.com", nickname: String = "nuevo10") {
        viewModel.onFirstNameChange("Lucía")
        viewModel.onLastNameChange("Quispe")
        viewModel.onNicknameChange(nickname)
        viewModel.onBirthDateChange(LocalDate.of(2004, 5, 20))
        viewModel.onDistrictChange("Cerro Colorado")
        viewModel.onPhoneChange("912345678")
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange("secreto1")
        viewModel.onConfirmPasswordChange("secreto1")
    }

    @Test
    fun emptyForm_marksRequiredFields() {
        viewModel.register()

        val errors = viewModel.uiState.value.errors
        assertEquals(FieldError.REQUIRED, errors.firstName)
        assertEquals(FieldError.REQUIRED, errors.nickname)
        assertEquals(FieldError.REQUIRED, errors.birthDate)
        assertEquals(FieldError.REQUIRED, errors.district)
        assertEquals(FieldError.REQUIRED, errors.phone)
        assertEquals(FieldError.REQUIRED, errors.email)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun under13_showsTooYoung() {
        fillValidForm()
        viewModel.onBirthDateChange(today.minusYears(12))
        viewModel.register()

        assertEquals(FieldError.TOO_YOUNG, viewModel.uiState.value.errors.birthDate)
    }

    @Test
    fun phoneInput_keepsOnlyNineDigits() {
        viewModel.onPhoneChange("98a7 654 32198")
        assertEquals("987654321", viewModel.uiState.value.phone)
    }

    @Test
    fun differentPasswords_showMismatch() {
        fillValidForm()
        viewModel.onConfirmPasswordChange("otra")
        viewModel.register()

        assertEquals(FieldError.PASSWORD_MISMATCH, viewModel.uiState.value.errors.confirmPassword)
    }

    @Test
    fun validForm_registersAndLogsIn() = runTest {
        fillValidForm()
        viewModel.register()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isRegistered)
        assertEquals("nuevo10", repository.currentUser.value?.nickname)
    }

    @Test
    fun existingEmail_showsError() = runTest {
        fillValidForm(email = "demo@fulbazo.com")
        viewModel.register()
        advanceUntilIdle()

        assertEquals(AuthError.EMAIL_ALREADY_IN_USE, viewModel.uiState.value.authError)
        assertFalse(viewModel.uiState.value.isRegistered)
    }
}
