package com.vapsa.fulbazo.data.repository

import android.util.Log
import com.vapsa.fulbazo.data.local.CourtDao
import com.vapsa.fulbazo.data.remote.CourtApi
import com.vapsa.fulbazo.di.ApplicationScope
import com.vapsa.fulbazo.domain.model.Court
import com.vapsa.fulbazo.domain.repository.CourtRepository
import com.vapsa.fulbazo.domain.repository.CourtSyncStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Losas "offline-first": la app siempre muestra lo que está guardado en Room y,
 * en segundo plano, lo actualiza con la API (Retrofit). Sin conexión se siguen
 * viendo las losas guardadas y [syncStatus] avisa que no están al día.
 */
@Singleton
class OfflineFirstCourtRepository @Inject constructor(
    private val api: CourtApi,
    private val dao: CourtDao,
    @ApplicationScope private val scope: CoroutineScope,
) : CourtRepository {

    private val _syncStatus = MutableStateFlow(CourtSyncStatus.SYNCING)
    override val syncStatus: StateFlow<CourtSyncStatus> = _syncStatus.asStateFlow()

    private val refreshMutex = Mutex()

    /** Se descarga una vez por sesión; después solo al pedir "Reintentar". */
    private val refreshedThisSession = AtomicBoolean(false)

    override fun observeCourts(): Flow<List<Court>> =
        dao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .onStart {
                if (refreshedThisSession.compareAndSet(false, true)) scope.launch { refresh() }
            }

    override suspend fun refresh(): Result<Unit> = refreshMutex.withLock {
        _syncStatus.value = CourtSyncStatus.SYNCING
        try {
            val courts = api.getCourts().mapNotNull { (id, dto) -> dto.toEntity(id) }
            // Una respuesta vacía no borra las losas guardadas.
            check(courts.isNotEmpty()) { "El servidor no tiene losas" }
            dao.replaceAll(courts)
            _syncStatus.value = CourtSyncStatus.UP_TO_DATE
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin red, servidor caído o JSON raro: nunca se cae la app.
            Log.w(TAG, "No se pudieron descargar las losas", e)
            _syncStatus.value = CourtSyncStatus.OFFLINE
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "OfflineFirstCourts"
    }
}
