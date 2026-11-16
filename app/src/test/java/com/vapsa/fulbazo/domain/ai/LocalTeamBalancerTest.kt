package com.vapsa.fulbazo.domain.ai

import com.vapsa.fulbazo.domain.model.TeamPlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LocalTeamBalancerTest {

    @Test
    fun everyPlayerIsUsedOnce_andSizesDifferByAtMostOne() {
        val players = (1..7).map { TeamPlayer("p$it", "p$it", communityFieldLevel = it.toDouble() / 2) }

        val split = LocalTeamBalancer.split(players)

        assertEquals(players.map { it.id }.toSet(), (split.teamA + split.teamB).toSet())
        assertEquals(7, split.teamA.size + split.teamB.size)
        assertTrue(abs(split.teamA.size - split.teamB.size) <= 1)
        assertFalse(split.generatedByAi)
    }

    @Test
    fun twoGoalkeepers_goToDifferentTeams() {
        val players = listOf(
            TeamPlayer("gk1", "gk1", isGoalkeeper = true, goalkeeperLevel = 5.0),
            TeamPlayer("gk2", "gk2", isGoalkeeper = true, goalkeeperLevel = 3.0),
            TeamPlayer("f1", "f1", communityFieldLevel = 4.0),
            TeamPlayer("f2", "f2", communityFieldLevel = 2.0),
        )

        val split = LocalTeamBalancer.split(players)

        assertTrue("gk1" in split.teamA != "gk2" in split.teamA)
    }

    @Test
    fun strongestPlayers_areSeparated() {
        val players = listOf(
            TeamPlayer("crack1", "crack1", communityFieldLevel = 5.0),
            TeamPlayer("crack2", "crack2", communityFieldLevel = 4.9),
            TeamPlayer("normal1", "normal1", communityFieldLevel = 2.0),
            TeamPlayer("normal2", "normal2", communityFieldLevel = 2.1),
        )

        val split = LocalTeamBalancer.split(players)

        assertTrue("crack1" in split.teamA != "crack2" in split.teamA)
    }
}
