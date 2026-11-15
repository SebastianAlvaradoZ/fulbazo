package com.vapsa.fulbazo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoPointTest {

    @Test
    fun samePoint_isZeroKm() {
        assertEquals(0.0, GeoPoint.AREQUIPA.distanceKmTo(GeoPoint.AREQUIPA), 1e-9)
    }

    @Test
    fun oneDegreeOfLatitude_isAbout111Km() {
        val distance = GeoPoint(0.0, 0.0).distanceKmTo(GeoPoint(1.0, 0.0))
        assertEquals(111.19, distance, 0.01)
    }

    @Test
    fun distance_isSymmetric() {
        val cayma = GeoPoint(-16.3720, -71.5450)
        val melgar = GeoPoint(-16.4030, -71.5130)
        assertEquals(cayma.distanceKmTo(melgar), melgar.distanceKmTo(cayma), 1e-9)
    }

    @Test
    fun courtsInArequipa_areAFewKmApart() {
        // Plaza de Cayma a la losa de Mariano Melgar: unos 4.8 km en línea recta.
        val distance = GeoPoint(-16.3720, -71.5450).distanceKmTo(GeoPoint(-16.4030, -71.5130))
        assertEquals(4.8, distance, 0.1)
    }

    @Test
    fun arequipaToLima_isAbout765Km() {
        val lima = GeoPoint(-12.0464, -77.0428)
        val distance = GeoPoint.AREQUIPA.distanceKmTo(lima)
        assertTrue("fue $distance", distance in 750.0..780.0)
    }

    @Test
    fun courtLocation_usesItsCoordinates() {
        val court = Court("c", "Losa", "Cayma", -16.37, -71.54)
        assertEquals(GeoPoint(-16.37, -71.54), court.location)
    }
}
