package com.vapsa.fulbazo.data.remote

import android.util.Log
import com.vapsa.fulbazo.domain.repository.ConnectivityMonitor
import com.vapsa.fulbazo.domain.repository.NoConnectionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ejecuta las escrituras en Firestore (unirse, crear partido, calificar…).
 *
 * Sin internet, Firestore guarda la escritura para después y su tarea no termina
 * hasta que vuelve la red: la pantalla se quedaría "cargando" para siempre. Por
 * eso, sin conexión falla al instante con [NoConnectionException] (sin escribir
 * nada), y si el servidor tarda demasiado también se corta.
 */
@Singleton
class FirestoreWriter @Inject constructor(
    private val connectivity: ConnectivityMonitor,
) {

    suspend fun <T> write(tag: String, block: suspend () -> T): Result<T> {
        if (!connectivity.isOnline.value) return Result.failure(NoConnectionException())
        return try {
            Result.success(withTimeout(TIMEOUT_MILLIS) { block() })
        } catch (e: TimeoutCancellationException) {
            Log.w(tag, "Firestore no respondió a tiempo", e)
            Result.failure(NoConnectionException("El servidor no respondió a tiempo"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(tag, "Error de Firestore", e)
            Result.failure(e)
        }
    }

    companion object {
        const val TIMEOUT_MILLIS = 15_000L
    }
}
