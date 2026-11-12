package com.vapsa.fulbazo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.vapsa.fulbazo.di.AppContainer
import com.vapsa.fulbazo.notifications.InvitationNotifier
import com.vapsa.fulbazo.ui.FulbazoApp
import com.vapsa.fulbazo.ui.theme.FulbazoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** Se pone en `true` cuando la app se abre desde una notificación de invitación. */
    private var openInvitations by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openInvitations = intent.wantsInvitations()

        lifecycleScope.launch { InvitationNotifier.get(this@MainActivity).run() }
        // Solo mientras no haya backend: simulan invitaciones y solicitudes de otros jugadores.
        lifecycleScope.launch { AppContainer.demoInvitationSimulator.run() }
        lifecycleScope.launch { AppContainer.demoJoinRequestSimulator.run() }

        setContent {
            FulbazoTheme {
                FulbazoApp(
                    openInvitationsRequested = openInvitations,
                    onInvitationsOpened = { openInvitations = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.wantsInvitations()) openInvitations = true
    }

    private fun Intent?.wantsInvitations(): Boolean =
        this?.getBooleanExtra(InvitationNotifier.EXTRA_OPEN_INVITATIONS, false) == true
}
