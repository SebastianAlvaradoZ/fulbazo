package com.vapsa.fulbazo.ui.friends

import com.vapsa.fulbazo.MainDispatcherRule
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.data.repository.FakeFriendRepository
import com.vapsa.fulbazo.data.repository.FakeGroupRepository
import com.vapsa.fulbazo.data.repository.FakePlayerRepository
import com.vapsa.fulbazo.domain.model.PlayerRelation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository(latencyMillis = 0)
    private val friendRepository = FakeFriendRepository(latencyMillis = 0)

    private suspend fun viewModel(): FriendsViewModel {
        authRepository.login("demo@fulbazo.com", "demo123")
        return FriendsViewModel(
            authRepository,
            FakePlayerRepository(authRepository),
            friendRepository,
            FakeGroupRepository(latencyMillis = 0),
            searchDebounceMillis = 0,
        )
    }

    @Test
    fun loadsFriendsRequestsAndGroups() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("carlitos10", "mari.gol"), state.friends.map { it.nickname })
        assertEquals("jose.crack", state.incomingRequests.single().player.nickname)
        assertEquals("Pichanga de los jueves", state.groups.single().name)
    }

    @Test
    fun search_marksRelationOfEachPlayer() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onQueryChange("o")
        advanceUntilIdle()

        val relations = viewModel.uiState.value.searchResults.associate { it.player.id to it.relation }
        assertEquals(PlayerRelation.FRIEND, relations["carlos"])
        assertEquals(PlayerRelation.REQUEST_RECEIVED, relations["jose"])
        assertEquals(PlayerRelation.NONE, relations["rodrigo"])
    }

    @Test
    fun sendingRequest_marksPlayerAsRequestSent() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        viewModel.onQueryChange("vale")
        advanceUntilIdle()

        viewModel.sendRequest("valeria")
        advanceUntilIdle()

        assertEquals(PlayerRelation.REQUEST_SENT, viewModel.uiState.value.searchResults.single().relation)
    }

    @Test
    fun acceptingRequest_addsFriend() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.respondToRequest("req-jose", accept = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.friends.any { it.id == "jose" })
        assertTrue(state.incomingRequests.isEmpty())
    }
}
