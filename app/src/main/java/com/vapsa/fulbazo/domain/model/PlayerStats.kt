package com.vapsa.fulbazo.domain.model

/** Escala de todas las habilidades: de 1 a 5. */
object SkillScale {
    const val MIN = 1
    const val MAX = 5
    const val DEFAULT = 3

    fun clamp(value: Int): Int = value.coerceIn(MIN, MAX)
}

/** Pierna buena. La pierna mala se deduce de ella. */
enum class Foot {
    RIGHT,
    LEFT,
    BOTH;

    /** Pierna mala, o `null` si el jugador es ambidiestro. */
    val weakFoot: Foot?
        get() = when (this) {
            RIGHT -> LEFT
            LEFT -> RIGHT
            BOTH -> null
        }
}

/** Posición en la cancha. Un arquero que sale a jugar se considera Defensa. */
enum class Position {
    GOALKEEPER,
    DEFENDER,
    MIDFIELDER,
    FORWARD,
}

/** Rol que tuvo un jugador en un partido, para las evaluaciones de la comunidad. */
enum class MatchRole {
    FIELD,
    GOALKEEPER,
}

/** Habilidades que se califican a un jugador de campo. */
enum class FieldSkill {
    SPEED,
    TECHNIQUE,
    PASSING,
    SHOOTING,
    DEFENDING,
    STAMINA,
    TEAMWORK,
}

/** Habilidades que se califican a un arquero. */
enum class GoalkeeperSkill {
    SAVES,
    REFLEXES,
    RUSHING_OUT,
    FOOTWORK,
    TEAMWORK,
}

/**
 * Lo que el jugador dice de sí mismo ("Cómo me veo").
 * Las habilidades de arquero solo existen si su posición principal o
 * secundaria es Arquero.
 */
data class SelfAssessment(
    val dominantFoot: Foot,
    val mainPosition: Position,
    val secondaryPosition: Position? = null,
    val fieldSkills: Map<FieldSkill, Int>,
    val goalkeeperSkills: Map<GoalkeeperSkill, Int>? = null,
) {
    val isGoalkeeper: Boolean
        get() = mainPosition == Position.GOALKEEPER || secondaryPosition == Position.GOALKEEPER

    /** Nivel general como jugador de campo: promedio de sus habilidades. */
    val fieldOverall: Double get() = fieldSkills.values.averageOrZero()

    /** Nivel general como arquero, o `null` si no ataja. */
    val goalkeeperOverall: Double? get() = goalkeeperSkills?.values?.averageOrZero()
}

/**
 * Promedio de las calificaciones que otros jugadores le dieron en un rol,
 * junto con cuántas personas lo calificaron.
 */
data class CommunityRatings<S : Enum<S>>(
    val averages: Map<S, Double>,
    val votes: Int,
) {
    val overall: Double get() = averages.values.averageOrZero()
}

/** Lo que dicen los demás ("Cómo me ven"), separado por rol. */
data class CommunityStats(
    val asFieldPlayer: CommunityRatings<FieldSkill>? = null,
    val asGoalkeeper: CommunityRatings<GoalkeeperSkill>? = null,
)

/** Estadísticas de un jugador: su autoevaluación y lo que opina la comunidad. */
data class PlayerProfile(
    val userId: String,
    val selfAssessment: SelfAssessment? = null,
    val community: CommunityStats = CommunityStats(),
)

private fun Collection<Number>.averageOrZero(): Double =
    if (isEmpty()) 0.0 else sumOf { it.toDouble() } / size
