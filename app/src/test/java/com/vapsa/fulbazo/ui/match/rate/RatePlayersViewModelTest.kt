package com.vapsa.fulbazo.ui.match.rate

import com.vapsa.fulbazo.MainDispatcherRule
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.data.repository.FakeCommentRepository
import com.vapsa.fulbazo.data.repository.FakeCourtRepository
import com.vapsa.fulbazo.data.repository.FakeMatchRepository
import com.vapsa.fulbazo.data.repository.FakePlayerRepository
import com.vapsa.fulbazo.data.repository.FakeStatsRepository
import com.vapsa.fulbazo.domain.model.FieldSkill
import com.vapsa.fulbazo.domain.model.GoalkeeperSkill
import com.vapsa.fulbazo.domain.model.MatchRole
import com.vapsa.fulbazo.domain.model.PlayerComment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RatePlayersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository(latencyMillis = 0)
    private val statsRepository = FakeStatsRepository(latencyMillis = 0)
    private val commentRepository = FakeCommentRepository(latencyMillis = 0)

    private suspend fun TestScope.viewModel(matchId: String): RatePlayersViewModel {
        authRepository.login("demo@fulbazo.com", "demo123")
        val viewModel = RatePlayersViewModel(
            matchId = matchId,
            authRepository = authRepository,
            matchRepository = FakeMatchRepository(latencyMillis = 0),
            courtRepository = FakeCourtRepository(),
            playerRepository = FakePlayerRepository(authRepository),
            ratingRepository = statsRepository,
            commentRepository = commentRepository,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun finishedMatch_listsTeammatesWithoutMe() = runTest {
        val state = viewModel("match-past-selva").uiState.value

        assertEquals(listOf("maria", "carlos", "valeria"), state.teammates.map { it.player.id })
        assertEquals(0, state.ratedCount)
    }

    @Test
    fun upcomingMatch_isNotAllowed() = runTest {
        assertTrue(viewModel(FakeMatchRepository.SEED_MATCH_ID).uiState.value.notAllowed)
    }

    @Test
    fun ratingAsFieldPlayer_updatesCommunityStats() = runTest {
        val viewModel = viewModel("match-past-selva")

        viewModel.openRating("valeria")
        viewModel.onFieldSkillChange(FieldSkill.SPEED, 5)
        viewModel.saveRating()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.draft)
        assertEquals("vale9", state.savedNickname)
        assertEquals(MatchRole.FIELD, state.teammates.first { it.player.id == "valeria" }.ratedAs)
        val field = statsRepository.observeProfile("valeria").first().community.asFieldPlayer!!
        assertEquals(1, field.votes)
        assertEquals(5.0, field.averages.getValue(FieldSkill.SPEED), 0.001)
    }

    @Test
    fun ratingAsGoalkeeper_onlySavesGoalkeeperSkills() = runTest {
        val viewModel = viewModel("match-past-selva")

        viewModel.openRating("maria")
        viewModel.onRoleChange(MatchRole.GOALKEEPER)
        viewModel.onGoalkeeperSkillChange(GoalkeeperSkill.SAVES, 5)
        viewModel.saveRating()
        advanceUntilIdle()

        val community = statsRepository.observeProfile("maria").first().community
        assertEquals(3, community.asGoalkeeper?.votes) // 2 de prueba + 1
        assertEquals(4, community.asFieldPlayer?.votes) // sin cambios
    }

    @Test
    fun reopeningRatedPlayer_prefillsPreviousValues() = runTest {
        val viewModel = viewModel("match-past-selva")
        viewModel.openRating("carlos")
        viewModel.onFieldSkillChange(FieldSkill.PASSING, 1)
        viewModel.saveRating()
        advanceUntilIdle()

        viewModel.openRating("carlos")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.draft?.fieldSkills?.get(FieldSkill.PASSING))
    }

    @Test
    fun ratingWithComment_savesCommentOnTheirProfile() = runTest {
        val viewModel = viewModel("match-past-selva")

        viewModel.openRating("valeria")
        viewModel.onCommentChange("  Gran arquera, muy puntual.  ")
        viewModel.saveRating()
        advanceUntilIdle()

        val comments = commentRepository.observeComments("valeria").first()
        assertEquals(listOf("Gran arquera, muy puntual."), comments.map { it.text })
        assertEquals("demo", comments.single().authorId)
    }

    @Test
    fun reopeningRatedPlayer_prefillsComment_andEmptyCommentDeletesIt() = runTest {
        val viewModel = viewModel("match-past-selva")
        viewModel.openRating("valeria")
        viewModel.onCommentChange("Buen pase")
        viewModel.saveRating()
        advanceUntilIdle()

        viewModel.openRating("valeria")
        advanceUntilIdle()
        assertEquals("Buen pase", viewModel.uiState.value.draft?.comment)

        viewModel.onCommentChange("")
        viewModel.saveRating()
        advanceUntilIdle()
        assertTrue(commentRepository.observeComments("valeria").first().isEmpty())
    }

    @Test
    fun comment_isLimitedTo200Characters() = runTest {
        val viewModel = viewModel("match-past-selva")
        viewModel.openRating("valeria")

        viewModel.onCommentChange("a".repeat(250))
        advanceUntilIdle()

        assertEquals(PlayerComment.MAX_LENGTH, viewModel.uiState.value.draft?.comment?.length)
    }
}
