package com.vapsa.fulbazo.data.repository

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.vapsa.fulbazo.domain.model.GeoPoint
import com.vapsa.fulbazo.domain.repository.LocationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Ubicación con el proveedor combinado de Google Play services. La precisión
 * "balanceada" (~100 m) basta para ordenar losas y gasta menos batería.
 */
class FusedLocationRepository @Inject constructor(
    @ApplicationContext context: Context,
) : LocationRepository {

    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    // Quien llama ya verificó el permiso; si lo revocaron, se captura la SecurityException.
    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): GeoPoint? = try {
        val cancellation = CancellationTokenSource()
        val current = withTimeoutOrNull(TIMEOUT_MILLIS) {
            try {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token).await()
            } finally {
                cancellation.cancel()
            }
        }
        (current ?: client.lastLocation.await())?.let { GeoPoint(it.latitude, it.longitude) }
    } catch (e: CancellationException) {
        throw e
    } catch (ignored: Exception) {
        // Sin permiso, sin Play services o con la ubicación apagada: seguimos sin ubicación.
        null
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
