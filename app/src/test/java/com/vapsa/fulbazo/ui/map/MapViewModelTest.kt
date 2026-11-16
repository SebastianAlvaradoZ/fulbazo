package com.vapsa.fulbazo.ui.map

import com.vapsa.fulbazo.FakeLocationRepository
import com.vapsa.fulbazo.MainDispatcherRule
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.data.repository.FakeCourtRepository
import com.vapsa.fulbazo.data.repository.FakeMatchRepository
import com.vapsa.fulbazo.data.repository.FakePlayerRepository
import com.vapsa.fulbazo.domain.model.Court
import com.vapsa.fulbazo.domain.model.GeoPoint
import com.vapsa.fulbazo.domain.repository.CourtRepository
import com.vapsa.fulbazo.domain.repository.CourtSyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
            FakeLocationRepository(),
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
            FakeLocationRepository(),
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
            FakeLocationRepository(),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.openCourt("losa-melgar")
        advanceUntilIdle()
        assertEquals("losa-melgar", viewModel.uiState.value.openedCourtId)

        viewModel.closeCourt()
        advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.openedCourtId)
    }

    private fun viewModelWith(location: FakeLocationRepository, mapsConfigured: Boolean = true): MapViewModel {
        val authRepository = FakeAuthRepository(latencyMillis = 0)
        return MapViewModel(
            authRepository,
            FakeCourtRepository(),
            FakeMatchRepository(latencyMillis = 0),
            FakePlayerRepository(authRepository),
            location,
            mapsConfigured = mapsConfigured,
        )
    }

    @Test
    fun withoutPermission_sortsByDate_andDoesNotAskForLocation() = runTest {
        val location = FakeLocationRepository(CAYMA)
        val viewModel = viewModelWith(location)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onLocationPermissionChanged(granted = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LocationPermission.DENIED, state.locationPermission)
        assertFalse(state.sortedByDistance)
        assertEquals(0, location.requests)
        assertEquals(listOf("losa-umacollo", "losa-cayma", "losa-melgar"), state.matches.map { it.courtId })
        assertTrue(state.matches.all { it.distanceKm == null })
    }

    @Test
    fun withPermission_sortsNearestFirst_andShowsDistance() = runTest {
        val location = FakeLocationRepository(CAYMA)
        val viewModel = viewModelWith(location)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onLocationPermissionChanged(granted = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LocationPermission.GRANTED, state.locationPermission)
        assertEquals(CAYMA, state.userLocation)
        assertTrue(state.sortedByDistance)
        // Cayma (0 km), Umacollo (~2.4 km) y Mariano Melgar (~4.8 km), aunque Umacollo sea antes.
        assertEquals(listOf("losa-cayma", "losa-umacollo", "losa-melgar"), state.matches.map { it.courtId })
        assertEquals(0.0, state.matches[0].distanceKm!!, 0.01)
        assertEquals(2.4, state.matches[1].distanceKm!!, 0.1)
        assertEquals(4.8, state.matches[2].distanceKm!!, 0.1)
    }

    @Test
    fun permissionGrantedButNoLocation_keepsDateOrder() = runTest {
        val viewModel = viewModelWith(FakeLocationRepository(location = null))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onLocationPermissionChanged(granted = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LocationPermission.GRANTED, state.locationPermission)
        assertNull(state.userLocation)
        assertEquals(listOf("losa-umacollo", "losa-cayma", "losa-melgar"), state.matches.map { it.courtId })
    }

    @Test
    fun revokingPermission_forgetsLocation() = runTest {
        val location = FakeLocationRepository(CAYMA)
        val viewModel = viewModelWith(location)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        viewModel.onLocationPermissionChanged(granted = true)
        advanceUntilIdle()
        // Volver a la pantalla con permiso no vuelve a pedir la ubicación.
        viewModel.onLocationPermissionChanged(granted = true)
        advanceUntilIdle()
        assertEquals(1, location.requests)

        viewModel.onLocationPermissionChanged(granted = false)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.userLocation)
        assertFalse(viewModel.uiState.value.sortedByDistance)
    }

    @Test
    fun filterKeepsDistance() = runTest {
        val viewModel = viewModelWith(FakeLocationRepository(CAYMA))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        viewModel.onLocationPermissionChanged(granted = true)
        viewModel.onFilterCourt("losa-melgar")
        advanceUntilIdle()

        val matches = viewModel.uiState.value.matches
        assertEquals(listOf("losa-melgar"), matches.map { it.courtId })
        assertEquals(4.8, matches.single().distanceKm!!, 0.1)
    }

    @Test
    fun withoutMapsKey_reportsMapUnavailable() = runTest {
        val viewModel = viewModelWith(FakeLocationRepository(), mapsConfigured = false)
        assertFalse(viewModel.uiState.value.mapsAvailable)
        val loaded = viewModel.uiState.filter { !it.isLoading }.first()
        assertFalse(loaded.mapsAvailable)
        assertEquals(3, loaded.matches.size)
    }

    /** Losas que se pueden "desconectar" para probar el aviso de sin conexión. */
    private class OfflineCourts(private val saved: List<Court>) : CourtRepository {
        override fun observeCourts(): Flow<List<Court>> = flowOf(saved)
        override val syncStatus = MutableStateFlow(CourtSyncStatus.OFFLINE)
        var refreshCalls = 0
        override suspend fun refresh(): Result<Unit> {
            refreshCalls++
            syncStatus.value = CourtSyncStatus.UP_TO_DATE
            return Result.success(Unit)
        }
    }

    private fun viewModelWithCourts(courts: CourtRepository): MapViewModel {
        val authRepository = FakeAuthRepository(latencyMillis = 0)
        return MapViewModel(
            authRepository,
            courts,
            FakeMatchRepository(latencyMillis = 0),
            FakePlayerRepository(authRepository),
            FakeLocationRepository(),
        )
    }

    @Test
    fun offlineWithSavedCourts_showsBanner_andRetryRefreshes() = runTest {
        val courts = OfflineCourts(FakeCourtRepository.COURTS)
        val viewModel = viewModelWithCourts(courts)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showingSavedCourts)
        assertFalse(viewModel.uiState.value.courtsUnavailable)

        viewModel.retryCourts()
        advanceUntilIdle()

        assertEquals(1, courts.refreshCalls)
        assertFalse(viewModel.uiState.value.showingSavedCourts)
    }

    @Test
    fun offlineWithoutSavedCourts_showsError() = runTest {
        val viewModel = viewModelWithCourts(OfflineCourts(emptyList()))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.courtsUnavailable)
        assertFalse(viewModel.uiState.value.showingSavedCourts)
    }

    private companion object {
        /** Plaza de Cayma, donde está la losa de Cayma. */
        val CAYMA = GeoPoint(-16.3720, -71.5450)
    }
}
