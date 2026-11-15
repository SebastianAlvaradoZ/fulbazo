package com.vapsa.fulbazo.ui.profile.edit

import com.vapsa.fulbazo.MainDispatcherRule
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.domain.model.AuthError
import com.vapsa.fulbazo.domain.model.NewUser
import com.vapsa.fulbazo.domain.validation.FieldError
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class EditProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository(latencyMillis = 0)

    private suspend fun loggedInViewModel(): EditProfileViewModel {
        authRepository.login("demo@fulbazo.com", "demo123")
        return EditProfileViewModel(authRepository)
    }

    @Test
    fun withoutSession_notLoggedIn() {
        assertTrue(EditProfileViewModel(authRepository).uiState.value.notLoggedIn)
    }

    @Test
    fun loadsCurrentUserData_withReadOnlyEmailAndBirthDate() = runTest {
        val state = loggedInViewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals("demo", state.nickname)
        assertEquals("Yanahuara", state.district)
        assertEquals("demo@fulbazo.com", state.email)
        assertEquals(LocalDate.of(2000, 1, 1), state.birthDate)
    }

    @Test
    fun invalidFields_useRegisterValidations() = runTest {
        val viewModel = loggedInViewModel()
        viewModel.onNicknameChange("ab")
        viewModel.onPhoneChange("123")
        viewModel.onFirstNameChange("")

        viewModel.save()

        val errors = viewModel.uiState.value.errors
        assertEquals(FieldError.INVALID_NICKNAME, errors.nickname)
        assertEquals(FieldError.INVALID_PHONE, errors.phone)
        assertEquals(FieldError.REQUIRED, errors.firstName)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun validChanges_areSaved_andCurrentUserUpdates() = runTest {
        val viewModel = loggedInViewModel()
        viewModel.onNicknameChange("el.demo")
        viewModel.onDistrictChange("Cayma")
        viewModel.onPhotoChange("content://foto")

        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        val user = authRepository.currentUser.value!!
        assertEquals("el.demo", user.nickname)
        assertEquals("Cayma", user.district)
        assertEquals("content://foto", user.photoUri)
    }

    @Test
    fun nicknameOfAnotherPlayer_showsError() = runTest {
        authRepository.register(
            NewUser("Ana", "Mamani", "ana9", "912345678", "ana@fulbazo.com", LocalDate.of(2001, 1, 1), "Cayma", "secreto1"),
        )
        authRepository.logout()
        val viewModel = loggedInViewModel()
        viewModel.onNicknameChange("ana9")

        viewModel.save()
        advanceUntilIdle()

        assertEquals(AuthError.NICKNAME_ALREADY_IN_USE, viewModel.uiState.value.authError)
        assertFalse(viewModel.uiState.value.isSaved)
    }
}
