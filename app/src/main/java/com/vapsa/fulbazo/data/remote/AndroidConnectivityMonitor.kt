package com.vapsa.fulbazo.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.vapsa.fulbazo.di.ApplicationScope
import com.vapsa.fulbazo.domain.repository.ConnectivityMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Escucha los cambios de red del sistema (ConnectivityManager) y dice si hay
 * una red con internet. Lo usan el aviso "Sin conexión" y los repositorios para
 * no quedarse esperando una respuesta que no va a llegar.
 */
@Singleton
class AndroidConnectivityMonitor @Inject constructor(
    @ApplicationContext context: Context,
    @ApplicationScope scope: CoroutineScope,
) : ConnectivityMonitor {

    private val manager = context.getSystemService(ConnectivityManager::class.java)

    override val isOnline: StateFlow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(hasInternet())
            }

            override fun onLost(network: Network) {
                trySend(hasInternet())
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(hasInternet())
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        trySend(hasInternet())
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, hasInternet())

    private fun hasInternet(): Boolean {
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
