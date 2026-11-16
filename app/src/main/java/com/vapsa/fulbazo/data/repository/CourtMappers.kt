package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.data.local.CourtEntity
import com.vapsa.fulbazo.data.remote.CourtDto
import com.vapsa.fulbazo.domain.model.Court

/**
 * Losa del servidor → base local. Devuelve `null` si le falta el nombre o las
 * coordenadas: una losa así no se puede mostrar en el mapa.
 */
fun CourtDto.toEntity(id: String): CourtEntity? {
    val name = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val lat = latitude?.takeIf { it in -90.0..90.0 } ?: return null
    val lng = longitude?.takeIf { it in -180.0..180.0 } ?: return null
    return CourtEntity(
        id = id,
        name = name,
        district = district?.trim().orEmpty(),
        latitude = lat,
        longitude = lng,
        address = address?.trim().orEmpty(),
        hasLighting = hasLighting ?: false,
    )
}

fun CourtEntity.toDomain(): Court = Court(
    id = id,
    name = name,
    district = district,
    latitude = latitude,
    longitude = longitude,
    address = address,
    hasLighting = hasLighting,
)
