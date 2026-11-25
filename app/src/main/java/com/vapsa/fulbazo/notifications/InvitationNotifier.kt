package com.vapsa.fulbazo.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.InvitationStatus
import com.vapsa.fulbazo.domain.model.MatchInvitation
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.CourtRepository
import com.vapsa.fulbazo.domain.repository.InvitationRepository
import com.vapsa.fulbazo.domain.repository.MatchRepository
import com.vapsa.fulbazo.domain.repository.PlayerRepository
import com.vapsa.fulbazo.ui.match.formatMatchDate
import com.vapsa.fulbazo.ui.navigation.LaunchTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Muestra una notificación del sistema por cada invitación nueva que recibe
 * el usuario con sesión iniciada. Al tocarla se abre la bandeja de invitaciones.
 *
 * Una sola instancia por app ([Singleton]), para recordar qué ya se notificó.
 */
@Singleton
class InvitationNotifier @Inject constructor(
    @ApplicationContext context: Context,
    private val authRepository: AuthRepository,
    private val invitationRepository: InvitationRepository,
    private val matchRepository: MatchRepository,
    private val courtRepository: CourtRepository,
    private val playerRepository: PlayerRepository,
) {
    private val appContext = context.applicationContext

    /** Invitaciones ya notificadas, para no repetirlas (p. ej. al girar la pantalla). */
    private val notifiedIds = Collections.synchronizedSet(mutableSetOf<String>())

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun run() {
        createChannel()
        authRepository.currentUser
            .flatMapLatest { user ->
                if (user == null) flowOf(emptyList()) else invitationRepository.observeReceived(user.id)
            }
            .collect { invitations ->
                invitations
                    .filter { it.status == InvitationStatus.PENDING && notifiedIds.add(it.id) }
                    .forEach { notify(it) }
            }
    }

    private suspend fun notify(invitation: MatchInvitation) {
        if (!canPostNotifications()) return

        val host = playerRepository.observePlayers().first()[invitation.fromUserId]?.nickname
            ?: invitation.fromUserId
        val match = matchRepository.observeMatches().first().firstOrNull { it.id == invitation.matchId }
        val court = match?.let { m -> courtRepository.observeCourts().first().firstOrNull { it.id == m.courtId } }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            invitation.id.hashCode(),
            LaunchTarget.intent(appContext, LaunchTarget.Invitations),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(appContext.getString(R.string.notification_invitation_title, host))
            .setContentText(
                if (match != null && court != null) {
                    appContext.getString(
                        R.string.notification_invitation_text,
                        court.name,
                        match.dateTime.formatMatchDate(),
                    )
                } else {
                    appContext.getString(R.string.invitations_title)
                },
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // El permiso se comprobó en canPostNotifications().
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(appContext).notify(invitation.id.hashCode(), notification)
    }

    private fun canPostNotifications(): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionGranted && NotificationManagerCompat.from(appContext).areNotificationsEnabled()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.notification_channel_invitations),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = appContext.getString(R.string.notification_channel_invitations_description)
        }
        appContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "match_invitations"
    }
}
