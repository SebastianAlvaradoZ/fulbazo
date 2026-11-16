package com.vapsa.fulbazo.domain.repository

import com.vapsa.fulbazo.domain.model.CommunityStats
import com.vapsa.fulbazo.domain.model.PlayerComment
import com.vapsa.fulbazo.domain.model.TeamPlayer
import com.vapsa.fulbazo.domain.model.TeamSplit

/** Funciones de inteligencia artificial (Gemini con Firebase AI Logic). */
interface AiRepository {

    /** Resumen corto y neutral de lo que dicen del jugador sus compañeros. */
    suspend fun summarizeReviews(
        nickname: String,
        comments: List<PlayerComment>,
        community: CommunityStats,
    ): Result<String>

    /** Dos equipos parejos con todos los [players]. */
    suspend fun buildTeams(players: List<TeamPlayer>): Result<TeamSplit>
}
