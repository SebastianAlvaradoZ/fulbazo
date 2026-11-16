package com.vapsa.fulbazo

import com.vapsa.fulbazo.di.ApplicationScope
import com.vapsa.fulbazo.notifications.InvitationNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Trabajos que viven mientras la app está abierta, sin depender de una pantalla.
 * Hoy es el aviso de invitaciones nuevas, que escucha Firestore en tiempo real.
 * Los arranca [FulbazoApplication]; las pruebas instrumentadas los arrancan a mano
 * porque usan la Application de prueba de Hilt.
 */
@Singleton
class AppBackgroundJobs @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val invitationNotifier: InvitationNotifier,
) {
    private val started = AtomicBoolean(false)

    /** Se puede llamar varias veces: solo arranca la primera. */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        applicationScope.launch { invitationNotifier.run() }
    }
}
