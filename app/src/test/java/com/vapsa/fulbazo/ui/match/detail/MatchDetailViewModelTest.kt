package com.vapsa.fulbazo.ui.match.detail

import com.vapsa.fulbazo.MainDispatcherRule
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.data.repository.FakeCourtRepository
import com.vapsa.fulbazo.data.repository.FakeJoinRequestRepository
import com.vapsa.fulbazo.data.repository.FakeMatchRepository
import com.vapsa.fulbazo.data.repository.FakePlayerRepository
import com.vapsa.fulbazo.domain.model.MatchLevel
import com.vapsa.fulbazo.domain.model.NewMatch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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
class MatchDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository(latencyMillis = 0)
    private val matchRepository = FakeMatchRepository(latencyMillis = 0)
    private val joinRequestRepository = FakeJoinRequestRepository(latencyMillis = 0)

    private suspend fun TestScope.viewModel(matchId: String): MatchDetailViewModel {
        authRepository.login("demo@fulbazo.com", "demo123")
        val viewModel = MatchDetailViewModel(
            matchId,
            authRepository,
            matchRepository,
            FakeCourtRepository(),
            FakePlayerRepository(authRepository),
            joinRequestRepository,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        return viewModel
    }

    private suspend fun demoHostedMatch(slots: Int = 10): String =
        matchRepository.createMatch(
            "demo",
            NewMatch("losa-cayma", FakeMatchRepository.inDays(1), slots = slots, level = MatchLevel.ANY),
        ).getOrThrow().id

    @Test
    fun unknownMatch_isNotFound() = runTest {
        val viewModel = viewModel("no-existe")

        assertTrue(viewModel.uiState.value.notFound)
    }

    @Test
    fun othersMatch_showsDataPlayersAndNoneRole() = runTest {
        val viewModel = viewModel(FakeMatchRepository.SEED_MATCH_ID)

        val state = viewModel.uiState.value
        assertEquals("Losa Deportiva Umacollo", state.match?.courtName)
        assertEquals(ViewerRole.NONE, state.role)
        assertEquals(listOf("carlitos10", "mari.gol", "pedrito"), state.players.map { it.player.nickname })
        assertTrue(state.players.first().isHost)
    }

    @Test
    fun requestToJoin_thenCancel() = runTest {
        val viewModel = viewModel(FakeMatchRepository.SEED_MATCH_ID)

        viewModel.requestToJoin()
        advanceUntilIdle()
        assertEquals(ViewerRole.REQUEST_PENDING, viewModel.uiState.value.role)
        assertEquals(MatchDetailMessage.REQUEST_SENT, viewModel.uiState.value.message)

        viewModel.cancelRequest()
        advanceUntilIdle()
        assertEquals(ViewerRole.NONE, viewModel.uiState.value.role)
    }

    @Test
    fun host_seesPendingRequests_andAcceptingAddsPlayer() = runTest {
        val matchId = demoHostedMatch()
        joinRequestRepository.sendRequest(matchId, "rodrigo")
        joinRequestRepository.sendRequest(matchId, "luis")
        val viewModel = viewModel(matchId)

        val state = viewModel.uiState.value
        assertEquals(ViewerRole.HOST, state.role)
        assertEquals(listOf("rodri", "lucho.arq"), state.pendingRequests.map { it.player.nickname })

        viewModel.respondToRequest(state.pendingRequests.first().requestId, accept = true)
        viewModel.respondToRequest(state.pendingRequests.last().requestId, accept = false)
        advanceUntilIdle()

        val updated = viewModel.uiState.value
        assertTrue(updated.pendingRequests.isEmpty())
        assertEquals(listOf("demo", "rodrigo"), updated.players.map { it.player.id })
    }

    @Test
    fun acceptingWhenFull_showsErrorAndKeepsRequest() = runTest {
        val matchId = demoHostedMatch(slots = 2)
        matchRepository.joinMatch(matchId, "carlos")
        joinRequestRepository.sendRequest(matchId, "rodrigo")
        val viewModel = viewModel(matchId)

        viewModel.respondToRequest(viewModel.uiState.value.pendingRequests.single().requestId, accept = true)
        advanceUntilIdle()

        assertEquals(MatchDetailError.MATCH_FULL, viewModel.uiState.value.error)
        assertEquals(1, viewModel.uiState.value.pendingRequests.size)
    }

    @Test
    fun player_canLeaveMatch() = runTest {
        matchRepository.joinMatch(FakeMatchRepository.SEED_MATCH_ID, "demo")
        val viewModel = viewModel(FakeMatchRepository.SEED_MATCH_ID)
        assertEquals(ViewerRole.PLAYER, viewModel.uiState.value.role)

        viewModel.leaveMatch()
        advanceUntilIdle()

        assertEquals(ViewerRole.NONE, viewModel.uiState.value.role)
        val match = matchRepository.observeMatches().first().first { it.id == FakeMatchRepository.SEED_MATCH_ID }
        assertTrue("demo" !in match.playerIds)
    }

    @Test
    fun pastMatch_isFinished() = runTest {
        val viewModel = viewModel("match-past-selva")

        val state = viewModel.uiState.value
        assertTrue(state.isFinished)
        assertEquals(ViewerRole.PLAYER, state.role)
    }
}
