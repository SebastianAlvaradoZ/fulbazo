package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.domain.ai.LocalTeamBalancer
import com.vapsa.fulbazo.domain.model.CommunityStats
import com.vapsa.fulbazo.domain.model.PlayerComment
import com.vapsa.fulbazo.domain.model.TeamPlayer
import com.vapsa.fulbazo.domain.model.TeamSplit
import com.vapsa.fulbazo.domain.repository.AiRepository
import kotlinx.coroutines.delay

/**
 * IA falsa para las pruebas: responde al instante, sin red. Con [fail] en `true`
 * simula que Gemini no responde.
 */
class FakeAiRepository(
    private val latencyMillis: Long = 300,
    var fail: Boolean = false,
) : AiRepository {

    /** Cuántas veces se pidió un resumen (para comprobar que se usa el guardado). */
    var summaryCalls = 0
        private set

    override suspend fun summarizeReviews(
        nickname: String,
        comments: List<PlayerComment>,
        community: CommunityStats,
    ): Result<String> {
        delay(latencyMillis)
        summaryCalls++
        if (fail) return Result.failure(IllegalStateException("Gemini no respondió"))
        return Result.success("Sus compañeros destacan a $nickname por su velocidad y puntualidad.")
    }

    override suspend fun buildTeams(players: List<TeamPlayer>): Result<TeamSplit> {
        delay(latencyMillis)
        if (fail) return Result.failure(IllegalStateException("Gemini no respondió"))
        return Result.success(
            LocalTeamBalancer.split(players).copy(explanation = "Equipos parejos en nivel y con un arquero cada uno.", generatedByAi = true),
        )
    }
}
