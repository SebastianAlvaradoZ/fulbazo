package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.domain.model.CommunityRatings
import com.vapsa.fulbazo.domain.model.CommunityStats
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Implementación en memoria de [StatsRepository] y [RatingRepository] para
 * desarrollar sin backend.
 * Se reemplazará por Firestore.
 */
class FakeStatsRepository(
    private val latencyMillis: Long = 600,
) : StatsRepository, RatingRepository {

    private val profiles = MutableStateFlow(
        mapOf(
            // El usuario de prueba de FakeAuthRepository ya tiene estadísticas
            // y algunas calificaciones de la comunidad.
            "demo" to PlayerProfile(
                userId = "demo",
                selfAssessment = SelfAssessment(
                    dominantFoot = Foot.RIGHT,
                    mainPosition = Position.MIDFIELDER,
                    secondaryPosition = Position.FORWARD,
                    fieldSkills = mapOf(
                        FieldSkill.SPEED to 4,
                        FieldSkill.TECHNIQUE to 4,
                        FieldSkill.PASSING to 5,
                        FieldSkill.SHOOTING to 3,
                        FieldSkill.DEFENDING to 2,
                        FieldSkill.STAMINA to 3,
                        FieldSkill.TEAMWORK to 5,
                    ),
                ),
                community = CommunityStats(
                    asFieldPlayer = CommunityRatings(
                        averages = mapOf(
                            FieldSkill.SPEED to 3.5,
                            FieldSkill.TECHNIQUE to 4.0,
                            FieldSkill.PASSING to 4.2,
                            FieldSkill.SHOOTING to 2.8,
                            FieldSkill.DEFENDING to 2.5,
                            FieldSkill.STAMINA to 3.0,
                            FieldSkill.TEAMWORK to 4.6,
                        ),
                        votes = 6,
                    ),
                ),
            ),
            // Otros jugadores de prueba, para ver sus perfiles.
            "carlos" to PlayerProfile(
                userId = "carlos",
                selfAssessment = SelfAssessment(
                    dominantFoot = Foot.LEFT,
                    mainPosition = Position.FORWARD,
                    fieldSkills = fieldSkills(speed = 5, technique = 4, passing = 3, shooting = 5, defending = 2, stamina = 4, teamwork = 3),
                ),
                community = CommunityStats(
                    asFieldPlayer = CommunityRatings(
                        averages = fieldAverages(4.5, 4.1, 3.2, 4.4, 2.1, 3.8, 3.5),
                        votes = 8,
                    ),
                ),
            ),
            "maria" to PlayerProfile(
                userId = "maria",
                selfAssessment = SelfAssessment(
                    dominantFoot = Foot.RIGHT,
                    mainPosition = Position.DEFENDER,
                    secondaryPosition = Position.GOALKEEPER,
                    fieldSkills = fieldSkills(speed = 3, technique = 3, passing = 4, shooting = 2, defending = 5, stamina = 4, teamwork = 5),
                    goalkeeperSkills = GoalkeeperSkill.entries.associateWith { 4 },
                ),
                community = CommunityStats(
                    asFieldPlayer = CommunityRatings(
                        averages = fieldAverages(3.0, 3.2, 3.9, 2.4, 4.6, 3.7, 4.8),
                        votes = 4,
                    ),
                    asGoalkeeper = CommunityRatings(
                        averages = GoalkeeperSkill.entries.associateWith { 3.5 },
                        votes = 2,
                    ),
                ),
            ),
            "jose" to PlayerProfile(
                userId = "jose",
                selfAssessment = SelfAssessment(
                    dominantFoot = Foot.BOTH,
                    mainPosition = Position.MIDFIELDER,
                    fieldSkills = fieldSkills(speed = 4, technique = 5, passing = 5, shooting = 4, defending = 3, stamina = 3, teamwork = 4),
                ),
            ),
            "luis" to PlayerProfile(
                userId = "luis",
                selfAssessment = SelfAssessment(
                    dominantFoot = Foot.RIGHT,
                    mainPosition = Position.GOALKEEPER,
                    fieldSkills = fieldSkills(speed = 2, technique = 2, passing = 3, shooting = 2, defending = 3, stamina = 3, teamwork = 4),
                    goalkeeperSkills = GoalkeeperSkill.entries.associateWith { 5 },
                ),
                community = CommunityStats(
                    asGoalkeeper = CommunityRatings(
                        averages = GoalkeeperSkill.entries.associateWith { 4.3 },
                        votes = 3,
                    ),
                ),
            ),
        ),
    )

    /** Calificaciones hechas en la app; se suman a las estadísticas iniciales de prueba. */
    private val ratings = MutableStateFlow(emptyList<PlayerRating>())

    override fun observeProfile(userId: String): Flow<PlayerProfile> =
        combine(profiles, ratings) { all, allRatings ->
            val base = all[userId] ?: PlayerProfile(userId)
            val received = allRatings.filter { it.ratedId == userId }
            base.copy(community = base.community.withRatings(received))
        }

    override suspend fun saveSelfAssessment(
        userId: String,
        assessment: SelfAssessment,
    ): Result<Unit> {
        delay(latencyMillis)
        profiles.update { current ->
            val profile = current[userId] ?: PlayerProfile(userId)
            current + (userId to profile.copy(selfAssessment = assessment))
        }
        return Result.success(Unit)
    }

    override fun observeRatingsBy(raterId: String, matchId: String): Flow<List<PlayerRating>> =
        ratings.map { list -> list.filter { it.raterId == raterId && it.matchId == matchId } }

    override suspend fun submitRating(rating: PlayerRating): Result<Unit> {
        delay(latencyMillis)
        if (rating.raterId == rating.ratedId) {
            return Result.failure(IllegalArgumentException("No puedes calificarte a ti mismo"))
        }
        if (!rating.isComplete) return Result.failure(IllegalArgumentException("Calificación incompleta"))
        ratings.update { list ->
            // Una sola calificación por evaluador, jugador y partido: la nueva reemplaza a la anterior.
            list.filterNot {
                it.matchId == rating.matchId && it.raterId == rating.raterId && it.ratedId == rating.ratedId
            } + rating
        }
        return Result.success(Unit)
    }

    private companion object {
        /**
         * Suma las calificaciones recibidas a los promedios existentes,
         * por separado para campo y para arquero.
         */
        fun CommunityStats.withRatings(received: List<PlayerRating>): CommunityStats = CommunityStats(
            asFieldPlayer = asFieldPlayer.merge(
                received.filter { it.role == MatchRole.FIELD }.map { it.fieldSkills },
            ),
            asGoalkeeper = asGoalkeeper.merge(
                received.filter { it.role == MatchRole.GOALKEEPER }.map { it.goalkeeperSkills },
            ),
        )

        fun <S : Enum<S>> CommunityRatings<S>?.merge(newScores: List<Map<S, Int>>): CommunityRatings<S>? {
            if (newScores.isEmpty()) return this
            val previousVotes = this?.votes ?: 0
            val totalVotes = previousVotes + newScores.size
            val skills = (this?.averages?.keys.orEmpty() + newScores.flatMap { it.keys }).toSet()
            val averages = skills.associateWith { skill ->
                val previousSum = (this?.averages?.get(skill) ?: 0.0) * previousVotes
                (previousSum + newScores.sumOf { it[skill] ?: 0 }) / totalVotes
            }
            return CommunityRatings(averages, totalVotes)
        }

        fun fieldSkills(
            speed: Int,
            technique: Int,
            passing: Int,
            shooting: Int,
            defending: Int,
            stamina: Int,
            teamwork: Int,
        ): Map<FieldSkill, Int> = mapOf(
            FieldSkill.SPEED to speed,
            FieldSkill.TECHNIQUE to technique,
            FieldSkill.PASSING to passing,
            FieldSkill.SHOOTING to shooting,
            FieldSkill.DEFENDING to defending,
            FieldSkill.STAMINA to stamina,
            FieldSkill.TEAMWORK to teamwork,
        )

        /** Promedios en el orden de [FieldSkill]. */
        fun fieldAverages(vararg values: Double): Map<FieldSkill, Double> =
            FieldSkill.entries.zip(values.toList()).toMap()
    }
}
