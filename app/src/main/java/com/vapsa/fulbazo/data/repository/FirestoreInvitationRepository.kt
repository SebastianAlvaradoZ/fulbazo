package com.vapsa.fulbazo.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.vapsa.fulbazo.data.remote.Collections
import com.vapsa.fulbazo.data.remote.FirestoreWriter
import com.vapsa.fulbazo.data.remote.getEnum
import com.vapsa.fulbazo.data.remote.snapshotFlow
import com.vapsa.fulbazo.domain.model.InvitationStatus
import com.vapsa.fulbazo.domain.model.MatchInvitation
import com.vapsa.fulbazo.domain.repository.InvitationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Invitaciones en `invitaciones/{partidoId}_{invitadoId}`: una sola por jugador y
 * partido. El invitado las recibe en tiempo real mientras la app está abierta,
 * y [com.vapsa.fulbazo.notifications.InvitationNotifier] muestra la notificación.
 */
@Singleton
class FirestoreInvitationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val writer: FirestoreWriter,
) : InvitationRepository {

    private val invitations get() = firestore.collection(Collections.INVITATIONS)

    override fun observeReceived(userId: String): Flow<List<MatchInvitation>> =
        invitations.whereEqualTo("paraId", userId).snapshotFlow().map { snapshot ->
            snapshot.documents.mapNotNull { it.toInvitation() }.sortedByDescending { it.createdAt }
        }

    override suspend fun sendInvitations(
        matchId: String,
        fromUserId: String,
        toUserIds: Collection<String>,
    ): Result<Unit> = writer.write(TAG) {
        val targets = toUserIds.filter { it != fromUserId }.distinct()
        if (targets.isEmpty()) return@write
        firestore.runTransaction<Unit> { tx ->
            // Primero todas las lecturas (Firestore lo exige), luego las escrituras.
            val refs = targets.map { invitations.document("${matchId}_$it") }
            val pending = refs.filterNot { tx.get(it).exists() }
            pending.forEach { ref ->
                tx.set(
                    ref,
                    mapOf(
                        "partidoId" to matchId,
                        "deId" to fromUserId,
                        "paraId" to ref.id.removePrefix("${matchId}_"),
                        "estado" to InvitationStatus.PENDING.name,
                        "creadaEn" to FieldValue.serverTimestamp(),
                    ),
                )
            }
            Unit
        }.await()
    }

    override suspend fun respond(invitationId: String, accept: Boolean): Result<Unit> = writer.write(TAG) {
        val status = if (accept) InvitationStatus.ACCEPTED else InvitationStatus.DECLINED
        invitations.document(invitationId).update("estado", status.name).await()
    }

    private companion object {
        const val TAG = "FirestoreInvitations"
    }
}

private fun DocumentSnapshot.toInvitation(): MatchInvitation? {
    return MatchInvitation(
        id = id,
        matchId = getString("partidoId") ?: return null,
        fromUserId = getString("deId") ?: return null,
        toUserId = getString("paraId") ?: return null,
        createdAt = createdAt("creadaEn"),
        status = getEnum("estado", InvitationStatus.PENDING),
    )
}
