package com.vapsa.fulbazo.data.repository

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.vapsa.fulbazo.data.remote.Collections
import com.vapsa.fulbazo.data.remote.FirestoreWriter
import com.vapsa.fulbazo.data.remote.snapshotFlow
import com.vapsa.fulbazo.domain.model.PlayerComment
import com.vapsa.fulbazo.domain.model.ReviewSummary
import com.vapsa.fulbazo.domain.repository.CommentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comentarios en Firestore:
 * - `comentarios/{partidoId}_{autorId}_{jugadorId}`: uno por partido y compañero.
 * - `reportes/{comentarioId}_{quienReporta}`: comentarios marcados como inapropiados.
 * - `resumenesIA/{jugadorId}`: el último resumen de Gemini y qué comentario leyó.
 */
@Singleton
class FirestoreCommentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val writer: FirestoreWriter,
) : CommentRepository {

    private val comments get() = firestore.collection(Collections.COMMENTS)

    override fun observeComments(playerId: String): Flow<List<PlayerComment>> =
        comments.whereEqualTo("jugadorId", playerId).snapshotFlow().map { snapshot ->
            snapshot.documents.mapNotNull { it.toComment() }.sortedByDescending { it.createdAt }
        }

    override fun observeCommentsBy(authorId: String, matchId: String): Flow<List<PlayerComment>> =
        comments.whereEqualTo("autorId", authorId).snapshotFlow().map { snapshot ->
            snapshot.documents.mapNotNull { it.toComment() }.filter { it.matchId == matchId }
        }

    override suspend fun saveComment(
        matchId: String,
        authorId: String,
        playerId: String,
        text: String,
    ): Result<Unit> = writer.write(TAG) {
        val ref = comments.document("${matchId}_${authorId}_$playerId")
        val clean = text.trim().take(PlayerComment.MAX_LENGTH)
        if (clean.isEmpty()) {
            ref.delete().await()
        } else {
            ref.set(
                mapOf(
                    "partidoId" to matchId,
                    "autorId" to authorId,
                    "jugadorId" to playerId,
                    "texto" to clean,
                    "creadoEn" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    override suspend fun reportComment(commentId: String, reporterId: String): Result<Unit> = writer.write(TAG) {
        firestore.collection(Collections.REPORTS).document("${commentId}_$reporterId").set(
            mapOf(
                "comentarioId" to commentId,
                "reportadoPor" to reporterId,
                "creadoEn" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    override suspend fun getSummary(playerId: String): ReviewSummary? = try {
        val doc = firestore.collection(Collections.AI_SUMMARIES).document(playerId).get().await()
        val text = doc.getString("texto")
        val lastCommentId = doc.getString("ultimoComentarioId")
        if (text == null || lastCommentId == null) {
            null
        } else {
            ReviewSummary(playerId, text, lastCommentId, doc.getLong("cantidad")?.toInt() ?: 0)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Sin resumen guardado se genera uno nuevo.
        Log.w(TAG, "No se pudo leer el resumen guardado", e)
        null
    }

    override suspend fun saveSummary(summary: ReviewSummary): Result<Unit> = writer.write(TAG) {
        firestore.collection(Collections.AI_SUMMARIES).document(summary.playerId).set(
            mapOf(
                "texto" to summary.text,
                "ultimoComentarioId" to summary.lastCommentId,
                "cantidad" to summary.commentCount,
                "generadoEn" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    private companion object {
        const val TAG = "FirestoreComments"
    }
}

private fun DocumentSnapshot.toComment(): PlayerComment? {
    return PlayerComment(
        id = id,
        matchId = getString("partidoId") ?: return null,
        authorId = getString("autorId") ?: return null,
        playerId = getString("jugadorId") ?: return null,
        text = getString("texto") ?: return null,
        createdAt = createdAt("creadoEn"),
    )
}
