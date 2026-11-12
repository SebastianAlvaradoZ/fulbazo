package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.domain.model.FieldSkill
import com.vapsa.fulbazo.domain.model.GoalkeeperSkill
import com.vapsa.fulbazo.domain.model.MatchRole
import com.vapsa.fulbazo.domain.model.PlayerRating
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeStatsRepositoryRatingTest {

    private val repository = FakeStatsRepository(latencyMillis = 0)

    private fun fieldRating(rater: String, rated: String, value: Int, match: String = "m1") = PlayerRating(
        matchId = match,
        raterId = rater,
        ratedId = rated,
        role = MatchRole.FIELD,
        fieldSkills = FieldSkill.entries.associateWith { value },
    )

    @Test
    fun firstRating_createsCommunityStats() = runTest {
        // jose no tiene calificaciones de la comunidad.
        assertNull(repository.observeProfile("jose").first().community.asFieldPlayer)

        repository.submitRating(fieldRating("demo", "jose", 4))

        val field = repository.observeProfile("jose").first().community.asFieldPlayer!!
        assertEquals(1, field.votes)
        assertEquals(4.0, field.overall, 0.001)
    }

    @Test
    fun rating_isAveragedWithExistingVotes() = runTest {
        // carlos tiene 8 votos con 4.5 de velocidad.
        repository.submitRating(fieldRating("demo", "carlos", 1))

        val field = repository.observeProfile("carlos").first().community.asFieldPlayer!!
        assertEquals(9, field.votes)
        assertEquals((4.5 * 8 + 1) / 9, field.averages.getValue(FieldSkill.SPEED), 0.001)
    }

    @Test
    fun ratingAgainInSameMatch_replacesPreviousOne() = runTest {
        repository.submitRating(fieldRating("demo", "jose", 2))
        repository.submitRating(fieldRating("demo", "jose", 5))

        val field = repository.observeProfile("jose").first().community.asFieldPlayer!!
        assertEquals(1, field.votes)
        assertEquals(5.0, field.overall, 0.001)
        assertEquals(1, repository.observeRatingsBy("demo", "m1").first().size)
    }

    @Test
    fun goalkeeperRating_goesToGoalkeeperStats() = runTest {
        repository.submitRating(
            PlayerRating(
                matchId = "m1",
                raterId = "demo",
                ratedId = "jose",
                role = MatchRole.GOALKEEPER,
                goalkeeperSkills = GoalkeeperSkill.entries.associateWith { 3 },
            ),
        )

        val community = repository.observeProfile("jose").first().community
        assertNull(community.asFieldPlayer)
        assertEquals(1, community.asGoalkeeper?.votes)
    }

    @Test
    fun selfOrIncompleteRating_isRejected() = runTest {
        assertTrue(repository.submitRating(fieldRating("demo", "demo", 5)).isFailure)
        assertTrue(
            repository.submitRating(
                PlayerRating("m1", "demo", "jose", MatchRole.FIELD, fieldSkills = mapOf(FieldSkill.SPEED to 3)),
            ).isFailure,
        )
    }
}
