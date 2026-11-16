package com.vapsa.fulbazo.domain.repository

import com.vapsa.fulbazo.domain.model.Court
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Estado de la última descarga de losas desde el servidor. */
enum class CourtSyncStatus {
    /** Descargando la lista. */
    SYNCING,

    /** La lista guardada está al día con el servidor. */
    UP_TO_DATE,

    /** No se pudo descargar (sin conexión o error): se muestran las losas guardadas, si hay. */
    OFFLINE,
}

/**
 * Losas deportivas. Los ViewModels dependen de esta interfaz y no de la
 * implementación (API + base de datos local).
 */
interface CourtRepository {

    /** Losas guardadas en el teléfono; se vuelve a emitir cuando se actualizan. */
    fun observeCourts(): Flow<List<Court>>

    val syncStatus: StateFlow<CourtSyncStatus>

    /** Vuelve a descargar las losas. Si falla, se siguen usando las guardadas. */
    suspend fun refresh(): Result<Unit>
}
