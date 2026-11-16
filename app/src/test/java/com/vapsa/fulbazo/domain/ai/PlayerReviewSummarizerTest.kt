package com.vapsa.fulbazo.domain.ai

import com.vapsa.fulbazo.data.repository.FakeAiRepository
import com.vapsa.fulbazo.data.repository.FakeCommentRepository
import com.vapsa.fulbazo.domain.model.CommunityStats
import com.vapsa.fulbazo.domain.model.PlayerComment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerReviewSummarizerTest {

    private val comments = FakeCommentRepository(latencyMillis = 0)
    private val ai = FakeAiRepository(latencyMillis = 0)
    private val summarizer = PlayerReviewSummarizer(comments, ai)

    @Test
    fun fewComments_doesNotCallTheAi() = runTest {
        val outcome = summarizer.summarize("rodrigo", "rodri", comments.observeComments("rodrigo").first(), CommunityStats())

        assertEquals(SummaryOutcome.NotEnough(1), outcome)
        assertEquals(0, ai.summaryCalls)
    }

    @Test
    fun enoughComments_generatesAndCachesTheSummary() = runTest {
        val list = comments.observeComments("carlos").first()

        val first = summarizer.summarize("carlos", "carlitos10", list, CommunityStats())
        val second = summarizer.summarize("carlos", "carlitos10", list, CommunityStats())

        assertTrue(first is SummaryOutcome.Ready)
        assertEquals(first, second)
        // La segunda vez usa el resumen guardado.
        assertEquals(1, ai.summaryCalls)
        assertEquals(list.first().id, comments.getSummary("carlos")?.lastCommentId)
    }

    @Test
    fun newComment_regeneratesTheSummary() = runTest {
        summarizer.summarize("carlos", "carlitos10", comments.observeComments("carlos").first(), CommunityStats())

        comments.saveComment("match-past-sachaca", "jose", "carlos", "Defiende poco.")
        summarizer.summarize("carlos", "carlitos10", comments.observeComments("carlos").first(), CommunityStats())

        assertEquals(2, ai.summaryCalls)
    }

    @Test
    fun aiFailure_isReported() = runTest {
        ai.fail = true

        val outcome = summarizer.summarize("carlos", "carlitos10", comments.observeComments("carlos").first(), CommunityStats())

        assertTrue(outcome is SummaryOutcome.Failed)
    }

    @Test
    fun onlyTheLast10Comments_areSentToTheAi() = runTest {
        val many = (1..12).map {
            PlayerComment("x$it", "m$it", "a$it", "ana", "comentario $it", java.time.LocalDateTime.now().minusDays(it.toLong()))
        }
        var received = 0
        val counting = object : com.vapsa.fulbazo.domain.repository.AiRepository by ai {
            override suspend fun summarizeReviews(
                nickname: String,
                comments: List<PlayerComment>,
                community: CommunityStats,
            ): Result<String> {
                received = comments.size
                return Result.success("ok")
            }
        }

        PlayerReviewSummarizer(comments, counting).summarize("ana", "ana", many, CommunityStats())

        assertEquals(AiPrompts.MAX_COMMENTS, received)
    }
}
