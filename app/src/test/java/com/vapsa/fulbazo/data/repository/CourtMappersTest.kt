package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.data.remote.CourtDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CourtMappersTest {

    @Test
    fun completeDto_mapsToEntityAndDomain() {
        val entity = CourtDto(" Losa Cayma ", "Cayma", -16.372, -71.545, "Plaza", true).toEntity("losa-cayma")!!

        val court = entity.toDomain()

        assertEquals("losa-cayma", court.id)
        assertEquals("Losa Cayma", court.name)
        assertEquals(-16.372, court.latitude, 0.0)
        assertEquals(true, court.hasLighting)
    }

    @Test
    fun missingOptionalFields_useDefaults() {
        val entity = CourtDto(name = "Losa", latitude = -16.0, longitude = -71.0).toEntity("x")!!

        assertEquals("", entity.district)
        assertEquals("", entity.address)
        assertFalse(entity.hasLighting)
    }

    @Test
    fun courtWithoutNameOrCoordinates_isSkipped() {
        assertNull(CourtDto(name = null, latitude = -16.0, longitude = -71.0).toEntity("a"))
        assertNull(CourtDto(name = "  ", latitude = -16.0, longitude = -71.0).toEntity("b"))
        assertNull(CourtDto(name = "Losa", latitude = null, longitude = -71.0).toEntity("c"))
        assertNull(CourtDto(name = "Losa", latitude = 120.0, longitude = -71.0).toEntity("d"))
    }
}
