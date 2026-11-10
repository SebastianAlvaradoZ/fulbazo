package com.vapsa.fulbazo.domain.model

import java.time.LocalDateTime

/** Nivel de juego que busca el anfitrión. */
enum class MatchLevel {
    ANY,
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
}

enum class MatchStatus {
    OPEN,
    FULL,
    CANCELLED,
    FINISHED,
}

/**
 * Partido creado por un anfitrión en una losa.
 * El anfitrión siempre está incluido en [playerIds].
 */
data class Match(
    val id: String,
    val courtId: String,
    val hostId: String,
    val dateTime: LocalDateTime,
    val slots: Int,
    val level: MatchLevel,
    val status: MatchStatus,
    val playerIds: List<String>,
    val description: String = "",
) {
    val spotsLeft: Int get() = (slots - playerIds.size).coerceAtLeast(0)
}

/** Datos para crear un partido. */
data class NewMatch(
    val courtId: String,
    val dateTime: LocalDateTime,
    val slots: Int,
    val level: MatchLevel,
    val description: String = "",
) {
    companion object {
        const val MIN_SLOTS = 2
        const val MAX_SLOTS = 22
        const val DEFAULT_SLOTS = 10
        const val MAX_DESCRIPTION_LENGTH = 200
    }
}
