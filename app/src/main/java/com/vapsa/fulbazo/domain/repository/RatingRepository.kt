package com.vapsa.fulbazo.domain.repository

import com.vapsa.fulbazo.domain.model.PlayerRating
import kotlinx.coroutines.flow.Flow

/**
 * Calificaciones entre jugadores después de un partido. Alimentan las
 * estadísticas "Cómo lo ven" de cada jugador.
 */
interface RatingRepository {

    /** Calificaciones que [raterId] ya dio en el partido [matchId]. */
    fun observeRatingsBy(raterId: String, matchId: String): Flow<List<PlayerRating>>

    /**
     * Guarda la calificación; si el evaluador ya había calificado a ese
     * jugador en ese partido, la reemplaza. Falla si está incompleta o si se
     * califica a sí mismo.
     */
    suspend fun submitRating(rating: PlayerRating): Result<Unit>
}
