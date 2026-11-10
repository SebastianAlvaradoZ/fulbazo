package com.vapsa.fulbazo.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vapsa.fulbazo.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Campo de solo lectura que abre un TimePicker de Material 3 (formato 24 h).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(
    value: LocalTime?,
    onValueChange: (LocalTime) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    initialTime: LocalTime = LocalTime.of(18, 0),
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) showDialog = true
        }
    }

    FormTextField(
        value = value?.format(formatter).orEmpty(),
        onValueChange = {},
        label = label,
        modifier = modifier,
        error = error,
        enabled = enabled,
        readOnly = true,
        interactionSource = interactionSource,
        trailingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_schedule),
                contentDescription = stringResource(R.string.create_match_pick_time),
            )
        },
    )

    if (showDialog) {
        val start = value ?: initialTime
        val pickerState = rememberTimePickerState(
            initialHour = start.hour,
            initialMinute = start.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(LocalTime.of(pickerState.hour, pickerState.minute))
                    showDialog = false
                }) {
                    Text(stringResource(R.string.dialog_accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
            text = { TimePicker(state = pickerState) },
        )
    }
}
