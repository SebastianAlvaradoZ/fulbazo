package com.vapsa.fulbazo.domain.repository

import com.vapsa.fulbazo.domain.model.PlayerSummary
import kotlinx.coroutines.flow.Flow

/** Directorio público de jugadores. */
interface PlayerRepository {

    /** Todos los jugadores indexados por id. */
    fun observePlayers(): Flow<Map<String, PlayerSummary>>

    /** Busca por apodo (sin distinguir mayúsculas), excluyendo a [excludeUserId]. */
    suspend fun searchByNickname(query: String, excludeUserId: String): List<PlayerSummary>
}
