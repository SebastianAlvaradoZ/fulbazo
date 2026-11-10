package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.domain.model.Match
import com.vapsa.fulbazo.domain.model.MatchLevel
import com.vapsa.fulbazo.domain.model.MatchStatus
import com.vapsa.fulbazo.domain.model.NewMatch
import com.vapsa.fulbazo.domain.repository.MatchRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/**
 * Partidos en memoria, con algunos de ejemplo creados por jugadores de prueba.
 * Se reemplazará por Firestore.
 */
class FakeMatchRepository(
    private val latencyMillis: Long = 600,
    today: LocalDate = LocalDate.now(),
) : MatchRepository {

    private val matches = MutableStateFlow(
        listOf(
            Match(
                id = SEED_MATCH_ID,
                courtId = "losa-umacollo",
                hostId = "carlos",
                dateTime = today.plusDays(1).atTime(LocalTime.of(19, 0)),
                slots = 10,
                level = MatchLevel.INTERMEDIATE,
                status = MatchStatus.OPEN,
                playerIds = listOf("carlos", "maria", "pedro"),
                description = "Pichanga de 5 contra 5, traer polo blanco y oscuro.",
            ),
            Match(
                id = "match-cayma",
                courtId = "losa-cayma",
                hostId = "valeria",
                dateTime = today.plusDays(2).atTime(LocalTime.of(17, 30)),
                slots = 12,
                level = MatchLevel.ANY,
                status = MatchStatus.OPEN,
                playerIds = listOf("valeria", "rodrigo"),
            ),
            Match(
                id = "match-melgar",
                courtId = "losa-melgar",
                hostId = "jose",
                dateTime = today.plusDays(3).atTime(LocalTime.of(8, 0)),
                slots = 8,
                level = MatchLevel.ADVANCED,
                status = MatchStatus.OPEN,
                playerIds = listOf("jose"),
                description = "Partido temprano, nivel competitivo.",
            ),
        ),
    )

    override fun observeMatches(): Flow<List<Match>> = matches.asStateFlow()

    override suspend fun createMatch(hostId: String, newMatch: NewMatch): Result<Match> {
        delay(latencyMillis)
        val match = Match(
            id = UUID.randomUUID().toString(),
            courtId = newMatch.courtId,
            hostId = hostId,
            dateTime = newMatch.dateTime,
            slots = newMatch.slots,
            level = newMatch.level,
            status = MatchStatus.OPEN,
            playerIds = listOf(hostId),
            description = newMatch.description,
        )
        matches.update { it + match }
        return Result.success(match)
    }

    override suspend fun joinMatch(matchId: String, userId: String): Result<Unit> {
        delay(latencyMillis)
        val match = matches.value.firstOrNull { it.id == matchId }
            ?: return Result.failure(NoSuchElementException("El partido no existe"))
        if (userId in match.playerIds) return Result.success(Unit)
        if (match.spotsLeft == 0) return Result.failure(IllegalStateException("El partido está lleno"))

        matches.update { list ->
            list.map {
                if (it.id != matchId) {
                    it
                } else {
                    val players = it.playerIds + userId
                    it.copy(
                        playerIds = players,
                        status = if (players.size >= it.slots) MatchStatus.FULL else it.status,
                    )
                }
            }
        }
        return Result.success(Unit)
    }

    companion object {
        /** Partido de ejemplo al que el simulador invita al usuario. */
        const val SEED_MATCH_ID = "match-umacollo"

        /** Útil en tests para crear fechas futuras. */
        fun inDays(days: Long, hour: Int = 18): LocalDateTime =
            LocalDate.now().plusDays(days).atTime(hour, 0)
    }
}
