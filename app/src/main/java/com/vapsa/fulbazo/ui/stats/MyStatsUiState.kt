package com.vapsa.fulbazo.ui.stats

import com.vapsa.fulbazo.domain.model.FieldSkill
import com.vapsa.fulbazo.domain.model.Foot
import com.vapsa.fulbazo.domain.model.GoalkeeperSkill
import com.vapsa.fulbazo.domain.model.Position
import com.vapsa.fulbazo.domain.model.SkillScale

/**
 * Estado de la pantalla "Mis estadísticas".
 */
data class MyStatsUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val dominantFoot: Foot? = null,
    val mainPosition: Position? = null,
    val secondaryPosition: Position? = null,
    val fieldSkills: Map<FieldSkill, Int> = FieldSkill.entries.associateWith { SkillScale.DEFAULT },
    val goalkeeperSkills: Map<GoalkeeperSkill, Int> =
        GoalkeeperSkill.entries.associateWith { SkillScale.DEFAULT },
    val footMissing: Boolean = false,
    val positionMissing: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val isSaved: Boolean = false,
) {
    /** Las habilidades de arquero solo se piden si ataja. */
    val showGoalkeeperSkills: Boolean
        get() = mainPosition == Position.GOALKEEPER || secondaryPosition == Position.GOALKEEPER

    val fieldOverall: Double get() = fieldSkills.values.average()

    val goalkeeperOverall: Double get() = goalkeeperSkills.values.average()

    /** Posiciones que se pueden elegir como secundarias (todas menos la principal). */
    val secondaryOptions: List<Position>
        get() = Position.entries.filter { it != mainPosition }
}
