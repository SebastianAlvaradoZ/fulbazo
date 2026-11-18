package com.vapsa.fulbazo.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.vapsa.fulbazo.data.remote.Collections
import com.vapsa.fulbazo.data.remote.FirestoreWriter
import com.vapsa.fulbazo.data.remote.snapshotFlow
import com.vapsa.fulbazo.domain.model.FriendGroup
import com.vapsa.fulbazo.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Grupos de amigos en `grupos/{id}`; solo los ve y edita su dueño. */
@Singleton
class FirestoreGroupRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val writer: FirestoreWriter,
) : GroupRepository {

    private val groups get() = firestore.collection(Collections.GROUPS)

    override fun observeGroups(ownerId: String): Flow<List<FriendGroup>> =
        groups.whereEqualTo("duenoId", ownerId).snapshotFlow().map { snapshot ->
            snapshot.documents.mapNotNull { it.toGroup() }.sortedBy { it.name.lowercase() }
        }

    override suspend fun createGroup(
        ownerId: String,
        name: String,
        memberIds: List<String>,
    ): Result<FriendGroup> = writer.write(TAG) {
        val ref = groups.document()
        val group = FriendGroup(ref.id, ownerId, name.trim(), memberIds.distinct())
        ref.set(mapOf("duenoId" to ownerId, "nombre" to group.name, "miembros" to group.memberIds)).await()
        group
    }

    override suspend fun updateGroup(groupId: String, name: String, memberIds: List<String>): Result<Unit> =
        writer.write(TAG) {
            groups.document(groupId).update(mapOf("nombre" to name.trim(), "miembros" to memberIds.distinct())).await()
        }

    override suspend fun deleteGroup(groupId: String): Result<Unit> = writer.write(TAG) {
        groups.document(groupId).delete().await()
    }

    private companion object {
        const val TAG = "FirestoreGroups"
    }
}

private fun DocumentSnapshot.toGroup(): FriendGroup? {
    @Suppress("UNCHECKED_CAST")
    val members = (get("miembros") as? List<String>).orEmpty()
    return FriendGroup(
        id = id,
        ownerId = getString("duenoId") ?: return null,
        name = getString("nombre") ?: return null,
        memberIds = members,
    )
}
