package com.vapsa.fulbazo.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.vapsa.fulbazo.data.remote.Collections
import com.vapsa.fulbazo.data.remote.firestoreResult
import com.vapsa.fulbazo.data.remote.getEnum
import com.vapsa.fulbazo.data.remote.snapshotFlow
import com.vapsa.fulbazo.data.remote.toLocalDateTime
import com.vapsa.fulbazo.data.remote.toTimestamp
import com.vapsa.fulbazo.domain.model.Match
import com.vapsa.fulbazo.domain.model.MatchLevel
import com.vapsa.fulbazo.domain.model.MatchStatus
import com.vapsa.fulbazo.domain.model.NewMatch
import com.vapsa.fulbazo.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Partidos en `partidos/{id}`. Entrar y salir de un partido se hace con una
 * transacción, para no pasarse de los cupos si dos jugadores entran a la vez.
 */
@Singleton
class FirestoreMatchRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : MatchRepository {

    private val matches get() = firestore.collection(Collections.MATCHES)

    override fun observeMatches(): Flow<List<Match>> =
        matches.snapshotFlow().map { snapshot -> snapshot.documents.mapNotNull { it.toMatch() } }

    override suspend fun createMatch(hostId: String, newMatch: NewMatch): Result<Match> = firestoreResult(TAG) {
        val ref = matches.document()
        val match = Match(
            id = ref.id,
            courtId = newMatch.courtId,
            hostId = hostId,
            dateTime = newMatch.dateTime,
            slots = newMatch.slots,
            level = newMatch.level,
            status = MatchStatus.OPEN,
            playerIds = listOf(hostId),
            description = newMatch.description,
        )
        ref.set(
            mapOf(
                "losaId" to match.courtId,
                "anfitrionId" to match.hostId,
                "fechaHora" to match.dateTime.toTimestamp(),
                "cupos" to match.slots,
                "nivel" to match.level.name,
                "estado" to match.status.name,
                "jugadores" to match.playerIds,
                "descripcion" to match.description,
                "creadoEn" to FieldValue.serverTimestamp(),
            ),
        ).await()
        match
    }

    override suspend fun joinMatch(matchId: String, userId: String): Result<Unit> = firestoreResult(TAG) {
        firestore.runTransaction<Unit> { tx ->
            val ref = matches.document(matchId)
            val match = tx.get(ref).toMatch() ?: throw NoSuchElementException("El partido no existe")
            if (userId in match.playerIds) return@runTransaction
            check(match.spotsLeft > 0) { "El partido está lleno" }
            val players = match.playerIds + userId
            tx.update(
                ref,
                mapOf(
                    "jugadores" to players,
                    "estado" to (if (players.size >= match.slots) MatchStatus.FULL else match.status).name,
                ),
            )
            Unit
        }.await()
    }

    override suspend fun leaveMatch(matchId: String, userId: String): Result<Unit> = firestoreResult(TAG) {
        firestore.runTransaction<Unit> { tx ->
            val ref = matches.document(matchId)
            val match = tx.get(ref).toMatch() ?: throw NoSuchElementException("El partido no existe")
            check(match.hostId != userId) { "El anfitrión no puede salir de su partido" }
            if (userId !in match.playerIds) return@runTransaction
            tx.update(
                ref,
                mapOf(
                    "jugadores" to match.playerIds - userId,
                    // Al liberarse un cupo, un partido lleno vuelve a estar abierto.
                    "estado" to (if (match.status == MatchStatus.FULL) MatchStatus.OPEN else match.status).name,
                ),
            )
            Unit
        }.await()
    }

    private companion object {
        const val TAG = "FirestoreMatchRepository"
    }
}

internal fun DocumentSnapshot.toMatch(): Match? {
    if (!exists()) return null
    @Suppress("UNCHECKED_CAST")
    val players = (get("jugadores") as? List<String>).orEmpty()
    return Match(
        id = id,
        courtId = getString("losaId") ?: return null,
        hostId = getString("anfitrionId") ?: return null,
        dateTime = getTimestamp("fechaHora")?.toLocalDateTime() ?: return null,
        slots = getLong("cupos")?.toInt() ?: return null,
        level = getEnum("nivel", MatchLevel.ANY),
        status = getEnum("estado", MatchStatus.OPEN),
        playerIds = players,
        description = getString("descripcion").orEmpty(),
    )
}

/** Fecha de creación del documento, o "ahora" mientras el servidor no la confirma. */
internal fun DocumentSnapshot.createdAt(field: String) =
    (getTimestamp(field) ?: Timestamp.now()).toLocalDateTime()
