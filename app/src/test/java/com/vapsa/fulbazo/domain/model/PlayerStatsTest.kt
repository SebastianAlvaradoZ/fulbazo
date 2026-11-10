package com.vapsa.fulbazo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStatsTest {

    private val fieldSkills = mapOf(
        FieldSkill.SPEED to 5,
        FieldSkill.TECHNIQUE to 4,
        FieldSkill.PASSING to 3,
        FieldSkill.SHOOTING to 3,
        FieldSkill.DEFENDING to 2,
        FieldSkill.STAMINA to 4,
        FieldSkill.TEAMWORK to 3,
    )

    @Test
    fun weakFoot_isDeducedFromDominantFoot() {
        assertEquals(Foot.LEFT, Foot.RIGHT.weakFoot)
        assertEquals(Foot.RIGHT, Foot.LEFT.weakFoot)
        assertNull(Foot.BOTH.weakFoot)
    }

    @Test
    fun overall_isTheAverageOfTheSkills() {
        val assessment = SelfAssessment(
            dominantFoot = Foot.RIGHT,
            mainPosition = Position.MIDFIELDER,
            fieldSkills = fieldSkills,
        )

        assertEquals(24.0 / 7, assessment.fieldOverall, 0.001)
        assertNull(assessment.goalkeeperOverall)
        assertFalse(assessment.isGoalkeeper)
    }

    @Test
    fun goalkeeper_asSecondaryPosition_countsAsGoalkeeper() {
        val assessment = SelfAssessment(
            dominantFoot = Foot.LEFT,
            mainPosition = Position.DEFENDER,
            secondaryPosition = Position.GOALKEEPER,
            fieldSkills = fieldSkills,
            goalkeeperSkills = GoalkeeperSkill.entries.associateWith { 4 },
        )

        assertTrue(assessment.isGoalkeeper)
        assertEquals(4.0, assessment.goalkeeperOverall!!, 0.001)
    }

    @Test
    fun skillScale_clampsToOneToFive() {
        assertEquals(1, SkillScale.clamp(0))
        assertEquals(5, SkillScale.clamp(9))
        assertEquals(3, SkillScale.clamp(3))
    }
}
