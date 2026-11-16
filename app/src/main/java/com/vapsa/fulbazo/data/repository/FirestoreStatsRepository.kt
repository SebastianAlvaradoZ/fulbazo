package com.vapsa.fulbazo.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.vapsa.fulbazo.data.remote.Collections
import com.vapsa.fulbazo.data.remote.firestoreResult
import com.vapsa.fulbazo.data.remote.getEnum
import com.vapsa.fulbazo.data.remote.snapshotFlow
import com.vapsa.fulbazo.domain.model.CommunityStatsCalculator
import com.vapsa.fulbazo.domain.model.FieldSkill
import com.vapsa.fulbazo.domain.model.Foot
import com.vapsa.fulbazo.domain.model.GoalkeeperSkill
import com.vapsa.fulbazo.domain.model.MatchRole
import com.vapsa.fulbazo.domain.model.PlayerProfile
import com.vapsa.fulbazo.domain.model.PlayerRating
import com.vapsa.fulbazo.domain.model.Position
import com.vapsa.fulbazo.domain.model.SelfAssessment
import com.vapsa.fulbazo.domain.repository.RatingRepository
import com.vapsa.fulbazo.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estadísticas en Firestore:
 * - `estadisticas/{uid}`: la autoevaluación del jugador ("Cómo me veo").
 * - `calificaciones/{partidoId}_{evaluadorId}_{evaluadoId}`: lo que opinan sus
 *   compañeros. "Cómo me ven" se calcula con ellas al leer el perfil.
 */
@Singleton
class FirestoreStatsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : StatsRepository, RatingRepository {

    private val stats get() = firestore.collection(Collections.STATS)
    private val ratings get() = firestore.collection(Collections.RATINGS)

    override fun observeProfile(userId: String): Flow<PlayerProfile> = combine(
        stats.document(userId).snapshotFlow(),
        ratings.whereEqualTo("evaluadoId", userId).snapshotFlow(),
    ) { statsDoc, received ->
        PlayerProfile(
            userId = userId,
            selfAssessment = statsDoc.toSelfAssessment(),
            community = CommunityStatsCalculator.fromRatings(received.documents.mapNotNull { it.toRating() }),
        )
    }

    override suspend fun saveSelfAssessment(userId: String, assessment: SelfAssessment): Result<Unit> =
        firestoreResult(TAG) {
            stats.document(userId).set(
                mapOf(
                    "pierna" to assessment.dominantFoot.name,
                    "posicion" to assessment.mainPosition.name,
                    "posicionSecundaria" to assessment.secondaryPosition?.name,
                    "habilidadesCampo" to assessment.fieldSkills.mapKeys { it.key.name },
                    "habilidadesArquero" to assessment.goalkeeperSkills?.mapKeys { it.key.name },
                    "actualizadoEn" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }

    override fun observeRatingsBy(raterId: String, matchId: String): Flow<List<PlayerRating>> =
        ratings.whereEqualTo("evaluadorId", raterId).snapshotFlow().map { snapshot ->
            snapshot.documents.mapNotNull { it.toRating() }.filter { it.matchId == matchId }
        }

    override suspend fun submitRating(rating: PlayerRating): Result<Unit> {
        if (rating.raterId == rating.ratedId) {
            return Result.failure(IllegalArgumentException("No puedes calificarte a ti mismo"))
        }
        if (!rating.isComplete) return Result.failure(IllegalArgumentException("Calificación incompleta"))
        return firestoreResult(TAG) {
            // El id fijo hace que volver a calificar reemplace la calificación anterior.
            ratings.document("${rating.matchId}_${rating.raterId}_${rating.ratedId}").set(
                mapOf(
                    "partidoId" to rating.matchId,
                    "evaluadorId" to rating.raterId,
                    "evaluadoId" to rating.ratedId,
                    "rol" to rating.role.name,
                    "habilidadesCampo" to rating.fieldSkills.mapKeys { it.key.name },
                    "habilidadesArquero" to rating.goalkeeperSkills.mapKeys { it.key.name },
                    "creadaEn" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    private companion object {
        const val TAG = "FirestoreStats"
    }
}

private fun DocumentSnapshot.toSelfAssessment(): SelfAssessment? {
    if (!exists()) return null
    return SelfAssessment(
        dominantFoot = getEnum("pierna", Foot.RIGHT),
        mainPosition = getEnum("posicion", Position.MIDFIELDER),
        secondaryPosition = getString("posicionSecundaria")?.let { name -> Position.entries.firstOrNull { it.name == name } },
        fieldSkills = skills<FieldSkill>("habilidadesCampo") ?: return null,
        goalkeeperSkills = skills<GoalkeeperSkill>("habilidadesArquero"),
    )
}

private fun DocumentSnapshot.toRating(): PlayerRating? {
    return PlayerRating(
        matchId = getString("partidoId") ?: return null,
        raterId = getString("evaluadorId") ?: return null,
        ratedId = getString("evaluadoId") ?: return null,
        role = getEnum("rol", MatchRole.FIELD),
        fieldSkills = skills<FieldSkill>("habilidadesCampo").orEmpty(),
        goalkeeperSkills = skills<GoalkeeperSkill>("habilidadesArquero").orEmpty(),
    )
}

/** Lee un mapa `{"SPEED": 4, ...}`; ignora las habilidades que la app ya no conoce. */
private inline fun <reified S : Enum<S>> DocumentSnapshot.skills(field: String): Map<S, Int>? {
    val raw = get(field) as? Map<*, *> ?: return null
    return raw.entries.mapNotNull { (key, value) ->
        val skill = enumValues<S>().firstOrNull { it.name == key } ?: return@mapNotNull null
        val score = (value as? Number)?.toInt() ?: return@mapNotNull null
        skill to score
    }.toMap()
}
