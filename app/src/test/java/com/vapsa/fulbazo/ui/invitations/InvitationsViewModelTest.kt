package com.vapsa.fulbazo.ui.invitations

import com.vapsa.fulbazo.MainDispatcherRule
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.data.repository.FakeCourtRepository
import com.vapsa.fulbazo.data.repository.FakeInvitationRepository
import com.vapsa.fulbazo.data.repository.FakeMatchRepository
import com.vapsa.fulbazo.data.repository.FakePlayerRepository
import com.vapsa.fulbazo.domain.model.MatchLevel
import com.vapsa.fulbazo.domain.model.NewMatch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InvitationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository(latencyMillis = 0)
    private val invitationRepository = FakeInvitationRepository(latencyMillis = 0)
    private val matchRepository = FakeMatchRepository(latencyMillis = 0)

    private suspend fun viewModel(): InvitationsViewModel {
        authRepository.login("demo@fulbazo.com", "demo123")
        return InvitationsViewModel(
            authRepository,
            invitationRepository,
            matchRepository,
            FakeCourtRepository(),
            FakePlayerRepository(authRepository),
        )
    }

    @Test
    fun accepting_joinsMatchAndMovesToAnswered() = runTest {
        invitationRepository.sendInvitations(FakeMatchRepository.SEED_MATCH_ID, "carlos", listOf("demo"))
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        val pending = viewModel.uiState.value.pending.single()
        assertEquals("carlitos10", pending.fromNickname)
        assertEquals("Losa Deportiva Umacollo", pending.match?.courtName)

        viewModel.respond(pending.invitationId, accept = true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.pending.isEmpty())
        assertEquals(1, viewModel.uiState.value.answered.size)
        val match = matchRepository.observeMatches().first().first { it.id == FakeMatchRepository.SEED_MATCH_ID }
        assertTrue("demo" in match.playerIds)
    }

    @Test
    fun acceptingFullMatch_showsErrorAndKeepsPending() = runTest {
        val full = matchRepository.createMatch(
            "carlos",
            NewMatch("losa-cayma", FakeMatchRepository.inDays(1), slots = 2, level = MatchLevel.ANY),
        ).getOrThrow()
        matchRepository.joinMatch(full.id, "maria")
        invitationRepository.sendInvitations(full.id, "carlos", listOf("demo"))
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.respond(viewModel.uiState.value.pending.single().invitationId, accept = true)
        advanceUntilIdle()

        assertEquals(InvitationActionError.MATCH_FULL, viewModel.uiState.value.actionError)
        assertEquals(1, viewModel.uiState.value.pending.size)
    }

    @Test
    fun declining_doesNotJoin() = runTest {
        invitationRepository.sendInvitations(FakeMatchRepository.SEED_MATCH_ID, "carlos", listOf("demo"))
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.respond(viewModel.uiState.value.pending.single().invitationId, accept = false)
        advanceUntilIdle()

        val match = matchRepository.observeMatches().first().first { it.id == FakeMatchRepository.SEED_MATCH_ID }
        assertTrue("demo" !in match.playerIds)
        assertTrue(viewModel.uiState.value.pending.isEmpty())
    }
}
