package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.domain.model.CommunityRatings
import com.vapsa.fulbazo.domain.model.CommunityStats
import com.vapsa.fulbazo.domain.model.FieldSkill
import com.vapsa.fulbazo.domain.model.Foot
import com.vapsa.fulbazo.domain.model.PlayerProfile
import com.vapsa.fulbazo.domain.model.Position
import com.vapsa.fulbazo.domain.model.SelfAssessment
import com.vapsa.fulbazo.domain.repository.StatsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Implementación en memoria de [StatsRepository] para desarrollar sin backend.
 * Se reemplazará por Firestore.
 */
class FakeStatsRepository(
    private val latencyMillis: Long = 600,
) : StatsRepository {

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
        ),
    )

    override fun observeProfile(userId: String): Flow<PlayerProfile> =
        profiles.map { it[userId] ?: PlayerProfile(userId) }

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
}
