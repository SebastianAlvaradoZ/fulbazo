package com.vapsa.fulbazo

import android.app.Application
import com.vapsa.fulbazo.data.repository.DemoInvitationSimulator
import com.vapsa.fulbazo.data.repository.DemoJoinRequestSimulator
import com.vapsa.fulbazo.di.ApplicationScope
import com.vapsa.fulbazo.notifications.InvitationNotifier
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Punto de entrada de la app y raíz del grafo de Hilt. Arranca los trabajos que
 * viven mientras la app está abierta, sin depender de una pantalla.
 */
@HiltAndroidApp
class FulbazoApplication : Application() {

    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope
    @Inject lateinit var invitationNotifier: InvitationNotifier
    @Inject lateinit var demoInvitationSimulator: DemoInvitationSimulator
    @Inject lateinit var demoJoinRequestSimulator: DemoJoinRequestSimulator

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { invitationNotifier.run() }
        // Solo mientras no haya backend: simulan invitaciones y solicitudes de otros jugadores.
        applicationScope.launch { demoInvitationSimulator.run() }
        applicationScope.launch { demoJoinRequestSimulator.run() }
    }
}
