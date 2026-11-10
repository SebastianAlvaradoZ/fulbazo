package com.vapsa.fulbazo.ui.stats

import androidx.annotation.StringRes
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.FieldSkill
import com.vapsa.fulbazo.domain.model.Foot
import com.vapsa.fulbazo.domain.model.GoalkeeperSkill
import com.vapsa.fulbazo.domain.model.Position

/**
 * Textos de `strings.xml` para los valores de las estadísticas.
 */
@get:StringRes
val Foot.labelRes: Int
    get() = when (this) {
        Foot.RIGHT -> R.string.foot_right
        Foot.LEFT -> R.string.foot_left
        Foot.BOTH -> R.string.foot_both
    }

@get:StringRes
val Position.labelRes: Int
    get() = when (this) {
        Position.GOALKEEPER -> R.string.position_goalkeeper
        Position.DEFENDER -> R.string.position_defender
        Position.MIDFIELDER -> R.string.position_midfielder
        Position.FORWARD -> R.string.position_forward
    }

@get:StringRes
val FieldSkill.labelRes: Int
    get() = when (this) {
        FieldSkill.SPEED -> R.string.skill_speed
        FieldSkill.TECHNIQUE -> R.string.skill_technique
        FieldSkill.PASSING -> R.string.skill_passing
        FieldSkill.SHOOTING -> R.string.skill_shooting
        FieldSkill.DEFENDING -> R.string.skill_defending
        FieldSkill.STAMINA -> R.string.skill_stamina
        FieldSkill.TEAMWORK -> R.string.skill_teamwork
    }

@get:StringRes
val GoalkeeperSkill.labelRes: Int
    get() = when (this) {
        GoalkeeperSkill.SAVES -> R.string.skill_saves
        GoalkeeperSkill.REFLEXES -> R.string.skill_reflexes
        GoalkeeperSkill.RUSHING_OUT -> R.string.skill_rushing_out
        GoalkeeperSkill.FOOTWORK -> R.string.skill_footwork
        GoalkeeperSkill.TEAMWORK -> R.string.skill_teamwork
    }
