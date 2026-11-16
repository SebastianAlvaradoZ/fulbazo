package com.vapsa.fulbazo.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

/**
 * API de losas: el nodo `/losas` de Firebase Realtime Database, leído por REST.
 * Responde un objeto con cada losa bajo su id:
 * `{"losa-cayma": {"nombre": "...", "distrito": "...", ...}, ...}`.
 */
interface CourtApi {

    /**
     * Si el nodo está vacío, Firebase responde `null` y la conversión falla: el
     * repositorio lo trata como error y conserva las losas guardadas.
     */
    @GET("losas.json")
    suspend fun getCourts(): Map<String, CourtDto>
}

/** Losa tal como llega del servidor. */
@Serializable
data class CourtDto(
    @SerialName("nombre") val name: String? = null,
    @SerialName("distrito") val district: String? = null,
    @SerialName("lat") val latitude: Double? = null,
    @SerialName("lng") val longitude: Double? = null,
    @SerialName("direccion") val address: String? = null,
    @SerialName("iluminacion") val hasLighting: Boolean? = null,
)
