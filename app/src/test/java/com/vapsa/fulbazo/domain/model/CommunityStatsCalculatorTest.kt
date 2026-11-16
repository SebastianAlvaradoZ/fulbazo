package com.vapsa.fulbazo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CommunityStatsCalculatorTest {

    private fun fieldRating(rater: String, value: Int) = PlayerRating(
        matchId = "m1",
        raterId = rater,
        ratedId = "ana",
        role = MatchRole.FIELD,
        fieldSkills = FieldSkill.entries.associateWith { value },
    )

    private fun goalkeeperRating(rater: String, value: Int) = PlayerRating(
        matchId = "m1",
        raterId = rater,
        ratedId = "ana",
        role = MatchRole.GOALKEEPER,
        goalkeeperSkills = GoalkeeperSkill.entries.associateWith { value },
    )

    @Test
    fun noRatings_hasNoCommunityStats() {
        val stats = CommunityStatsCalculator.fromRatings(emptyList())

        assertNull(stats.asFieldPlayer)
        assertNull(stats.asGoalkeeper)
    }

    @Test
    fun averagesEachSkill_andCountsVotesPerRole() {
        val stats = CommunityStatsCalculator.fromRatings(
            listOf(fieldRating("a", 4), fieldRating("b", 3), goalkeeperRating("c", 5)),
        )

        val field = requireNotNull(stats.asFieldPlayer)
        assertEquals(2, field.votes)
        assertEquals(3.5, field.averages.getValue(FieldSkill.SPEED), 0.001)
        assertEquals(3.5, field.overall, 0.001)

        val goalkeeper = requireNotNull(stats.asGoalkeeper)
        assertEquals(1, goalkeeper.votes)
        assertEquals(5.0, goalkeeper.averages.getValue(GoalkeeperSkill.SAVES), 0.001)
    }

    @Test
    fun incompleteRatings_areIgnored() {
        val incomplete = fieldRating("a", 4).copy(fieldSkills = mapOf(FieldSkill.SPEED to 1))

        val stats = CommunityStatsCalculator.fromRatings(listOf(incomplete, fieldRating("b", 2)))

        val field = requireNotNull(stats.asFieldPlayer)
        assertEquals(1, field.votes)
        assertEquals(2.0, field.averages.getValue(FieldSkill.SPEED), 0.001)
    }
}
