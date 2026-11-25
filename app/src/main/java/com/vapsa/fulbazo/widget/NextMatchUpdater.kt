package com.vapsa.fulbazo.widget

import android.util.Log
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.ConnectivityMonitor
import com.vapsa.fulbazo.domain.repository.CourtRepository
import com.vapsa.fulbazo.domain.repository.InvitationRepository
import com.vapsa.fulbazo.domain.repository.MatchRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mantiene al día el widget "Mi próximo partido":
 * - [run]: en tiempo real mientras la app vive (unirse, salir o crear un partido, una
 *   invitación nueva, entrar o salir de la cuenta).
 * - [refresh]: lo que hace el refresco periódico con la app cerrada.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class NextMatchUpdater(
    private val authRepository: AuthRepository,
    private val matchRepository: MatchRepository,
    private val courtRepository: CourtRepository,
    private val invitationRepository: InvitationRepository,
    private val connectivity: ConnectivityMonitor,
    private val store: NextMatchStore,
    private val now: () -> LocalDateTime,
    private val refreshWindowMillis: Long,
) {

    @Inject constructor(
        authRepository: AuthRepository,
        matchRepository: MatchRepository,
        courtRepository: CourtRepository,
        invitationRepository: InvitationRepository,
        connectivity: ConnectivityMonitor,
        store: NextMatchStore,
    ) : this(
        authRepository = authRepository,
        matchRepository = matchRepository,
        courtRepository = courtRepository,
        invitationRepository = invitationRepository,
        connectivity = connectivity,
        store = store,
        now = { LocalDateTime.now() },
        refreshWindowMillis = REFRESH_WINDOW_MILLIS,
    )

    private val widgetsPlaced = MutableStateFlow(false)

    /** Escucha los cambios solo mientras haya un widget puesto. No termina. */
    suspend fun run() {
        widgetsPlaced.value = store.hasWidgets()
        widgetsPlaced
            .flatMapLatest { placed -> if (placed) statesWithRetry() else emptyFlow() }
            .conflate()
            .collect { save(it) }
    }

    /**
     * Una actualización puntual. Con internet escucha unos segundos, porque lo primero que
     * llega puede ser la copia guardada; sin red solo quita los partidos que ya pasaron.
     *
     * @return `false` si hubo un error y conviene intentarlo más tarde.
     */
    suspend fun refresh(): Boolean {
        widgetsPlaced.value = store.hasWidgets()
        if (!widgetsPlaced.value) return true
        return try {
            // La red puede tardar un momento en llegarle a la app en segundo plano.
            val online = withTimeoutOrNull(ONLINE_WAIT_MILLIS) { connectivity.isOnline.first { it } } ?: false
            if (online) {
                withTimeoutOrNull(refreshWindowMillis) { states().conflate().collect { save(it) } }
            } else {
                store.current()?.let { store.save(it.at(now())) }
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo actualizar el widget", e)
            false
        }
    }

    private fun states(): Flow<NextMatchState> = authRepository.observeSessionUserId().flatMapLatest { userId ->
        if (userId == null) {
            flowOf(NextMatchState.SignedOut)
        } else {
            combine(
                matchRepository.observeMatches(),
                courtRepository.observeCourts(),
                invitationRepository.observeReceived(userId),
            ) { matches, courts, invitations ->
                buildNextMatchState(userId, matches, courts, invitations, now())
            }
        }
    }

    /** Si Firestore corta la escucha, se vuelve a intentar cada vez más espaciado. */
    private fun statesWithRetry(): Flow<NextMatchState> = states().retryWhen { cause, attempt ->
        Log.w(TAG, "Se cortó la escucha del widget", cause)
        delay((RETRY_MILLIS shl attempt.coerceAtMost(5).toInt()))
        true
    }

    /** Sin red los datos son los guardados: se conserva la hora del último dato real. */
    private suspend fun save(state: NextMatchState) {
        val updatedAt = if (connectivity.isOnline.value) now() else store.current()?.updatedAt
        store.save(if (state.signedIn) state.copy(updatedAt = updatedAt) else state)
    }

    private companion object {
        const val TAG = "NextMatchUpdater"
        const val REFRESH_WINDOW_MILLIS = 10_000L
        const val ONLINE_WAIT_MILLIS = 3_000L
        const val RETRY_MILLIS = 10_000L
    }
}
