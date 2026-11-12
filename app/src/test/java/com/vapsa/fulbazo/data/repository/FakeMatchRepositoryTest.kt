package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.domain.model.MatchLevel
import com.vapsa.fulbazo.domain.model.MatchStatus
import com.vapsa.fulbazo.domain.model.NewMatch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeMatchRepositoryTest {

    private val repository = FakeMatchRepository(latencyMillis = 0)

    @Test
    fun createMatch_hostIsFirstPlayer() = runTest {
        val match = repository.createMatch(
            hostId = "demo",
            newMatch = NewMatch(
                courtId = "losa-cayma",
                dateTime = FakeMatchRepository.inDays(1),
                slots = 4,
                level = MatchLevel.ANY,
            ),
        ).getOrThrow()

        assertEquals("demo", match.hostId)
        assertEquals(listOf("demo"), match.playerIds)
        assertEquals(3, match.spotsLeft)
        assertTrue(repository.observeMatches().first().any { it.id == match.id })
    }

    @Test
    fun joinMatch_fillsSlotsAndMarksFull() = runTest {
        val match = repository.createMatch(
            "demo",
            NewMatch("losa-cayma", FakeMatchRepository.inDays(1), slots = 2, level = MatchLevel.ANY),
        ).getOrThrow()

        assertTrue(repository.joinMatch(match.id, "carlos").isSuccess)
        assertTrue(repository.joinMatch(match.id, "maria").isFailure)

        val updated = repository.observeMatches().first().first { it.id == match.id }
        assertEquals(MatchStatus.FULL, updated.status)
    }

    @Test
    fun leaveMatch_freesSpot_butHostCannotLeave() = runTest {
        val match = repository.createMatch(
            "demo",
            NewMatch("losa-cayma", FakeMatchRepository.inDays(1), slots = 2, level = MatchLevel.ANY),
        ).getOrThrow()
        repository.joinMatch(match.id, "carlos")

        assertTrue(repository.leaveMatch(match.id, "carlos").isSuccess)
        assertTrue(repository.leaveMatch(match.id, "demo").isFailure)

        val updated = repository.observeMatches().first().first { it.id == match.id }
        assertEquals(listOf("demo"), updated.playerIds)
        assertEquals(MatchStatus.OPEN, updated.status)
    }
}
