package com.vapsa.fulbazo.domain.repository

import com.vapsa.fulbazo.domain.model.GeoPoint

/** Ubicación del usuario. Solo se consulta si concedió el permiso de ubicación. */
interface LocationRepository {
    /** Ubicación actual (o la última conocida), o `null` si no se pudo obtener. */
    suspend fun currentLocation(): GeoPoint?
}
