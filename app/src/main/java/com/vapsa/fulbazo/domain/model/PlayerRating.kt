package com.vapsa.fulbazo.domain.model

/**
 * Calificación que un jugador ([raterId]) le da a un compañero ([ratedId])
 * después de un partido. Solo hay una por evaluador, jugador y partido; al
 * volver a calificar, se reemplaza.
 *
 * Según el [role] se llenan las habilidades de campo o las de arquero.
 */
data class PlayerRating(
    val matchId: String,
    val raterId: String,
    val ratedId: String,
    val role: MatchRole,
    val fieldSkills: Map<FieldSkill, Int> = emptyMap(),
    val goalkeeperSkills: Map<GoalkeeperSkill, Int> = emptyMap(),
) {
    /** Tiene todas las habilidades de su rol, con valores de 1 a 5. */
    val isComplete: Boolean
        get() = when (role) {
            MatchRole.FIELD -> fieldSkills.keys == FieldSkill.entries.toSet() &&
                fieldSkills.values.all { it in SkillScale.MIN..SkillScale.MAX }
            MatchRole.GOALKEEPER -> goalkeeperSkills.keys == GoalkeeperSkill.entries.toSet() &&
                goalkeeperSkills.values.all { it in SkillScale.MIN..SkillScale.MAX }
        }
}
