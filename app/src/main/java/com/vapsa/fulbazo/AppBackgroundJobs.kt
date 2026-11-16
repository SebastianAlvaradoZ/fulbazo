package com.vapsa.fulbazo

import com.vapsa.fulbazo.data.repository.DemoInvitationSimulator
import com.vapsa.fulbazo.data.repository.DemoJoinRequestSimulator
import com.vapsa.fulbazo.di.ApplicationScope
import com.vapsa.fulbazo.notifications.InvitationNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Trabajos que viven mientras la app está abierta, sin depender de una pantalla.
 * Los arranca [FulbazoApplication]; las pruebas instrumentadas los arrancan a mano
 * porque usan la Application de prueba de Hilt.
 */
@Singleton
class AppBackgroundJobs @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val invitationNotifier: InvitationNotifier,
    private val demoInvitationSimulator: DemoInvitationSimulator,
    private val demoJoinRequestSimulator: DemoJoinRequestSimulator,
) {
    private val started = AtomicBoolean(false)

    /** Se puede llamar varias veces: solo arranca la primera. */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        applicationScope.launch { invitationNotifier.run() }
        // Solo mientras no haya backend: simulan invitaciones y solicitudes de otros jugadores.
        applicationScope.launch { demoInvitationSimulator.run() }
        applicationScope.launch { demoJoinRequestSimulator.run() }
    }
}
