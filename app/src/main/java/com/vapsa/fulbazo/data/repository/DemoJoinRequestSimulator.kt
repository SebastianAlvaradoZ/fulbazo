package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.JoinRequestRepository
import com.vapsa.fulbazo.domain.repository.MatchRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Solo para probar sin backend: cuando el usuario crea un partido, unos
 * segundos después dos jugadores de prueba le piden unirse. Así se puede probar
 * aceptar y rechazar solicitudes. Se elimina cuando las solicitudes vengan de Firebase.
 */
class DemoJoinRequestSimulator(
    private val authRepository: AuthRepository,
    private val matchRepository: MatchRepository,
    private val joinRequestRepository: JoinRequestRepository,
    private val delayMillis: Long = 6_000,
) {
    suspend fun run() = coroutineScope {
        val alreadySimulated = mutableSetOf<String>()
        combine(authRepository.currentUser, matchRepository.observeMatches()) { user, matches ->
            if (user == null) emptyList() else matches.filter { it.hostId == user.id }.map { it.id }
        }.collect { hostedMatchIds ->
            hostedMatchIds.filter { alreadySimulated.add(it) }.forEach { matchId ->
                // Se lanza en el scope externo para que no se cancele si la lista cambia.
                this@coroutineScope.launch {
                    delay(delayMillis)
                    SIMULATED_PLAYERS.forEach { joinRequestRepository.sendRequest(matchId, it) }
                }
            }
        }
    }

    private companion object {
        val SIMULATED_PLAYERS = listOf("rodrigo", "luis")
    }
}
