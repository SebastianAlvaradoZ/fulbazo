package com.vapsa.fulbazo.domain.repository

import com.vapsa.fulbazo.domain.model.JoinRequest
import kotlinx.coroutines.flow.Flow

interface JoinRequestRepository {

    /** Solicitudes de un partido, de la más antigua a la más nueva. */
    fun observeForMatch(matchId: String): Flow<List<JoinRequest>>

    /** Solicitudes enviadas por el usuario a cualquier partido. */
    fun observeByUser(userId: String): Flow<List<JoinRequest>>

    /** Crea una solicitud pendiente; si ya hay una pendiente, no hace nada. */
    suspend fun sendRequest(matchId: String, userId: String): Result<Unit>

    /** El jugador retira su solicitud pendiente. */
    suspend fun cancelRequest(requestId: String): Result<Unit>

    /** El anfitrión acepta o rechaza la solicitud. */
    suspend fun respond(requestId: String, accept: Boolean): Result<Unit>
}
