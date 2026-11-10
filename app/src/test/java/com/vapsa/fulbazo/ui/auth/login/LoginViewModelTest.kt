package com.vapsa.fulbazo.ui.auth.login

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

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val viewModel = LoginViewModel(FakeAuthRepository(latencyMillis = 0))

    @Test
    fun emptyFields_showValidationErrors() {
        viewModel.login()

        val state = viewModel.uiState.value
        assertEquals(FieldError.REQUIRED, state.emailError)
        assertEquals(FieldError.REQUIRED, state.passwordError)
        assertFalse(state.isLoading)
    }

    @Test
    fun wrongPassword_showsInvalidCredentials() = runTest {
        viewModel.onEmailChange("demo@fulbazo.com")
        viewModel.onPasswordChange("incorrecta")
        viewModel.login()
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(AuthError.INVALID_CREDENTIALS, state.authError)
        assertFalse(state.isLoggedIn)
    }

    @Test
    fun validCredentials_logsIn() = runTest {
        viewModel.onEmailChange("demo@fulbazo.com")
        viewModel.onPasswordChange("demo123")
        viewModel.login()

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoggedIn)
    }
}
