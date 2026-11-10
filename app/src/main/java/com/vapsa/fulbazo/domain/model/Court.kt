package com.vapsa.fulbazo.domain.model

/**
 * Losa deportiva donde se pueden jugar partidos.
 */
data class Court(
    val id: String,
    val name: String,
    val district: String,
    val latitude: Double,
    val longitude: Double,
)
