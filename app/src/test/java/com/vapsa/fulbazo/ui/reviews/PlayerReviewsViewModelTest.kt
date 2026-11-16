package com.vapsa.fulbazo.ui.reviews

import com.vapsa.fulbazo.MainDispatcherRule
import com.vapsa.fulbazo.data.repository.FakeAiRepository
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.data.repository.FakeCommentRepository
import com.vapsa.fulbazo.data.repository.FakePlayerRepository
import com.vapsa.fulbazo.data.repository.FakeStatsRepository
import com.vapsa.fulbazo.domain.ai.PlayerReviewSummarizer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerReviewsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository(latencyMillis = 0)
    private val commentRepository = FakeCommentRepository(latencyMillis = 0)
    private val aiRepository = FakeAiRepository(latencyMillis = 0)

    private suspend fun TestScope.viewModel(playerId: String): PlayerReviewsViewModel {
        authRepository.login("demo@fulbazo.com", "demo123")
        val viewModel = PlayerReviewsViewModel(
            playerId = playerId,
            authRepository = authRepository,
            playerRepository = FakePlayerRepository(authRepository),
            statsRepository = FakeStatsRepository(latencyMillis = 0),
            commentRepository = commentRepository,
            summarizer = PlayerReviewSummarizer(commentRepository, aiRepository),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun comments_areListedNewestFirst_withAuthorNickname() = runTest {
        val state = viewModel("carlos").uiState.value

        assertEquals(listOf("c3", "c2", "c1"), state.comments.map { it.id })
        assertEquals("vale9", state.comments.first().authorNickname)
        // c1 lo escribió el usuario demo.
        assertTrue(state.comments.last().isMine)
    }

    @Test
    fun enoughComments_showsAiSummary() = runTest {
        val summary = viewModel("carlos").uiState.value.summary

        assertTrue(summary is AiSummaryState.Ready)
        assertTrue((summary as AiSummaryState.Ready).text.contains("carlitos10"))
    }

    @Test
    fun fewComments_showsNotEnough() = runTest {
        assertEquals(AiSummaryState.NotEnough, viewModel("rodrigo").uiState.value.summary)
    }

    @Test
    fun aiError_canBeRetried() = runTest {
        aiRepository.fail = true
        val viewModel = viewModel("carlos")
        assertEquals(AiSummaryState.Error, viewModel.uiState.value.summary)

        aiRepository.fail = false
        viewModel.retrySummary()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.summary is AiSummaryState.Ready)
    }

    @Test
    fun reportingAComment_marksItAndSendsTheReport() = runTest {
        val viewModel = viewModel("carlos")

        viewModel.report("c2")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("c2" in state.reportedIds)
        assertEquals(ReviewsMessage.REPORTED, state.message)
        assertTrue(("c2" to "demo") in commentRepository.reports.value)
    }

    @Test
    fun ownComment_cannotBeReported() = runTest {
        val viewModel = viewModel("carlos")

        viewModel.report("c1")
        advanceUntilIdle()

        assertTrue(commentRepository.reports.value.isEmpty())
    }
}
