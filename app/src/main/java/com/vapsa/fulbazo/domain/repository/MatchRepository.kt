package com.vapsa.fulbazo.domain.repository

import com.vapsa.fulbazo.domain.model.Match
import com.vapsa.fulbazo.domain.model.NewMatch
import kotlinx.coroutines.flow.Flow

interface MatchRepository {

    /** Todos los partidos; se vuelve a emitir cada vez que cambian. */
    fun observeMatches(): Flow<List<Match>>

    /** Crea un partido con [hostId] como anfitrión y primer jugador. */
    suspend fun createMatch(hostId: String, newMatch: NewMatch): Result<Match>

    /** Agrega al jugador si hay cupo. Falla si el partido está lleno o no existe. */
    suspend fun joinMatch(matchId: String, userId: String): Result<Unit>
}
