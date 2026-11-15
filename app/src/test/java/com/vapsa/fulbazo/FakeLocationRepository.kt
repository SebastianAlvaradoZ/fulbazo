package com.vapsa.fulbazo

import com.vapsa.fulbazo.domain.model.GeoPoint
import com.vapsa.fulbazo.domain.repository.LocationRepository

/** Ubicación fija para las pruebas; cuenta cuántas veces se pidió. */
class FakeLocationRepository(var location: GeoPoint? = null) : LocationRepository {
    var requests = 0
        private set

    override suspend fun currentLocation(): GeoPoint? {
        requests++
        return location
    }
}
