package com.vapsa.fulbazo.ui.profile.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vapsa.fulbazo.di.AppContainer
import com.vapsa.fulbazo.domain.model.AuthError
import com.vapsa.fulbazo.domain.model.AuthResult
import com.vapsa.fulbazo.domain.model.ProfileUpdate
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.validation.FieldError
import com.vapsa.fulbazo.domain.validation.UserValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EditProfileErrors(
    val firstName: FieldError? = null,
    val lastName: FieldError? = null,
    val nickname: FieldError? = null,
    val phone: FieldError? = null,
    val district: FieldError? = null,
) {
    val hasErrors: Boolean
        get() = listOf(firstName, lastName, nickname, phone, district).any { it != null }
}

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val notLoggedIn: Boolean = false,
    val photoUri: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val nickname: String = "",
    val phone: String = "",
    val district: String? = null,
    /** Solo lectura. */
    val email: String = "",
    /** Solo lectura. */
    val birthDate: LocalDate? = null,
    val errors: EditProfileErrors = EditProfileErrors(),
    val isSaving: Boolean = false,
    val authError: AuthError? = null,
    /** `true` después de guardar: la vista vuelve al perfil. */
    val isSaved: Boolean = false,
)

class EditProfileViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        val user = authRepository.currentUser.value
        _uiState.value = if (user == null) {
            EditProfileUiState(isLoading = false, notLoggedIn = true)
        } else {
            EditProfileUiState(
                isLoading = false,
                photoUri = user.photoUri,
                firstName = user.firstName,
                lastName = user.lastName,
                nickname = user.nickname,
                phone = user.phone,
                district = user.district,
                email = user.email,
                birthDate = user.birthDate,
            )
        }
    }

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

    fun onPhoneChange(value: String) = updateField {
        copy(phone = value.filter(Char::isDigit).take(9), errors = errors.copy(phone = null))
    }

    fun onDistrictChange(value: String) = updateField {
        copy(district = value, errors = errors.copy(district = null))
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.notLoggedIn) return

        // Las mismas reglas que en el registro.
        val errors = EditProfileErrors(
            firstName = UserValidator.validateName(state.firstName),
            lastName = UserValidator.validateName(state.lastName),
            nickname = UserValidator.validateNickname(state.nickname),
            phone = UserValidator.validatePhone(state.phone),
            district = UserValidator.validateDistrict(state.district),
        )
        if (errors.hasErrors) {
            _uiState.update { it.copy(errors = errors) }
            return
        }

        _uiState.update { it.copy(isSaving = true, authError = null) }
        viewModelScope.launch {
            val result = authRepository.updateProfile(
                ProfileUpdate(
                    firstName = state.firstName.trim(),
                    lastName = state.lastName.trim(),
                    nickname = state.nickname.trim(),
                    phone = UserValidator.normalizePhone(state.phone),
                    district = requireNotNull(state.district),
                    photoUri = state.photoUri,
                ),
            )
            _uiState.update {
                when (result) {
                    is AuthResult.Success -> it.copy(isSaving = false, isSaved = true)
                    is AuthResult.Failure -> it.copy(isSaving = false, authError = result.error)
                }
            }
        }
    }

    fun onSavedHandled() {
        _uiState.update { it.copy(isSaved = false) }
    }

    private fun updateField(transform: EditProfileUiState.() -> EditProfileUiState) {
        _uiState.update { it.transform().copy(authError = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { EditProfileViewModel(AppContainer.authRepository) }
        }
    }
}
