package com.vapsa.fulbazo.ui.auth.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.ArequipaDistricts
import com.vapsa.fulbazo.domain.validation.UserValidator
import com.vapsa.fulbazo.ui.auth.message
import com.vapsa.fulbazo.ui.components.AnimatedErrorBanner
import com.vapsa.fulbazo.ui.components.DateField
import com.vapsa.fulbazo.ui.components.DropdownField
import com.vapsa.fulbazo.ui.components.ErrorBanner
import com.vapsa.fulbazo.ui.components.FormTextField
import com.vapsa.fulbazo.ui.components.PasswordTextField
import com.vapsa.fulbazo.ui.components.ProfilePhotoSection
import com.vapsa.fulbazo.ui.components.rememberPhotoPicker
import com.vapsa.fulbazo.ui.theme.FulbazoTheme
import java.time.LocalDate

/**
 * Punto de entrada con ViewModel: observa el estado y avisa cuando termina el registro.
 */
@Composable
fun RegisterRoute(
    onRegistered: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pickPhoto = rememberPhotoPicker(onPicked = viewModel::onPhotoChange)

    LaunchedEffect(uiState.isRegistered) {
        if (uiState.isRegistered) {
            viewModel.onRegisterHandled()
            onRegistered()
        }
    }

    RegisterScreen(
        uiState = uiState,
        actions = RegisterActions(
            onPickPhoto = pickPhoto,
            onRemovePhoto = { viewModel.onPhotoChange(null) },
            onFirstNameChange = viewModel::onFirstNameChange,
            onLastNameChange = viewModel::onLastNameChange,
            onNicknameChange = viewModel::onNicknameChange,
            onBirthDateChange = viewModel::onBirthDateChange,
            onDistrictChange = viewModel::onDistrictChange,
            onPhoneChange = viewModel::onPhoneChange,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
            onRegisterClick = viewModel::register,
            onBackClick = onNavigateBack,
        ),
    )
}

/**
 * Eventos que la vista de registro envía al ViewModel.
 */
data class RegisterActions(
    val onPickPhoto: () -> Unit = {},
    val onRemovePhoto: () -> Unit = {},
    val onFirstNameChange: (String) -> Unit = {},
    val onLastNameChange: (String) -> Unit = {},
    val onNicknameChange: (String) -> Unit = {},
    val onBirthDateChange: (LocalDate) -> Unit = {},
    val onDistrictChange: (String) -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onEmailChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onConfirmPasswordChange: (String) -> Unit = {},
    val onRegisterClick: () -> Unit = {},
    val onBackClick: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    uiState: RegisterUiState,
    actions: RegisterActions,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val enabled = !uiState.isLoading
    val errors = uiState.errors
    val submit = {
        focusManager.clearFocus()
        actions.onRegisterClick()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.register_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBackClick, enabled = enabled) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.auth_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.register_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            AnimatedErrorBanner(message = uiState.authError?.message())

            ProfilePhotoSection(
                photoUri = uiState.photoUri,
                enabled = enabled,
                onPickPhoto = actions.onPickPhoto,
                onRemovePhoto = actions.onRemovePhoto,
            )
            Spacer(Modifier.height(8.dp))

            FormTextField(
                value = uiState.firstName,
                onValueChange = actions.onFirstNameChange,
                label = stringResource(R.string.register_first_name),
                error = errors.firstName?.message(),
                enabled = enabled,
                capitalization = KeyboardCapitalization.Words,
            )
            FormTextField(
                value = uiState.lastName,
                onValueChange = actions.onLastNameChange,
                label = stringResource(R.string.register_last_name),
                error = errors.lastName?.message(),
                enabled = enabled,
                capitalization = KeyboardCapitalization.Words,
            )
            FormTextField(
                value = uiState.nickname,
                onValueChange = actions.onNicknameChange,
                label = stringResource(R.string.register_nickname),
                error = errors.nickname?.message(),
                supportingText = stringResource(R.string.register_nickname_hint),
                enabled = enabled,
            )
            DateField(
                value = uiState.birthDate,
                onValueChange = actions.onBirthDateChange,
                label = stringResource(R.string.register_birth_date),
                error = errors.birthDate?.message(),
                enabled = enabled,
                initialDisplayedDate = LocalDate.now().minusYears(18),
            )
            DropdownField(
                value = uiState.district,
                options = ArequipaDistricts.all,
                onValueChange = actions.onDistrictChange,
                label = stringResource(R.string.register_district),
                error = errors.district?.message(),
                enabled = enabled,
            )
            FormTextField(
                value = uiState.phone,
                onValueChange = actions.onPhoneChange,
                label = stringResource(R.string.register_phone),
                error = errors.phone?.message(),
                prefix = stringResource(R.string.register_phone_prefix),
                enabled = enabled,
                keyboardType = KeyboardType.Phone,
            )
            FormTextField(
                value = uiState.email,
                onValueChange = actions.onEmailChange,
                label = stringResource(R.string.auth_email),
                error = errors.email?.message(),
                enabled = enabled,
                keyboardType = KeyboardType.Email,
            )
            PasswordTextField(
                value = uiState.password,
                onValueChange = actions.onPasswordChange,
                label = stringResource(R.string.auth_password),
                error = errors.password?.message(),
                supportingText = stringResource(
                    R.string.register_password_hint,
                    UserValidator.PASSWORD_MIN_LENGTH,
                ),
                enabled = enabled,
            )
            PasswordTextField(
                value = uiState.confirmPassword,
                onValueChange = actions.onConfirmPasswordChange,
                label = stringResource(R.string.auth_confirm_password),
                error = errors.confirmPassword?.message(),
                enabled = enabled,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = submit,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.register_button))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.register_have_account),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = actions.onBackClick, enabled = enabled) {
                    Text(stringResource(R.string.register_go_to_login))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    FulbazoTheme {
        RegisterScreen(
            uiState = RegisterUiState(firstName = "Lucía", nickname = "lu"),
            actions = RegisterActions(),
        )
    }
}
