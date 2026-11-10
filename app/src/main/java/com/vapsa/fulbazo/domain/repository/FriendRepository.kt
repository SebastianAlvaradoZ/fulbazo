package com.vapsa.fulbazo.domain.repository

import com.vapsa.fulbazo.domain.model.FriendRequest
import kotlinx.coroutines.flow.Flow

interface FriendRepository {

    /** Ids de los amigos del usuario. */
    fun observeFriendIds(userId: String): Flow<Set<String>>

    /** Solicitudes pendientes enviadas o recibidas por el usuario. */
    fun observePendingRequests(userId: String): Flow<List<FriendRequest>>

    suspend fun sendRequest(fromUserId: String, toUserId: String): Result<Unit>

    /** Acepta o rechaza una solicitud recibida. */
    suspend fun respondToRequest(requestId: String, accept: Boolean): Result<Unit>

    suspend fun removeFriend(userId: String, friendId: String): Result<Unit>
}
