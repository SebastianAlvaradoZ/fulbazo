package com.vapsa.fulbazo.ui.match.detail

import com.vapsa.fulbazo.domain.model.PlayerSummary
import com.vapsa.fulbazo.ui.match.MatchItem

/** Relación del usuario actual con el partido; decide qué botón se muestra. */
enum class ViewerRole {
    HOST,
    PLAYER,
    REQUEST_PENDING,
    REQUEST_REJECTED,
    NONE,
}

data class PlayerItem(val player: PlayerSummary, val isHost: Boolean)

data class PendingRequestItem(val requestId: String, val player: PlayerSummary)

enum class MatchDetailError { MATCH_FULL, FAILED }

enum class MatchDetailMessage { REQUEST_SENT, REQUEST_CANCELLED, LEFT_MATCH }

data class MatchDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val loadFailed: Boolean = false,
    val match: MatchItem? = null,
    val players: List<PlayerItem> = emptyList(),
    val role: ViewerRole = ViewerRole.NONE,
    /** El partido ya se jugó: no se puede unir, salir ni aceptar solicitudes. */
    val isFinished: Boolean = false,
    val myPendingRequestId: String? = null,
    /** Solo se llena para el anfitrión. */
    val pendingRequests: List<PendingRequestItem> = emptyList(),
    /** `true` mientras se procesa una acción del propio usuario. */
    val isBusy: Boolean = false,
    val busyRequestIds: Set<String> = emptySet(),
    val error: MatchDetailError? = null,
    val message: MatchDetailMessage? = null,
) {
    val isFull: Boolean get() = match?.spotsLeft == 0
    val playersCount: Int get() = players.size
}
