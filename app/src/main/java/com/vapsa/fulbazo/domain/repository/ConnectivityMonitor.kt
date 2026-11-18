package com.vapsa.fulbazo.domain.repository

import kotlinx.coroutines.flow.StateFlow
import java.io.IOException

/** Si el teléfono tiene internet en este momento. */
interface ConnectivityMonitor {
    val isOnline: StateFlow<Boolean>
}

/** Una operación no se hizo porque no hay internet (o el servidor no respondió a tiempo). */
class NoConnectionException(message: String = "Sin conexión a internet") : IOException(message)
