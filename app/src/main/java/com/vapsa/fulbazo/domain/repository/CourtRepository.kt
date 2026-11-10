package com.vapsa.fulbazo.domain.repository

import com.vapsa.fulbazo.domain.model.Court
import kotlinx.coroutines.flow.Flow

/**
 * Losas deportivas disponibles. Más adelante vendrán de la API propia (JSON)
 * con caché en Room.
 */
interface CourtRepository {
    fun observeCourts(): Flow<List<Court>>
}
