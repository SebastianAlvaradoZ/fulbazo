package com.vapsa.fulbazo.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Losa guardada en el teléfono (Room), para usar la app sin conexión. */
@Entity(tableName = "losas")
data class CourtEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "nombre") val name: String,
    @ColumnInfo(name = "distrito") val district: String,
    @ColumnInfo(name = "lat") val latitude: Double,
    @ColumnInfo(name = "lng") val longitude: Double,
    @ColumnInfo(name = "direccion") val address: String,
    @ColumnInfo(name = "iluminacion") val hasLighting: Boolean,
)
