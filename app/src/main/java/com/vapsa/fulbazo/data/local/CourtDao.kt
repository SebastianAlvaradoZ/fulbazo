package com.vapsa.fulbazo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CourtDao {

    @Query("SELECT * FROM losas ORDER BY nombre")
    abstract fun observeAll(): Flow<List<CourtEntity>>

    /** Cambia todas las losas guardadas por las nuevas, en una sola transacción. */
    @Transaction
    open suspend fun replaceAll(courts: List<CourtEntity>) {
        deleteAll()
        insertAll(courts)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAll(courts: List<CourtEntity>)

    @Query("DELETE FROM losas")
    protected abstract suspend fun deleteAll()
}
