package com.vapsa.fulbazo.ui.match

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.MatchLevel
import com.vapsa.fulbazo.domain.model.MatchStatus
import com.vapsa.fulbazo.ui.components.FulbazoCard
import com.vapsa.fulbazo.ui.theme.Spacing
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val matchDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE d 'de' MMM, HH:mm", Locale.forLanguageTag("es-PE"))

/** Fecha del partido en español, p. ej. "mié 11 de nov., 19:00". */
fun LocalDateTime.formatMatchDate(): String =
    format(matchDateFormatter).replaceFirstChar { it.titlecase(Locale.forLanguageTag("es-PE")) }

/** Distancia hasta la losa: "a 850 m" si es menos de 1 km, si no "a 2.4 km". */
@Composable
fun formatDistance(km: Double): String =
    if (km < 1.0) {
        stringResource(R.string.match_distance_m, ((km * 1000) / 10).roundToInt() * 10)
    } else {
        stringResource(R.string.match_distance_km, km)
    }

@get:StringRes
val MatchLevel.labelRes: Int
    get() = when (this) {
        MatchLevel.ANY -> R.string.level_any
        MatchLevel.BEGINNER -> R.string.level_beginner
        MatchLevel.INTERMEDIATE -> R.string.level_intermediate
        MatchLevel.ADVANCED -> R.string.level_advanced
    }

/**
 * Tarjeta de un partido: a la izquierda la fecha en grande; a la derecha la
 * losa, el anfitrión, chips con la hora, la distancia y el nivel, y una barra
 * con los cupos ocupados. [extraContent] permite agregar botones (por ejemplo,
 * en invitaciones) y [onClick] hace que la tarjeta se pueda tocar.
 */
@Composable
fun MatchCard(
    item: MatchItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    extraContent: @Composable (() -> Unit)? = null,
) {
    FulbazoCard(modifier = modifier.fillMaxWidth(), onClick = onClick) { MatchCardContent(item, extraContent) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchCardContent(item: MatchItem, extraContent: @Composable (() -> Unit)?) {
    // Los cupos libres cuentan hacia arriba o abajo cuando alguien entra o sale.
    val spotsLeft by animateIntAsState(targetValue = item.spotsLeft, label = "spotsLeft")
    val finished = item.status == MatchStatus.FINISHED
    Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            DateBlock(item.dateTime, finished)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = item.courtName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when {
                        finished && item.isHost -> stringResource(R.string.match_you_were_host)
                        finished && item.isPlayer -> stringResource(R.string.match_you_played)
                        item.isHost -> stringResource(R.string.match_you_are_host)
                        item.isPlayer -> stringResource(R.string.match_you_joined)
                        else -> stringResource(R.string.match_host, item.hostNickname)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.padding(top = Spacing.xs),
                ) {
                    InfoChip(R.drawable.ic_schedule, item.dateTime.format(timeFormatter))
                    InfoChip(R.drawable.ic_place, item.district)
                    item.distanceKm?.let { InfoChip(R.drawable.ic_map, formatDistance(it)) }
                    LevelChip(item.level)
                }
            }
        }
        SpotsBar(item, spotsLeft, finished)
        if (item.description.isNotBlank()) {
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        extraContent?.invoke()
    }
}

/** Día de la semana, número y mes, como en un calendario. */
@Composable
private fun DateBlock(dateTime: LocalDateTime, finished: Boolean) {
    val container = if (finished) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
    val content = if (finished) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
    Surface(shape = MaterialTheme.shapes.medium, color = container, contentColor = content) {
        Column(
            modifier = Modifier
                .width(56.dp)
                .padding(vertical = Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(dateTime.format(weekdayFormatter).shortLabel(), style = MaterialTheme.typography.labelSmall)
            Text(
                dateTime.dayOfMonth.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(dateTime.format(monthFormatter).shortLabel(), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Dato corto con ícono: hora, distrito o distancia. */
@Composable
private fun InfoChip(@DrawableRes iconRes: Int, text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(14.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

/** Nivel con color: verde principiante, naranja intermedio, rojo avanzado. */
@Composable
private fun LevelChip(level: MatchLevel) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (level) {
        MatchLevel.ANY -> scheme.secondaryContainer to scheme.onSecondaryContainer
        MatchLevel.BEGINNER -> scheme.primaryContainer to scheme.onPrimaryContainer
        MatchLevel.INTERMEDIATE -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        MatchLevel.ADVANCED -> scheme.errorContainer to scheme.onErrorContainer
    }
    Surface(shape = CircleShape, color = container, contentColor = content) {
        Text(
            stringResource(level.labelRes),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = Spacing.xs),
        )
    }
}

/** Cupos: el texto y una barra que se llena con los jugadores inscritos. */
@Composable
private fun SpotsBar(item: MatchItem, spotsLeft: Int, finished: Boolean) {
    val taken = (item.slots - item.spotsLeft).coerceIn(0, item.slots)
    val progress by animateFloatAsState(
        targetValue = if (item.slots == 0) 0f else taken.toFloat() / item.slots,
        label = "spotsProgress",
    )
    val color = when {
        finished -> MaterialTheme.colorScheme.onSurfaceVariant
        item.spotsLeft == 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(painterResource(R.drawable.ic_group), contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Text(
                text = when {
                    finished -> stringResource(R.string.match_finished)
                    item.spotsLeft == 0 -> stringResource(R.string.match_full)
                    else -> stringResource(R.string.match_spots_left, spotsLeft, item.slots)
                },
                style = MaterialTheme.typography.labelLarge,
                color = color,
            )
        }
        if (!finished) {
            LinearProgressIndicator(
                progress = { progress },
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
                drawStopIndicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            )
        }
    }
}

private val peru = Locale.forLanguageTag("es-PE")
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", peru)
private val weekdayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", peru)
private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", peru)

/** "jue." a "JUE". */
private fun String.shortLabel(): String = trimEnd('.').uppercase(peru)
