package com.vapsa.fulbazo.domain.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Un punto en el mapa, en grados decimales. */
data class GeoPoint(val latitude: Double, val longitude: Double) {

    /**
     * Distancia en línea recta hasta [other], en kilómetros (fórmula del
     * haversine). Basta para ordenar losas de una misma ciudad.
     */
    fun distanceKmTo(other: GeoPoint): Double {
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLng = Math.toRadians(other.longitude - longitude)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(other.latitude)) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    companion object {
        const val EARTH_RADIUS_KM = 6_371.0

        /** Plaza de Armas de Arequipa: centro del mapa cuando no conocemos tu ubicación. */
        val AREQUIPA = GeoPoint(-16.3988, -71.5369)
    }
}

/** Ubicación de la losa en el mapa. */
val Court.location: GeoPoint get() = GeoPoint(latitude, longitude)
