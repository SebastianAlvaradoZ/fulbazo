package com.vapsa.fulbazo.ui.map

import com.vapsa.fulbazo.MainDispatcherRule
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.data.repository.FakeCourtRepository
import com.vapsa.fulbazo.data.repository.FakeMatchRepository
import com.vapsa.fulbazo.data.repository.FakePlayerRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun showsUpcomingOpenMatchesSortedByDate_withHostNickname() = runTest {
        val authRepository = FakeAuthRepository(latencyMillis = 0)
        val viewModel = MapViewModel(
            authRepository,
            FakeCourtRepository(),
            FakeMatchRepository(latencyMillis = 0),
            FakePlayerRepository(authRepository),
        )

        val state = viewModel.uiState.filter { !it.isLoading }.first()

        assertEquals(3, state.matches.size)
        assertTrue(state.matches.zipWithNext().all { (a, b) -> !a.dateTime.isAfter(b.dateTime) })
        assertEquals("carlitos10", state.matches.first().hostNickname)
        assertEquals("Losa Deportiva Umacollo", state.matches.first().courtName)
        assertEquals(8, state.courts.size)
        assertTrue("losa-umacollo" in state.courtIdsWithMatches)
    }
}
