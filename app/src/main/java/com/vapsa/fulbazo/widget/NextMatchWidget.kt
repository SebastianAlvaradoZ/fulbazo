package com.vapsa.fulbazo.widget

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.ui.match.monthFormatter
import com.vapsa.fulbazo.ui.match.peru
import com.vapsa.fulbazo.ui.match.shortLabel
import com.vapsa.fulbazo.ui.match.timeFormatter
import com.vapsa.fulbazo.ui.match.weekdayFormatter
import com.vapsa.fulbazo.ui.navigation.LaunchTarget
import com.vapsa.fulbazo.ui.theme.BackgroundLight
import com.vapsa.fulbazo.ui.theme.DarkColorScheme
import com.vapsa.fulbazo.ui.theme.LightColorScheme
import com.vapsa.fulbazo.ui.theme.Spacing
import com.vapsa.fulbazo.ui.theme.SurfaceContainerDark
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

/**
 * Widget "Mi próximo partido" con accesos rápidos. Dibuja lo último que guardó
 * [NextMatchStore]; quien lo actualiza es [NextMatchUpdater].
 *
 * Cambia de diseño según el espacio (no solo de tamaño): 2x2 muestra cuándo y dónde; 4x2
 * agrega los cupos y los accesos rápidos; 4x3, los siguientes partidos.
 */
class NextMatchWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(WidgetSizes.all)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val saved = context.nextMatchDataStore.data
        val initial = saved.first()
        provideContent {
            val state by saved.collectAsState(initial)
            GlanceTheme(colors = widgetColors) { NextMatchContent(state, LocalDateTime.now()) }
        }
    }
}

/** Colores "verde cancha" de la app, claros y oscuros según el sistema. */
private val widgetColors = ColorProviders(light = LightColorScheme, dark = DarkColorScheme)

/** En oscuro, un tono más claro que el fondo de la app para separarse del fondo de pantalla. */
private val WidgetBackground = ColorProvider(day = BackgroundLight, night = SurfaceContainerDark)

private object WidgetSizes {
    val Small = DpSize(110.dp, 110.dp)
    val SmallTall = DpSize(110.dp, 160.dp)
    val Wide = DpSize(250.dp, 110.dp)
    val Medium = DpSize(250.dp, 180.dp)
    val Large = DpSize(250.dp, 280.dp)

    val all = setOf(Small, SmallTall, Wide, Medium, Large)
}

/** Alto mínimo de todo lo que se toca. */
private val TouchTarget = 48.dp

@Composable
internal fun NextMatchContent(saved: NextMatchState?, now: LocalDateTime) {
    val state = saved?.at(now)
    val size = LocalSize.current
    val isSmall = size.width < WidgetSizes.Wide.width
    when {
        state == null -> MessageContent(R.string.widget_loading, target = LaunchTarget.Home, buttonRes = null)
        !state.signedIn -> MessageContent(
            R.string.widget_signed_out,
            target = LaunchTarget.Home,
            buttonRes = R.string.login_button,
        )
        state.next == null -> EmptyContent(state, isSmall)
        isSmall -> SmallContent(state.next!!, now, tall = size.height >= WidgetSizes.SmallTall.height)
        size.height >= WidgetSizes.Medium.height -> FullContent(
            state,
            now,
            large = size.height >= WidgetSizes.Large.height,
        )
        else -> WideContent(state, now)
    }
}

/** 2x2: cuándo y dónde. Tocar cualquier parte abre el partido. */
@Composable
private fun SmallContent(match: WidgetMatch, now: LocalDateTime, tall: Boolean) {
    WidgetSurface(LaunchTarget.MatchDetail(match.id), description = match.spoken(now)) {
        Text(
            text = match.dayText(now),
            maxLines = 1,
            style = text(12, GlanceTheme.colors.primary, FontWeight.Bold),
        )
        Text(
            text = match.dateTime.format(timeFormatter),
            maxLines = 1,
            style = text(28, GlanceTheme.colors.onSurface, FontWeight.Bold),
        )
        Text(
            text = match.courtName,
            maxLines = if (tall) 2 else 1,
            style = text(14, GlanceTheme.colors.onSurface, FontWeight.Medium),
        )
        if (tall) {
            Spacer(GlanceModifier.defaultWeight())
            SpotsText(match)
            Spacer(GlanceModifier.height(Spacing.xs))
            SpotsBar(match)
        }
    }
}

/** 4x1 o 4x2 muy bajo: una fila con el partido y los accesos como íconos. */
@Composable
private fun WideContent(state: NextMatchState, now: LocalDateTime) {
    val match = state.next ?: return
    WidgetSurface(LaunchTarget.MatchDetail(match.id), description = match.spoken(now), center = true) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MatchRow(match, now, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(Spacing.sm))
            IconAction(R.drawable.ic_add, R.string.create_match_button, LaunchTarget.CreateMatch)
        }
    }
}

/** 4x2 y 4x3: el partido, sus cupos y los accesos rápidos; en grande, los siguientes. */
@Composable
private fun FullContent(state: NextMatchState, now: LocalDateTime, large: Boolean) {
    val match = state.next ?: return
    val description = match.spoken(now)
    WidgetSurface(LaunchTarget.Home) {
        Row(
            GlanceModifier
                .fillMaxWidth()
                .clickable(openAction(LaunchTarget.MatchDetail(match.id)))
                .semantics { contentDescription = description },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MatchRow(match, now, GlanceModifier.defaultWeight())
            RefreshButton()
        }
        Spacer(GlanceModifier.height(Spacing.sm))
        // En grupos: Glance dibuja como máximo 10 elementos por columna.
        Column(GlanceModifier.fillMaxWidth()) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SpotsText(match, GlanceModifier.defaultWeight())
                UpdatedText(state)
            }
            Spacer(GlanceModifier.height(Spacing.xs))
            SpotsBar(match)
        }
        if (large) {
            Column(GlanceModifier.fillMaxWidth().padding(top = Spacing.sm)) {
                state.matches.drop(1).take(2).forEach { LaterMatchRow(it, now) }
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        Spacer(GlanceModifier.height(Spacing.sm))
        QuickActions(state)
    }
}

@Composable
private fun EmptyContent(state: NextMatchState, isSmall: Boolean) {
    if (isSmall || LocalSize.current.height < WidgetSizes.Medium.height) {
        MessageContent(R.string.widget_empty, LaunchTarget.CreateMatch, R.string.create_match_button)
        return
    }
    WidgetSurface(LaunchTarget.Home) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Logo()
            Spacer(GlanceModifier.width(Spacing.sm))
            Text(
                text = LocalContext.current.getString(R.string.widget_name),
                maxLines = 1,
                style = text(16, GlanceTheme.colors.onSurface, FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight(),
            )
            RefreshButton()
        }
        Spacer(GlanceModifier.defaultWeight())
        Text(
            text = LocalContext.current.getString(R.string.widget_empty),
            style = text(14, GlanceTheme.colors.onSurfaceVariant),
        )
        UpdatedText(state)
        Spacer(GlanceModifier.defaultWeight())
        QuickActions(state)
    }
}

/** Sin sesión, sin partidos (pequeño) o cargando: logo, una frase y qué hacer. */
@Composable
private fun MessageContent(messageRes: Int, target: LaunchTarget, buttonRes: Int?) {
    val showButton = buttonRes != null && LocalSize.current.height >= WidgetSizes.SmallTall.height
    WidgetSurface(target, center = true) {
        Column(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Logo()
            Spacer(GlanceModifier.height(Spacing.xs))
            Text(
                text = LocalContext.current.getString(messageRes),
                maxLines = 3,
                style = text(13, GlanceTheme.colors.onSurface, FontWeight.Medium, TextAlign.Center),
            )
            if (showButton) {
                Spacer(GlanceModifier.height(Spacing.sm))
                ActionButton(buttonRes!!, iconRes = null, target, filled = true, GlanceModifier.fillMaxWidth())
            }
        }
    }
}

/** Bloque de fecha, losa y hora. */
@Composable
private fun MatchRow(match: WidgetMatch, now: LocalDateTime, modifier: GlanceModifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        DateBlock(match.dateTime)
        Spacer(GlanceModifier.width(12.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(
                text = LocalContext.current.getString(R.string.widget_name),
                maxLines = 1,
                style = text(12, GlanceTheme.colors.primary, FontWeight.Medium),
            )
            Text(
                text = match.courtName,
                maxLines = 1,
                style = text(16, GlanceTheme.colors.onSurface, FontWeight.Bold),
            )
            Text(
                text = "${match.dayText(now)}, ${match.dateTime.format(timeFormatter)}",
                maxLines = 1,
                style = text(14, GlanceTheme.colors.onSurfaceVariant),
            )
        }
    }
}

/** Día de la semana, número y mes, igual que en las tarjetas de la app. */
@Composable
private fun DateBlock(dateTime: LocalDateTime) {
    val color = GlanceTheme.colors.onPrimaryContainer
    Column(
        modifier = GlanceModifier
            .width(52.dp)
            .rounded(GlanceTheme.colors.primaryContainer, 14.dp)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(dateTime.format(weekdayFormatter).shortLabel(), maxLines = 1, style = text(11, color, FontWeight.Medium))
        Text(dateTime.dayOfMonth.toString(), maxLines = 1, style = text(22, color, FontWeight.Bold))
        Text(dateTime.format(monthFormatter).shortLabel(), maxLines = 1, style = text(11, color, FontWeight.Medium))
    }
}

/** Un partido de la lista del tamaño grande. */
@Composable
private fun LaterMatchRow(match: WidgetMatch, now: LocalDateTime) {
    val description = match.spoken(now)
    Row(
        GlanceModifier
            .fillMaxWidth()
            .height(TouchTarget)
            .clickable(openAction(LaunchTarget.MatchDetail(match.id)))
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_sports_soccer),
            contentDescription = null,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
            modifier = GlanceModifier.size(20.dp),
        )
        Spacer(GlanceModifier.width(12.dp))
        Text(
            text = match.courtName,
            maxLines = 1,
            style = text(14, GlanceTheme.colors.onSurface, FontWeight.Medium),
            modifier = GlanceModifier.defaultWeight(),
        )
        Spacer(GlanceModifier.width(Spacing.sm))
        Text(
            text = "${match.dayText(now)}, ${match.dateTime.format(timeFormatter)}",
            maxLines = 1,
            style = text(12, GlanceTheme.colors.onSurfaceVariant),
        )
    }
}

@Composable
private fun SpotsText(match: WidgetMatch, modifier: GlanceModifier = GlanceModifier) {
    val context = LocalContext.current
    val full = match.spotsLeft == 0
    Text(
        text = if (full) {
            context.getString(R.string.match_full)
        } else {
            context.getString(R.string.match_spots_left, match.spotsLeft, match.slots)
        },
        maxLines = 1,
        style = text(12, if (full) GlanceTheme.colors.error else GlanceTheme.colors.primary, FontWeight.Medium),
        modifier = modifier,
    )
}

@Composable
private fun SpotsBar(match: WidgetMatch) {
    LinearProgressIndicator(
        progress = match.progress,
        color = if (match.spotsLeft == 0) GlanceTheme.colors.error else GlanceTheme.colors.primary,
        backgroundColor = GlanceTheme.colors.surfaceVariant,
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(6.dp),
    )
}

@Composable
private fun UpdatedText(state: NextMatchState) {
    val updatedAt = state.updatedAt ?: return
    Text(
        text = LocalContext.current.getString(R.string.widget_updated, updatedAt.format(timeFormatter)),
        maxLines = 1,
        style = text(11, GlanceTheme.colors.onSurfaceVariant),
    )
}

/** Accesos rápidos: crear un partido y ver las invitaciones pendientes. */
@Composable
private fun QuickActions(state: NextMatchState) {
    Row(GlanceModifier.fillMaxWidth()) {
        ActionButton(
            R.string.create_match_button,
            R.drawable.ic_add,
            LaunchTarget.CreateMatch,
            filled = true,
            GlanceModifier.defaultWeight(),
        )
        Spacer(GlanceModifier.width(Spacing.sm))
        ActionButton(
            R.string.invitations_title,
            R.drawable.ic_notifications,
            LaunchTarget.Invitations,
            filled = false,
            GlanceModifier.defaultWeight(),
            badge = state.pendingInvitations,
        )
    }
}

@Composable
private fun ActionButton(
    textRes: Int,
    iconRes: Int?,
    target: LaunchTarget,
    filled: Boolean,
    modifier: GlanceModifier,
    badge: Int = 0,
) {
    val context = LocalContext.current
    val container = if (filled) GlanceTheme.colors.primary else GlanceTheme.colors.secondaryContainer
    val content = if (filled) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSecondaryContainer
    val label = context.getString(textRes)
    Row(
        modifier = modifier
            .height(TouchTarget)
            .rounded(container, 24.dp)
            .clickable(openAction(target))
            .semantics {
                contentDescription =
                    if (badge > 0) context.getString(R.string.widget_invitations_pending, badge) else label
            }
            .padding(horizontal = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        iconRes?.let {
            Image(
                provider = ImageProvider(it),
                contentDescription = null,
                colorFilter = ColorFilter.tint(content),
                modifier = GlanceModifier.size(20.dp),
            )
            Spacer(GlanceModifier.width(Spacing.xs))
        }
        Text(label, maxLines = 1, style = text(14, content, FontWeight.Medium))
        if (badge > 0) {
            Spacer(GlanceModifier.width(6.dp))
            Box(
                GlanceModifier
                    .size(22.dp)
                    .rounded(GlanceTheme.colors.tertiary, 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (badge > MAX_BADGE) "$MAX_BADGE+" else badge.toString(),
                    maxLines = 1,
                    style = text(12, GlanceTheme.colors.onTertiary, FontWeight.Bold),
                )
            }
        }
    }
}

@Composable
private fun IconAction(iconRes: Int, labelRes: Int, target: LaunchTarget) {
    Box(
        GlanceModifier
            .size(TouchTarget)
            .rounded(GlanceTheme.colors.primary, 24.dp)
            .clickable(openAction(target)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = LocalContext.current.getString(labelRes),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
            modifier = GlanceModifier.size(24.dp),
        )
    }
}

/** Actualiza a mano: con la app cerrada Android solo refresca cada 15 minutos. */
@Composable
private fun RefreshButton() {
    Box(
        GlanceModifier
            .size(TouchTarget)
            .clickable(actionRunCallback<RefreshNextMatchAction>()),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = LocalContext.current.getString(R.string.widget_refresh),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
            modifier = GlanceModifier.size(24.dp),
        )
    }
}

@Composable
private fun Logo() {
    Image(
        provider = ImageProvider(R.drawable.ic_fulbazo_logo),
        contentDescription = null,
        modifier = GlanceModifier.size(36.dp),
    )
}

/**
 * Fondo del widget: llena todo su espacio, usa las esquinas del sistema (Android 12+) y
 * deja 16 dp por dentro. Tocar el fondo abre [target].
 */
@Composable
private fun WidgetSurface(
    target: LaunchTarget,
    description: String? = null,
    center: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val background = GlanceModifier
        .fillMaxSize()
        .appWidgetBackground()
        .let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                it.background(WidgetBackground)
                    .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            } else {
                it.rounded(WidgetBackground, 16.dp)
            }
        }
        .clickable(openAction(target))
    Column(
        modifier = (if (description == null) background else background.semantics { contentDescription = description })
            .padding(Spacing.md),
        verticalAlignment = if (center) Alignment.CenterVertically else Alignment.Top,
        content = content,
    )
}

/** Fondo con esquinas redondeadas; antes de Android 12 se dibuja con una imagen. */
private fun GlanceModifier.rounded(color: ColorProvider, radius: Dp): GlanceModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        background(color).cornerRadius(radius)
    } else {
        background(ImageProvider(R.drawable.widget_rounded), colorFilter = ColorFilter.tint(color))
    }

@Composable
private fun openAction(target: LaunchTarget) =
    actionStartActivity(LaunchTarget.intent(LocalContext.current, target))

private fun text(
    size: Int,
    color: ColorProvider,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Start,
) = TextStyle(color = color, fontSize = size.sp, fontWeight = weight, textAlign = align)

/** "Hoy", "Mañana" o "Mié 25 nov". */
@Composable
private fun WidgetMatch.dayText(now: LocalDateTime): String = dayText(LocalContext.current, now)

private fun WidgetMatch.dayText(context: Context, now: LocalDateTime): String = when (dayFrom(now.toLocalDate())) {
    WidgetDay.TODAY -> context.getString(R.string.widget_today)
    WidgetDay.TOMORROW -> context.getString(R.string.widget_tomorrow)
    WidgetDay.LATER -> {
        val weekday = dateTime.format(weekdayFormatter).trimEnd('.').replaceFirstChar { it.titlecase(peru) }
        "$weekday ${dateTime.dayOfMonth} ${dateTime.format(monthFormatter).trimEnd('.')}"
    }
}

/** Lo que lee TalkBack: el partido completo en una frase. */
@Composable
private fun WidgetMatch.spoken(now: LocalDateTime): String {
    val context = LocalContext.current
    return context.getString(
        R.string.widget_match_description,
        courtName,
        dayText(context, now),
        dateTime.format(timeFormatter),
        spotsLeft,
    )
}

private const val MAX_BADGE = 9
