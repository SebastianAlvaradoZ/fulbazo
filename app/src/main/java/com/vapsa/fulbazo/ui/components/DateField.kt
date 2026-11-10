package com.vapsa.fulbazo.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val MIN_YEAR = 1940

/**
 * Campo de solo lectura que abre un DatePicker de Material 3 al tocarlo.
 * Solo permite elegir fechas entre [minDate] (si se indica) y [maxDate].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    maxDate: LocalDate = LocalDate.now(),
    minDate: LocalDate? = null,
    initialDisplayedDate: LocalDate = maxDate,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    // Abre el selector cuando se suelta un toque sobre el campo de solo lectura.
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
                painter = painterResource(R.drawable.ic_calendar),
                contentDescription = stringResource(R.string.register_pick_date),
            )
        },
    )

    if (showDialog) {
        val maxMillis = maxDate.toUtcMillis()
        val minMillis = minDate?.toUtcMillis() ?: Long.MIN_VALUE
        val minYear = minDate?.year ?: MIN_YEAR
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = value?.toUtcMillis(),
            initialDisplayedMonthMillis = (value ?: initialDisplayedDate).toUtcMillis(),
            yearRange = minYear..maxDate.year,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis in minMillis..maxMillis

                override fun isSelectableYear(year: Int) = year in minYear..maxDate.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { onValueChange(it.toUtcLocalDate()) }
                        showDialog = false
                    },
                    enabled = pickerState.selectedDateMillis != null,
                ) {
                    Text(stringResource(R.string.dialog_accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// El DatePicker trabaja con milisegundos en UTC.
private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
