package com.vapsa.fulbazo.ui.profile.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.ArequipaDistricts
import com.vapsa.fulbazo.ui.auth.message
import com.vapsa.fulbazo.ui.components.DropdownField
import com.vapsa.fulbazo.ui.components.EmptyState
import com.vapsa.fulbazo.ui.components.ErrorBanner
import com.vapsa.fulbazo.ui.components.FormTextField
import com.vapsa.fulbazo.ui.components.LoadingState
import com.vapsa.fulbazo.ui.components.ProfilePhotoSection
import com.vapsa.fulbazo.ui.components.rememberPhotoPicker
import java.time.format.DateTimeFormatter

@Composable
fun EditProfileRoute(
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditProfileViewModel = viewModel(factory = EditProfileViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pickPhoto = rememberPhotoPicker(onPicked = viewModel::onPhotoChange)

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.onSavedHandled()
            onSaved()
        }
    }

    EditProfileScreen(
        uiState = uiState,
        actions = EditProfileActions(
            onBack = onNavigateBack,
            onPickPhoto = pickPhoto,
            onRemovePhoto = { viewModel.onPhotoChange(null) },
            onFirstNameChange = viewModel::onFirstNameChange,
            onLastNameChange = viewModel::onLastNameChange,
            onNicknameChange = viewModel::onNicknameChange,
            onPhoneChange = viewModel::onPhoneChange,
            onDistrictChange = viewModel::onDistrictChange,
            onSave = viewModel::save,
        ),
    )
}

data class EditProfileActions(
    val onBack: () -> Unit = {},
    val onPickPhoto: () -> Unit = {},
    val onRemovePhoto: () -> Unit = {},
    val onFirstNameChange: (String) -> Unit = {},
    val onLastNameChange: (String) -> Unit = {},
    val onNicknameChange: (String) -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onDistrictChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    uiState: EditProfileUiState,
    actions: EditProfileActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_profile_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when {
            uiState.isLoading -> LoadingState(contentModifier)
            uiState.notLoggedIn -> EmptyState(stringResource(R.string.error_unknown), contentModifier)
            else -> EditProfileForm(uiState, actions, contentModifier)
        }
    }
}

@Composable
private fun EditProfileForm(
    uiState: EditProfileUiState,
    actions: EditProfileActions,
    modifier: Modifier = Modifier,
) {
    val enabled = !uiState.isSaving
    val errors = uiState.errors
    Column(
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        uiState.authError?.let {
            ErrorBanner(message = it.message())
            Spacer(Modifier.height(12.dp))
        }
        ProfilePhotoSection(
            photoUri = uiState.photoUri,
            enabled = enabled,
            onPickPhoto = actions.onPickPhoto,
            onRemovePhoto = actions.onRemovePhoto,
        )
        FormTextField(
            value = uiState.nickname,
            onValueChange = actions.onNicknameChange,
            label = stringResource(R.string.register_nickname),
            error = errors.nickname?.message(),
            supportingText = stringResource(R.string.register_nickname_hint),
            enabled = enabled,
        )
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

        // Datos que no se pueden cambiar.
        FormTextField(
            value = uiState.email,
            onValueChange = {},
            label = stringResource(R.string.auth_email),
            enabled = false,
        )
        FormTextField(
            value = uiState.birthDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")).orEmpty(),
            onValueChange = {},
            label = stringResource(R.string.register_birth_date),
            supportingText = stringResource(R.string.edit_profile_readonly_note),
            enabled = false,
        )

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = actions.onSave,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(stringResource(R.string.edit_profile_save))
            }
        }
    }
}
