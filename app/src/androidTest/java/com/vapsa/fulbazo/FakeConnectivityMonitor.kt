package com.vapsa.fulbazo

import com.vapsa.fulbazo.domain.repository.ConnectivityMonitor
import kotlinx.coroutines.flow.MutableStateFlow

/** Red falsa para las pruebas: se "desconecta" cambiando [isOnline]. */
class FakeConnectivityMonitor : ConnectivityMonitor {
    override val isOnline = MutableStateFlow(true)
}
