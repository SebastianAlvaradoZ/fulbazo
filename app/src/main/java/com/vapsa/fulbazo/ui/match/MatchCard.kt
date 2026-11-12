package com.vapsa.fulbazo.ui.match

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.MatchLevel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val matchDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE d 'de' MMM, HH:mm", Locale.forLanguageTag("es-PE"))

/** Fecha del partido en español, p. ej. "mié 11 de nov., 19:00". */
fun LocalDateTime.formatMatchDate(): String =
    format(matchDateFormatter).replaceFirstChar { it.titlecase(Locale.forLanguageTag("es-PE")) }

@get:StringRes
val MatchLevel.labelRes: Int
    get() = when (this) {
        MatchLevel.ANY -> R.string.level_any
        MatchLevel.BEGINNER -> R.string.level_beginner
        MatchLevel.INTERMEDIATE -> R.string.level_intermediate
        MatchLevel.ADVANCED -> R.string.level_advanced
    }

/**
 * Tarjeta de un partido: losa, fecha, anfitrión, cupos y nivel.
 * [extraContent] permite agregar botones (por ejemplo, en invitaciones) y
 * [onClick] hace que la tarjeta se pueda tocar.
 */
@Composable
fun MatchCard(
    item: MatchItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    extraContent: @Composable (() -> Unit)? = null,
) {
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth()) { MatchCardContent(item, extraContent) }
    } else {
        Card(modifier = modifier.fillMaxWidth()) { MatchCardContent(item, extraContent) }
    }
}

@Composable
private fun MatchCardContent(item: MatchItem, extraContent: @Composable (() -> Unit)?) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_place),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = item.courtName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = "${item.dateTime.formatMatchDate()} · ${item.district}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = when {
                item.isHost -> stringResource(R.string.match_you_are_host)
                item.isPlayer -> stringResource(R.string.match_you_joined)
                else -> stringResource(R.string.match_host, item.hostNickname)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (item.spotsLeft == 0) {
                    stringResource(R.string.match_full)
                } else {
                    stringResource(R.string.match_spots_left, item.spotsLeft, item.slots)
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (item.spotsLeft == 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
            AssistChip(onClick = {}, label = { Text(stringResource(item.level.labelRes)) })
        }
        if (item.description.isNotBlank()) {
            Text(text = item.description, style = MaterialTheme.typography.bodySmall)
        }
        extraContent?.invoke()
    }
}
