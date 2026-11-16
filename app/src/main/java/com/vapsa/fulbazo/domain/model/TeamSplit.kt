package com.vapsa.fulbazo.domain.model

/**
 * Lo que se sabe de un jugador para armar equipos: su autoevaluación y lo que
 * opina la comunidad, ya resumidos en números del 1 al 5.
 */
data class TeamPlayer(
    val id: String,
    val nickname: String,
    val mainPosition: Position? = null,
    val isGoalkeeper: Boolean = false,
    /** Nivel de campo según él mismo ("Cómo me veo"), o `null` si no lo llenó. */
    val selfFieldLevel: Double? = null,
    /** Nivel de campo según la comunidad ("Cómo me ven"), o `null` si nadie lo calificó. */
    val communityFieldLevel: Double? = null,
    val communityVotes: Int = 0,
    /** Nivel como arquero (comunidad o, si no hay, el propio), o `null` si no ataja. */
    val goalkeeperLevel: Double? = null,
) {
    /** Nivel para comparar: lo que dice la comunidad pesa más que la autoevaluación. */
    val estimatedLevel: Double get() = communityFieldLevel ?: selfFieldLevel ?: SkillScale.DEFAULT.toDouble()
}

/** Dos equipos con los ids de sus jugadores y por qué quedan parejos. */
data class TeamSplit(
    val teamA: List<String>,
    val teamB: List<String>,
    val explanation: String,
    /** `false` cuando la IA falló y se usó el reparto automático de respaldo. */
    val generatedByAi: Boolean,
)
