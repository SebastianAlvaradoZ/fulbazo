package com.vapsa.fulbazo.domain.model

/**
 * Calcula "Cómo me ven" a partir de las calificaciones que recibió un jugador:
 * el promedio de cada habilidad, separado por rol (campo o arquero), y cuántas
 * calificaciones hubo en cada rol.
 */
object CommunityStatsCalculator {

    fun fromRatings(received: List<PlayerRating>): CommunityStats {
        val asField = received.filter { it.role == MatchRole.FIELD && it.isComplete }
        val asGoalkeeper = received.filter { it.role == MatchRole.GOALKEEPER && it.isComplete }
        return CommunityStats(
            asFieldPlayer = asField.takeIf { it.isNotEmpty() }?.let { ratings ->
                CommunityRatings(averages(ratings.map { it.fieldSkills }), votes = ratings.size)
            },
            asGoalkeeper = asGoalkeeper.takeIf { it.isNotEmpty() }?.let { ratings ->
                CommunityRatings(averages(ratings.map { it.goalkeeperSkills }), votes = ratings.size)
            },
        )
    }

    private fun <S : Enum<S>> averages(skills: List<Map<S, Int>>): Map<S, Double> =
        skills.flatMap { it.entries }
            .groupBy({ it.key }, { it.value })
            .mapValues { (_, values) -> values.average() }
}
