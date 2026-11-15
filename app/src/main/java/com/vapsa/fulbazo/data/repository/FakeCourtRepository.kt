package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.domain.model.Court
import com.vapsa.fulbazo.domain.repository.CourtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Losas de ejemplo con coordenadas aproximadas en Arequipa.
 * Se reemplazarán por la lista registrada a mano (API propia + Room).
 */
class FakeCourtRepository : CourtRepository {

    override fun observeCourts(): Flow<List<Court>> = flowOf(COURTS)

    companion object {
        val COURTS = listOf(
            Court("losa-umacollo", "Losa Deportiva Umacollo", "Yanahuara", -16.3935, -71.5480, "Av. Ejército, Umacollo", true),
            Court("losa-selva-alegre", "Losa del Parque Selva Alegre", "Arequipa (Cercado)", -16.3880, -71.5310, "Parque Selva Alegre", true),
            Court("losa-cayma", "Losa Deportiva Cayma", "Cayma", -16.3720, -71.5450, "Plaza de Cayma", false),
            Court("losa-zamacola", "Losa Deportiva Zamácola", "Cerro Colorado", -16.3650, -71.5680, "Urb. Zamácola", true),
            Court("losa-melgar", "Losa Deportiva Mariano Melgar", "Mariano Melgar", -16.4030, -71.5130, "Av. Simón Bolívar", false),
            Court("losa-paucarpata", "Losa Deportiva Paucarpata", "Paucarpata", -16.4280, -71.5050, "Plaza de Paucarpata", true),
            Court("losa-bustamante", "Losa Deportiva Bustamante", "José Luis Bustamante y Rivero", -16.4250, -71.5270, "Av. Dolores", true),
            Court("losa-sachaca", "Losa Deportiva Sachaca", "Sachaca", -16.4220, -71.5650, "Plaza de Sachaca", false),
        )
    }
}
