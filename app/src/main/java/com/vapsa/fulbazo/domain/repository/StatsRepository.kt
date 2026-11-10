package com.vapsa.fulbazo.domain.repository

import com.vapsa.fulbazo.domain.model.PlayerProfile
import com.vapsa.fulbazo.domain.model.SelfAssessment
import kotlinx.coroutines.flow.Flow

/**
 * Contrato para leer y guardar las estadísticas de los jugadores.
 */
interface StatsRepository {

    /** Emite el perfil de estadísticas del jugador cada vez que cambia. */
    fun observeProfile(userId: String): Flow<PlayerProfile>

    /** Guarda la autoevaluación del jugador ("Cómo me veo"). */
    suspend fun saveSelfAssessment(userId: String, assessment: SelfAssessment): Result<Unit>
}
