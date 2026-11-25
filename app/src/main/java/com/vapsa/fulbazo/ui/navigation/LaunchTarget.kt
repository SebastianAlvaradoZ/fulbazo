package com.vapsa.fulbazo.ui.navigation

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.vapsa.fulbazo.MainActivity

/**
 * Pantallas que se pueden abrir desde fuera de la app (una notificación o el widget).
 * Viajan como extras del Intent de [MainActivity]; si no hay sesión, esperan al Login.
 */
sealed interface LaunchTarget {

    data object Home : LaunchTarget

    data object Invitations : LaunchTarget

    data object CreateMatch : LaunchTarget

    data class MatchDetail(val matchId: String) : LaunchTarget

    companion object {
        private const val EXTRA_SCREEN = "launch_screen"
        private const val EXTRA_MATCH_ID = "launch_match_id"
        internal const val SCREEN_INVITATIONS = "invitations"
        internal const val SCREEN_CREATE_MATCH = "create_match"
        internal const val SCREEN_MATCH = "match"

        /** Los ids de Firestore; otra cosa en el extra se ignora. */
        private val MATCH_ID = Regex("[A-Za-z0-9_-]{1,128}")

        fun intent(context: Context, target: LaunchTarget): Intent {
            val screen = when (target) {
                Home -> null
                Invitations -> SCREEN_INVITATIONS
                CreateMatch -> SCREEN_CREATE_MATCH
                is MatchDetail -> SCREEN_MATCH
            }
            val matchId = (target as? MatchDetail)?.matchId
            return Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                // Distinto por destino: si no, Android reutilizaría el PendingIntent de otro botón.
                data = "fulbazo://abrir/${screen ?: "inicio"}/${matchId.orEmpty()}".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                screen?.let { putExtra(EXTRA_SCREEN, it) }
                matchId?.let { putExtra(EXTRA_MATCH_ID, it) }
            }
        }

        /** Ruta de navegación que pide el Intent, o `null` si solo abre la app. */
        fun routeFrom(intent: Intent?): String? =
            routeFor(intent?.getStringExtra(EXTRA_SCREEN), intent?.getStringExtra(EXTRA_MATCH_ID))

        internal fun routeFor(screen: String?, matchId: String?): String? = when (screen) {
            SCREEN_INVITATIONS -> AppRoutes.INVITATIONS
            SCREEN_CREATE_MATCH -> AppRoutes.createMatch()
            SCREEN_MATCH -> matchId?.takeIf { MATCH_ID.matches(it) }?.let { AppRoutes.matchDetail(it) }
            else -> null
        }
    }
}
