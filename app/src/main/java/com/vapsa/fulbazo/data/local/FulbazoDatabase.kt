package com.vapsa.fulbazo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/** Base de datos local: hoy guarda las losas para verlas sin conexión. */
@Database(entities = [CourtEntity::class], version = 1, exportSchema = false)
abstract class FulbazoDatabase : RoomDatabase() {
    abstract fun courtDao(): CourtDao

    companion object {
        const val NAME = "fulbazo.db"
    }
}
