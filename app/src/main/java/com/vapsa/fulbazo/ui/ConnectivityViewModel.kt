package com.vapsa.fulbazo.ui

import androidx.lifecycle.ViewModel
import com.vapsa.fulbazo.domain.repository.ConnectivityMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Si hay internet, para el aviso "Sin conexión" que se ve en toda la app. */
@HiltViewModel
class ConnectivityViewModel @Inject constructor(
    connectivity: ConnectivityMonitor,
) : ViewModel() {
    val isOnline: StateFlow<Boolean> = connectivity.isOnline
}
