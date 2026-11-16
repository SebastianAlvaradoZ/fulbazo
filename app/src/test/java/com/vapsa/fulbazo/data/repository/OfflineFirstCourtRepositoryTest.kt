package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.data.local.CourtDao
import com.vapsa.fulbazo.data.local.CourtEntity
import com.vapsa.fulbazo.data.remote.CourtApi
import com.vapsa.fulbazo.data.remote.CourtDto
import com.vapsa.fulbazo.domain.repository.CourtSyncStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstCourtRepositoryTest {

    /** Room de mentira: guarda las losas en memoria. */
    private class FakeCourtDao(initial: List<CourtEntity> = emptyList()) : CourtDao() {
        val stored = MutableStateFlow(initial)
        override fun observeAll(): Flow<List<CourtEntity>> = stored
        override suspend fun replaceAll(courts: List<CourtEntity>) {
            stored.value = courts.sortedBy { it.name }
        }
        override suspend fun insertAll(courts: List<CourtEntity>) = error("No se usa")
        override suspend fun deleteAll() = error("No se usa")
    }

    private class FakeCourtApi(var response: () -> Map<String, CourtDto>) : CourtApi {
        var calls = 0
        override suspend fun getCourts(): Map<String, CourtDto> {
            calls++
            return response()
        }
    }

    private val serverCourts = mapOf(
        "losa-cayma" to CourtDto("Losa Deportiva Cayma", "Cayma", -16.372, -71.545, "Plaza de Cayma", false),
        "losa-umacollo" to CourtDto("Losa Deportiva Umacollo", "Yanahuara", -16.3935, -71.548, "Av. Ejército", true),
    )

    private val savedCourt = CourtEntity("losa-vieja", "Losa Guardada", "Cercado", -16.4, -71.53, "", false)

    private fun TestScope.repository(api: CourtApi, dao: CourtDao) = OfflineFirstCourtRepository(api, dao, this)

    @Test
    fun withNetwork_downloadsAndSavesCourts() = runTest {
        val dao = FakeCourtDao()
        val repo = repository(FakeCourtApi { serverCourts }, dao)

        repo.observeCourts().first()
        advanceUntilIdle()

        assertEquals(CourtSyncStatus.UP_TO_DATE, repo.syncStatus.value)
        assertEquals(listOf("Losa Deportiva Cayma", "Losa Deportiva Umacollo"), repo.observeCourts().first().map { it.name })
    }

    @Test
    fun withoutNetwork_keepsSavedCourts_andReportsOffline() = runTest {
        val dao = FakeCourtDao(listOf(savedCourt))
        val repo = repository(FakeCourtApi { throw IOException("Sin red") }, dao)

        repo.observeCourts().first()
        advanceUntilIdle()

        assertEquals(CourtSyncStatus.OFFLINE, repo.syncStatus.value)
        assertEquals(listOf("losa-vieja"), repo.observeCourts().first().map { it.id })
    }

    @Test
    fun emptyServerResponse_doesNotDeleteSavedCourts() = runTest {
        val dao = FakeCourtDao(listOf(savedCourt))
        val repo = repository(FakeCourtApi { emptyMap() }, dao)

        val result = repo.refresh()

        assertTrue(result.isFailure)
        assertEquals(listOf(savedCourt), dao.stored.value)
    }

    @Test
    fun retryAfterError_updatesCourts() = runTest {
        var online = false
        val api = FakeCourtApi { if (online) serverCourts else throw IOException("Sin red") }
        val repo = repository(api, FakeCourtDao())
        repo.refresh()
        assertEquals(CourtSyncStatus.OFFLINE, repo.syncStatus.value)

        online = true
        val result = repo.refresh()

        assertTrue(result.isSuccess)
        assertEquals(CourtSyncStatus.UP_TO_DATE, repo.syncStatus.value)
        assertEquals(2, repo.observeCourts().first().size)
    }

    @Test
    fun observingTwice_downloadsOnlyOncePerSession() = runTest {
        val api = FakeCourtApi { serverCourts }
        val repo = repository(api, FakeCourtDao())

        repo.observeCourts().first()
        repo.observeCourts().first()
        advanceUntilIdle()

        assertEquals(1, api.calls)
    }
}
