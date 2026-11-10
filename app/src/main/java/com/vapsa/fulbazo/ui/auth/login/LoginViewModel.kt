package com.vapsa.fulbazo.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vapsa.fulbazo.di.AppContainer
import com.vapsa.fulbazo.domain.model.AuthResult
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.validation.FieldError
import com.vapsa.fulbazo.domain.validation.UserValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, authError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, authError = null) }
    }

    fun login() {
        val state = _uiState.value
        if (state.isLoading) return

        val emailError = UserValidator.validateEmail(state.email)
        val passwordError = if (state.password.isEmpty()) FieldError.REQUIRED else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, authError = null) }
        viewModelScope.launch {
            when (val result = authRepository.login(state.email.trim(), state.password)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoading = false, isLoggedIn = true)
                }
                is AuthResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, authError = result.error)
                }
            }
        }
    }

    /** La vista llama a esto después de navegar, para no repetir la navegación. */
    fun onLoginHandled() {
        _uiState.update { it.copy(isLoggedIn = false) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { LoginViewModel(AppContainer.authRepository) }
        }
    }
}
