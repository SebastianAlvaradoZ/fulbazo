package com.vapsa.fulbazo.ui.map

import com.vapsa.fulbazo.MainDispatcherRule
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.data.repository.FakeCourtRepository
import com.vapsa.fulbazo.data.repository.FakeMatchRepository
import com.vapsa.fulbazo.data.repository.FakePlayerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
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

    @Test
    fun filterByCourt_showsOnlyItsMatches_andTogglesOff() = runTest {
        val authRepository = FakeAuthRepository(latencyMillis = 0)
        val viewModel = MapViewModel(
            authRepository,
            FakeCourtRepository(),
            FakeMatchRepository(latencyMillis = 0),
            FakePlayerRepository(authRepository),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.matchCountByCourt["losa-cayma"])
        // En el filtro aparecen primero las losas que tienen partidos.
        val state = viewModel.uiState.value
        assertTrue((state.matchCountByCourt[state.filterCourts.first().id] ?: 0) > 0)

        viewModel.onFilterCourt("losa-cayma")
        advanceUntilIdle()
        assertEquals(listOf("losa-cayma"), viewModel.uiState.value.matches.map { it.courtId })

        viewModel.onFilterCourt("losa-cayma")
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.matches.size)
    }

    @Test
    fun openAndCloseCourtSheet() = runTest {
        val authRepository = FakeAuthRepository(latencyMillis = 0)
        val viewModel = MapViewModel(
            authRepository,
            FakeCourtRepository(),
            FakeMatchRepository(latencyMillis = 0),
            FakePlayerRepository(authRepository),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.openCourt("losa-melgar")
        advanceUntilIdle()
        assertEquals("losa-melgar", viewModel.uiState.value.openedCourtId)

        viewModel.closeCourt()
        advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.openedCourtId)
    }
}
