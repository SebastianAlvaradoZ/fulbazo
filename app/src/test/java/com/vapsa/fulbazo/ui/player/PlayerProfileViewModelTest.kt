package com.vapsa.fulbazo.ui.player

import com.vapsa.fulbazo.MainDispatcherRule
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.data.repository.FakeCourtRepository
import com.vapsa.fulbazo.data.repository.FakeFriendRepository
import com.vapsa.fulbazo.data.repository.FakeInvitationRepository
import com.vapsa.fulbazo.data.repository.FakeMatchRepository
import com.vapsa.fulbazo.data.repository.FakePlayerRepository
import com.vapsa.fulbazo.data.repository.FakeStatsRepository
import com.vapsa.fulbazo.domain.model.MatchLevel
import com.vapsa.fulbazo.domain.model.NewMatch
import com.vapsa.fulbazo.domain.model.PlayerRelation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository(latencyMillis = 0)
    private val friendRepository = FakeFriendRepository(latencyMillis = 0)
    private val matchRepository = FakeMatchRepository(latencyMillis = 0)
    private val invitationRepository = FakeInvitationRepository(latencyMillis = 0)

    private suspend fun TestScope.viewModel(playerId: String): PlayerProfileViewModel {
        authRepository.login("demo@fulbazo.com", "demo123")
        val viewModel = PlayerProfileViewModel(
            playerId = playerId,
            authRepository = authRepository,
            playerRepository = FakePlayerRepository(authRepository),
            statsRepository = FakeStatsRepository(latencyMillis = 0),
            friendRepository = friendRepository,
            matchRepository = matchRepository,
            courtRepository = FakeCourtRepository(),
            invitationRepository = invitationRepository,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun unknownPlayer_isNotFound() = runTest {
        assertTrue(viewModel("nadie").uiState.value.notFound)
    }

    @Test
    fun ownProfile_isMe() = runTest {
        assertTrue(viewModel("demo").uiState.value.isMe)
    }

    @Test
    fun friend_showsStatsAndMatchesTogether() = runTest {
        val state = viewModel("carlos").uiState.value

        assertEquals("carlitos10", state.player?.nickname)
        assertEquals(PlayerRelation.FRIEND, state.relation)
        assertNotNull(state.self)
        assertEquals(8, state.community.asFieldPlayer?.votes)
        assertNull(state.community.asGoalkeeper)
        // Selva Alegre, Sachaca y Paucarpata (partidos de prueba ya jugados).
        assertEquals(3, state.matchesTogether)
    }

    @Test
    fun incomingRequest_canBeAccepted() = runTest {
        val viewModel = viewModel("jose")
        assertEquals(PlayerRelation.REQUEST_RECEIVED, viewModel.uiState.value.relation)

        viewModel.acceptFriendRequest()
        advanceUntilIdle()

        assertEquals(PlayerRelation.FRIEND, viewModel.uiState.value.relation)
        assertEquals(PlayerProfileMessage.REQUEST_ACCEPTED, viewModel.uiState.value.message)
    }

    @Test
    fun stranger_canReceiveFriendRequest() = runTest {
        val viewModel = viewModel("valeria")
        assertEquals(PlayerRelation.NONE, viewModel.uiState.value.relation)

        viewModel.sendFriendRequest()
        advanceUntilIdle()

        assertEquals(PlayerRelation.REQUEST_SENT, viewModel.uiState.value.relation)
    }

    @Test
    fun inviteToMatch_onlyMyUpcomingHostedMatches_andOnce() = runTest {
        val match = matchRepository.createMatch(
            "demo",
            NewMatch("losa-cayma", FakeMatchRepository.inDays(2), slots = 10, level = MatchLevel.ANY),
        ).getOrThrow()
        val viewModel = viewModel("valeria")
        // El partido pasado en Sachaca (anfitrión demo) no se puede elegir.
        assertEquals(listOf(match.id), viewModel.uiState.value.invitableMatches.map { it.matchId })

        viewModel.inviteToMatch(match.id)
        advanceUntilIdle()

        assertEquals(1, invitationRepository.observeReceived("valeria").first().size)
        assertTrue(viewModel.uiState.value.invitableMatches.isEmpty())
        assertEquals(PlayerProfileMessage.INVITED, viewModel.uiState.value.message)
    }
}
