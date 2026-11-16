package com.vapsa.fulbazo.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.vapsa.fulbazo.data.remote.Collections
import com.vapsa.fulbazo.data.remote.firestoreResult
import com.vapsa.fulbazo.data.remote.getEnum
import com.vapsa.fulbazo.data.remote.pairId
import com.vapsa.fulbazo.data.remote.snapshotFlow
import com.vapsa.fulbazo.domain.model.FriendRequest
import com.vapsa.fulbazo.domain.model.FriendRequestStatus
import com.vapsa.fulbazo.domain.repository.FriendRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Amigos en Firestore:
 * - `solicitudesAmistad/{deId}_{paraId}`: solicitud de amistad.
 * - `amistades/{a}_{b}`: la amistad aceptada, con los dos jugadores en `miembros`.
 */
@Singleton
class FirestoreFriendRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : FriendRepository {

    private val requests get() = firestore.collection(Collections.FRIEND_REQUESTS)
    private val friendships get() = firestore.collection(Collections.FRIENDSHIPS)

    override fun observeFriendIds(userId: String): Flow<Set<String>> =
        friendships.whereArrayContains("miembros", userId).snapshotFlow().map { snapshot ->
            snapshot.documents.flatMap { doc ->
                @Suppress("UNCHECKED_CAST")
                (doc.get("miembros") as? List<String>).orEmpty().filter { it != userId }
            }.toSet()
        }

    override fun observePendingRequests(userId: String): Flow<List<FriendRequest>> = combine(
        requests.whereEqualTo("deId", userId).snapshotFlow(),
        requests.whereEqualTo("paraId", userId).snapshotFlow(),
    ) { sent, received ->
        (sent.documents + received.documents)
            .mapNotNull { it.toFriendRequest() }
            .filter { it.status == FriendRequestStatus.PENDING }
    }

    override suspend fun sendRequest(fromUserId: String, toUserId: String): Result<Unit> {
        if (fromUserId == toUserId) return Result.failure(IllegalArgumentException("No puedes agregarte"))
        // Si el otro ya me había enviado una solicitud, la acepto directamente.
        val reverse = firestoreResult(TAG) { requests.document("${toUserId}_$fromUserId").get().await() }
            .getOrElse { return Result.failure(it) }
        if (reverse.exists() && reverse.getString("estado") == FriendRequestStatus.PENDING.name) {
            return respondToRequest(reverse.id, accept = true)
        }
        return firestoreResult(TAG) {
            if (friendships.document(pairId(fromUserId, toUserId)).get().await().exists()) return@firestoreResult
            requests.document("${fromUserId}_$toUserId").set(
                mapOf(
                    "deId" to fromUserId,
                    "paraId" to toUserId,
                    "estado" to FriendRequestStatus.PENDING.name,
                    "creadaEn" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    override suspend fun respondToRequest(requestId: String, accept: Boolean): Result<Unit> = firestoreResult(TAG) {
        val request = requests.document(requestId).get().await().toFriendRequest()
            ?: throw NoSuchElementException("La solicitud no existe")
        val status = if (accept) FriendRequestStatus.ACCEPTED else FriendRequestStatus.REJECTED
        firestore.runBatch { batch ->
            batch.update(requests.document(requestId), "estado", status.name)
            if (accept) {
                batch.set(
                    friendships.document(pairId(request.fromUserId, request.toUserId)),
                    mapOf(
                        "miembros" to listOf(request.fromUserId, request.toUserId),
                        // Quién la pidió y quién la aceptó: las reglas lo revisan.
                        "deId" to request.fromUserId,
                        "paraId" to request.toUserId,
                        "desde" to FieldValue.serverTimestamp(),
                    ),
                )
            }
        }.await()
    }

    override suspend fun removeFriend(userId: String, friendId: String): Result<Unit> = firestoreResult(TAG) {
        firestore.runBatch { batch ->
            batch.delete(friendships.document(pairId(userId, friendId)))
            // Se borran las solicitudes viejas para que puedan volver a ser amigos.
            batch.delete(requests.document("${userId}_$friendId"))
            batch.delete(requests.document("${friendId}_$userId"))
        }.await()
    }

    private companion object {
        const val TAG = "FirestoreFriends"
    }
}

private fun DocumentSnapshot.toFriendRequest(): FriendRequest? {
    if (!exists()) return null
    return FriendRequest(
        id = id,
        fromUserId = getString("deId") ?: return null,
        toUserId = getString("paraId") ?: return null,
        status = getEnum("estado", FriendRequestStatus.PENDING),
    )
}
