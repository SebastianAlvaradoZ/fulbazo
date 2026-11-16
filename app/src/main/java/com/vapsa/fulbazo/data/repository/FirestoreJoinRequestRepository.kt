package com.vapsa.fulbazo.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.vapsa.fulbazo.data.remote.Collections
import com.vapsa.fulbazo.data.remote.firestoreResult
import com.vapsa.fulbazo.data.remote.getEnum
import com.vapsa.fulbazo.data.remote.snapshotFlow
import com.vapsa.fulbazo.domain.model.JoinRequest
import com.vapsa.fulbazo.domain.model.JoinRequestStatus
import com.vapsa.fulbazo.domain.repository.JoinRequestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Solicitudes para unirse en `solicitudes/{partidoId}_{usuarioId}`: hay una sola
 * por jugador y partido. Guarda el id del anfitrión para que las reglas le
 * permitan aceptarla o rechazarla.
 */
@Singleton
class FirestoreJoinRequestRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : JoinRequestRepository {

    private val requests get() = firestore.collection(Collections.JOIN_REQUESTS)

    override fun observeForMatch(matchId: String): Flow<List<JoinRequest>> =
        requests.whereEqualTo("partidoId", matchId).snapshotFlow().map { snapshot ->
            snapshot.documents.mapNotNull { it.toJoinRequest() }.sortedBy { it.createdAt }
        }

    override fun observeByUser(userId: String): Flow<List<JoinRequest>> =
        requests.whereEqualTo("usuarioId", userId).snapshotFlow().map { snapshot ->
            snapshot.documents.mapNotNull { it.toJoinRequest() }.sortedByDescending { it.createdAt }
        }

    override suspend fun sendRequest(matchId: String, userId: String): Result<Unit> = firestoreResult(TAG) {
        val hostId = firestore.collection(Collections.MATCHES).document(matchId).get().await()
            .getString("anfitrionId") ?: throw NoSuchElementException("El partido no existe")
        firestore.runTransaction<Unit> { tx ->
            val ref = requests.document("${matchId}_$userId")
            val current = tx.get(ref)
            // Si ya hay una pendiente no se hace nada; una rechazada se puede volver a enviar.
            if (current.exists() && current.getString("estado") == JoinRequestStatus.PENDING.name) {
                return@runTransaction
            }
            tx.set(
                ref,
                mapOf(
                    "partidoId" to matchId,
                    "usuarioId" to userId,
                    "anfitrionId" to hostId,
                    "estado" to JoinRequestStatus.PENDING.name,
                    "creadaEn" to FieldValue.serverTimestamp(),
                ),
            )
            Unit
        }.await()
    }

    override suspend fun cancelRequest(requestId: String): Result<Unit> = firestoreResult(TAG) {
        firestore.runTransaction<Unit> { tx ->
            val ref = requests.document(requestId)
            check(tx.get(ref).getString("estado") == JoinRequestStatus.PENDING.name) {
                "La solicitud ya no está pendiente"
            }
            tx.delete(ref)
            Unit
        }.await()
    }

    override suspend fun respond(requestId: String, accept: Boolean): Result<Unit> = firestoreResult(TAG) {
        firestore.runTransaction<Unit> { tx ->
            val ref = requests.document(requestId)
            check(tx.get(ref).getString("estado") == JoinRequestStatus.PENDING.name) {
                "La solicitud ya no está pendiente"
            }
            val status = if (accept) JoinRequestStatus.ACCEPTED else JoinRequestStatus.REJECTED
            tx.update(ref, "estado", status.name)
            Unit
        }.await()
    }

    private companion object {
        const val TAG = "FirestoreJoinRequests"
    }
}

private fun DocumentSnapshot.toJoinRequest(): JoinRequest? {
    return JoinRequest(
        id = id,
        matchId = getString("partidoId") ?: return null,
        userId = getString("usuarioId") ?: return null,
        createdAt = createdAt("creadaEn"),
        status = getEnum("estado", JoinRequestStatus.PENDING),
    )
}
