package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.InvitationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy

/**
 * Solo para probar sin backend: unos segundos después de iniciar sesión,
 * el jugador de prueba "carlos" invita al usuario a su partido.
 * Se elimina cuando las invitaciones vengan de Firebase.
 */
class DemoInvitationSimulator(
    private val authRepository: AuthRepository,
    private val invitationRepository: InvitationRepository,
    private val delayMillis: Long = 8_000,
) {
    suspend fun run() {
        authRepository.currentUser
            .distinctUntilChangedBy { it?.id }
            .collectLatest { user ->
                if (user == null || user.id == SIMULATED_HOST_ID) return@collectLatest
                delay(delayMillis)
                invitationRepository.sendInvitations(
                    matchId = FakeMatchRepository.SEED_MATCH_ID,
                    fromUserId = SIMULATED_HOST_ID,
                    toUserIds = listOf(user.id),
                )
            }
    }

    private companion object {
        const val SIMULATED_HOST_ID = "carlos"
    }
}
