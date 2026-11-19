package com.vapsa.fulbazo.ui.friends.group

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.FriendGroup
import com.vapsa.fulbazo.ui.components.FormTextField
import com.vapsa.fulbazo.ui.components.LoadingState
import com.vapsa.fulbazo.ui.components.UserAvatar

@Composable
fun GroupEditorRoute(
    groupId: String?,
    onDone: () -> Unit,
    viewModel: GroupEditorViewModel = hiltViewModel<GroupEditorViewModel, GroupEditorViewModel.Factory>(
        key = "group-$groupId",
    ) { factory -> factory.create(groupId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.isDone) { if (uiState.isDone) onDone() }
    LaunchedEffect(uiState.saveFailed) {
        if (uiState.saveFailed) {
            snackbarHostState.showSnackbar(context.getString(R.string.generic_error))
            viewModel.onErrorShown()
        }
    }

    GroupEditorScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onDone,
        onNameChange = viewModel::onNameChange,
        onToggleMember = viewModel::onToggleMember,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupEditorScreen(
    uiState: GroupEditorUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onToggleMember: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val enabled = !uiState.isSaving

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (uiState.isNew) R.string.group_new_title else R.string.group_edit_title))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (!uiState.isNew) {
                        IconButton(onClick = { confirmDelete = true }, enabled = enabled) {
                            Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.action_delete))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingState(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FormTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.group_name),
                enabled = enabled,
                error = when (uiState.nameError) {
                    GroupNameError.REQUIRED -> stringResource(R.string.group_name_required)
                    GroupNameError.TOO_LONG -> stringResource(R.string.group_name_too_long, FriendGroup.MAX_NAME_LENGTH)
                    null -> null
                },
            )

            Text(stringResource(R.string.group_members), style = MaterialTheme.typography.titleMedium)
            if (uiState.membersMissing) {
                Text(
                    stringResource(R.string.group_members_missing),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (uiState.friends.isEmpty()) {
                Text(
                    stringResource(R.string.group_no_friends),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            uiState.friends.forEach { friend ->
                val checked = friend.id in uiState.memberIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = checked,
                            enabled = enabled,
                            role = Role.Checkbox,
                            onValueChange = { onToggleMember(friend.id) },
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
                    Spacer(Modifier.width(8.dp))
                    UserAvatar(photoUri = friend.photoUri, contentDescription = null, size = 40.dp, name = friend.fullName)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(friend.nickname, style = MaterialTheme.typography.titleMedium)
                        Text(
                            friend.fullName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onSave,
                enabled = enabled && uiState.friends.isNotEmpty(),
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
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.group_delete_title)) },
            text = { Text(stringResource(R.string.group_delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
