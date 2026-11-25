package com.vapsa.fulbazo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vapsa.fulbazo.ui.FulbazoApp
import com.vapsa.fulbazo.ui.navigation.LaunchTarget
import com.vapsa.fulbazo.ui.theme.FulbazoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Pantalla que pidió la notificación o el widget con el que se abrió la app. */
    private var requestedRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Al girar la pantalla o volver desde Recientes, Android entrega otra vez el mismo
        // Intent: ahí ya no se navega de nuevo.
        val fromHistory = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (savedInstanceState == null && !fromHistory) requestedRoute = LaunchTarget.routeFrom(intent)

        setContent {
            FulbazoTheme {
                FulbazoApp(
                    requestedRoute = requestedRoute,
                    onRouteOpened = { requestedRoute = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        LaunchTarget.routeFrom(intent)?.let { requestedRoute = it }
    }
}
