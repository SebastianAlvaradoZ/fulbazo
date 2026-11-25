package com.vapsa.fulbazo.widget

import com.vapsa.fulbazo.domain.model.Court
import com.vapsa.fulbazo.domain.model.InvitationStatus
import com.vapsa.fulbazo.domain.model.Match
import com.vapsa.fulbazo.domain.model.MatchInvitation
import com.vapsa.fulbazo.domain.model.MatchStatus
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.time.LocalDateTime

/** Lo que muestra el widget. Se guarda en el teléfono para dibujarlo sin abrir la app. */
@Serializable
data class NextMatchState(
    val signedIn: Boolean,
    /** Mis próximos partidos, del más cercano al más lejano. */
    val matches: List<WidgetMatch> = emptyList(),
    val pendingInvitations: Int = 0,
    /** Última vez que los datos llegaron del servidor, o `null` si nunca. */
    @Serializable(LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime? = null,
) {
    /** El mismo estado sin los partidos que ya empezaron (los datos pueden ser viejos). */
    fun at(now: LocalDateTime): NextMatchState = copy(matches = matches.filter { it.dateTime.isAfter(now) })

    val next: WidgetMatch? get() = matches.firstOrNull()

    companion object {
        /** Se guardan más de los que se ven, por si pasan varios sin conexión. */
        const val MAX_MATCHES = 8

        val SignedOut = NextMatchState(signedIn = false)
    }
}

@Serializable
data class WidgetMatch(
    val id: String,
    val courtName: String,
    val district: String,
    @Serializable(LocalDateTimeSerializer::class)
    val dateTime: LocalDateTime,
    val slots: Int,
    val spotsLeft: Int,
) {
    /** Parte de los cupos ya ocupada, de 0 a 1. */
    val progress: Float
        get() = if (slots <= 0) 0f else ((slots - spotsLeft).toFloat() / slots).coerceIn(0f, 1f)

    fun dayFrom(today: LocalDate): WidgetDay = when (dateTime.toLocalDate()) {
        today -> WidgetDay.TODAY
        today.plusDays(1) -> WidgetDay.TOMORROW
        else -> WidgetDay.LATER
    }
}

enum class WidgetDay { TODAY, TOMORROW, LATER }

/** Arma el estado del widget para [userId]: mismos criterios que "Mis partidos". */
fun buildNextMatchState(
    userId: String,
    matches: List<Match>,
    courts: List<Court>,
    invitations: List<MatchInvitation>,
    now: LocalDateTime,
): NextMatchState {
    val courtsById = courts.associateBy { it.id }
    return NextMatchState(
        signedIn = true,
        matches = matches
            .filter { userId in it.playerIds && it.dateTime.isAfter(now) }
            .filter { it.status != MatchStatus.CANCELLED && it.status != MatchStatus.FINISHED }
            .sortedBy { it.dateTime }
            .take(NextMatchState.MAX_MATCHES)
            .map { match ->
                val court = courtsById[match.courtId]
                WidgetMatch(
                    id = match.id,
                    courtName = court?.name ?: match.courtId,
                    district = court?.district.orEmpty(),
                    dateTime = match.dateTime,
                    slots = match.slots,
                    spotsLeft = match.spotsLeft,
                )
            },
        pendingInvitations = invitations.count { it.status == InvitationStatus.PENDING },
    )
}

/** Guarda la fecha como texto ISO ("2026-11-25T19:00:00"). */
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDateTime = LocalDateTime.parse(decoder.decodeString())
}
