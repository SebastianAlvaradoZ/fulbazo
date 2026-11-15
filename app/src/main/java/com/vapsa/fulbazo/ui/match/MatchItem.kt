package com.vapsa.fulbazo.ui.match

import com.vapsa.fulbazo.domain.model.Court
import com.vapsa.fulbazo.domain.model.Match
import com.vapsa.fulbazo.domain.model.MatchLevel
import com.vapsa.fulbazo.domain.model.MatchStatus
import com.vapsa.fulbazo.domain.model.PlayerSummary
import java.time.LocalDateTime

/**
 * Datos de un partido listos para mostrarse en una tarjeta.
 */
data class MatchItem(
    val matchId: String,
    val courtId: String,
    val courtName: String,
    val district: String,
    val latitude: Double?,
    val longitude: Double?,
    val dateTime: LocalDateTime,
    val slots: Int,
    val spotsLeft: Int,
    val level: MatchLevel,
    val status: MatchStatus,
    val hostNickname: String,
    val isHost: Boolean,
    val isPlayer: Boolean,
    val description: String,
    /** Distancia desde tu ubicación, o `null` si no la conocemos. */
    val distanceKm: Double? = null,
)

/** Une el partido con su losa y su anfitrión. */
fun Match.toItem(
    courts: Map<String, Court>,
    players: Map<String, PlayerSummary>,
    currentUserId: String?,
): MatchItem {
    val court = courts[courtId]
    return MatchItem(
        matchId = id,
        courtId = courtId,
        courtName = court?.name ?: courtId,
        district = court?.district.orEmpty(),
        latitude = court?.latitude,
        longitude = court?.longitude,
        dateTime = dateTime,
        slots = slots,
        spotsLeft = spotsLeft,
        level = level,
        status = status,
        hostNickname = players[hostId]?.nickname ?: hostId,
        isHost = hostId == currentUserId,
        isPlayer = currentUserId != null && currentUserId in playerIds,
        description = description,
    )
}
