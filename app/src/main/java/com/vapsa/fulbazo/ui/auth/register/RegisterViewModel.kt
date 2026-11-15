package com.vapsa.fulbazo.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vapsa.fulbazo.domain.model.AuthResult
import com.vapsa.fulbazo.domain.model.NewUser
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.validation.UserValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    /** Constructor que usa Hilt; las pruebas usan el principal para fijar la hora y los tiempos. */
    @Inject constructor(
        authRepository: AuthRepository,
    ) : this(
        authRepository = authRepository,
        today = { LocalDate.now() },
    )

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    /** Foto opcional: `null` quita la foto elegida. */
    fun onPhotoChange(uri: String?) = updateField { copy(photoUri = uri) }

    fun onFirstNameChange(value: String) = updateField {
        copy(firstName = value, errors = errors.copy(firstName = null))
    }

    fun onLastNameChange(value: String) = updateField {
        copy(lastName = value, errors = errors.copy(lastName = null))
    }

    fun onNicknameChange(value: String) = updateField {
        copy(nickname = value, errors = errors.copy(nickname = null))
    }

    fun onBirthDateChange(value: LocalDate) = updateField {
        copy(birthDate = value, errors = errors.copy(birthDate = null))
    }

    fun onDistrictChange(value: String) = updateField {
        copy(district = value, errors = errors.copy(district = null))
    }

    fun onPhoneChange(value: String) = updateField {
        // Solo dígitos y como máximo 9, para que no se pueda escribir de más.
        copy(phone = value.filter(Char::isDigit).take(9), errors = errors.copy(phone = null))
    }

    fun onEmailChange(value: String) = updateField {
        copy(email = value, errors = errors.copy(email = null))
    }

    fun onPasswordChange(value: String) = updateField {
        copy(password = value, errors = errors.copy(password = null))
    }

    fun onConfirmPasswordChange(value: String) = updateField {
        copy(confirmPassword = value, errors = errors.copy(confirmPassword = null))
    }

    fun register() {
        val state = _uiState.value
        if (state.isLoading) return

        val errors = validate(state)
        if (errors.hasErrors) {
            _uiState.update { it.copy(errors = errors) }
            return
        }

        _uiState.update { it.copy(isLoading = true, authError = null) }
        viewModelScope.launch {
            val result = authRepository.register(
                NewUser(
                    firstName = state.firstName.trim(),
                    lastName = state.lastName.trim(),
                    nickname = state.nickname.trim(),
                    phone = UserValidator.normalizePhone(state.phone),
                    email = state.email.trim(),
                    birthDate = requireNotNull(state.birthDate),
                    district = requireNotNull(state.district),
                    password = state.password,
                    photoUri = state.photoUri,
                ),
            )
            _uiState.update {
                when (result) {
                    is AuthResult.Success -> it.copy(isLoading = false, isRegistered = true)
                    is AuthResult.Failure -> it.copy(isLoading = false, authError = result.error)
                }
            }
        }
    }

    /** La vista llama a esto después de navegar, para no repetir la navegación. */
    fun onRegisterHandled() {
        _uiState.update { it.copy(isRegistered = false) }
    }

    private fun validate(state: RegisterUiState) = RegisterErrors(
        firstName = UserValidator.validateName(state.firstName),
        lastName = UserValidator.validateName(state.lastName),
        nickname = UserValidator.validateNickname(state.nickname),
        birthDate = UserValidator.validateBirthDate(state.birthDate, today()),
        district = UserValidator.validateDistrict(state.district),
        phone = UserValidator.validatePhone(state.phone),
        email = UserValidator.validateEmail(state.email),
        password = UserValidator.validatePassword(state.password),
        confirmPassword = UserValidator.validatePasswordConfirmation(
            state.password,
            state.confirmPassword,
        ),
    )

    private fun updateField(transform: RegisterUiState.() -> RegisterUiState) {
        _uiState.update { it.transform().copy(authError = null) }
    }
}
