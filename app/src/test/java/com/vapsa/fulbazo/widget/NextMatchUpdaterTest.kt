package com.vapsa.fulbazo.widget

import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.data.repository.FakeCourtRepository
import com.vapsa.fulbazo.data.repository.FakeInvitationRepository
import com.vapsa.fulbazo.data.repository.FakeMatchRepository
import com.vapsa.fulbazo.domain.model.Match
import com.vapsa.fulbazo.domain.model.MatchLevel
import com.vapsa.fulbazo.domain.model.NewMatch
import com.vapsa.fulbazo.domain.repository.ConnectivityMonitor
import com.vapsa.fulbazo.domain.repository.MatchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class NextMatchUpdaterTest {

    private val today = LocalDate.of(2026, 11, 25)
    private var now = today.atTime(12, 0)

    private val auth = FakeAuthRepository(latencyMillis = 0)
    private val matches = FakeMatchRepository(latencyMillis = 0, today = today)
    private val invitations = FakeInvitationRepository(latencyMillis = 0)
    private val online = MutableStateFlow(true)
    private val store = FakeStore()

    private fun updater(matchRepository: MatchRepository = matches) = NextMatchUpdater(
        authRepository = auth,
        matchRepository = matchRepository,
        courtRepository = FakeCourtRepository(),
        invitationRepository = invitations,
        connectivity = object : ConnectivityMonitor {
            override val isOnline = online
        },
        store = store,
        now = { now },
        refreshWindowMillis = 1_000,
    )

    private suspend fun login() = auth.login("demo@fulbazo.com", "demo123")

    private fun TestScope.startRunning(updater: NextMatchUpdater) {
        backgroundScope.launch { updater.run() }
        runCurrent()
    }

    @Test
    fun withoutSession_showsSignedOut() = runTest {
        updater().refresh()

        assertEquals(NextMatchState.SignedOut, store.saved)
    }

    @Test
    fun withSessionButNoMatches_savesEmptyWithTheHour() = runTest {
        login()

        assertTrue(updater().refresh())

        assertTrue(store.saved!!.signedIn)
        assertNull(store.saved!!.next)
        assertEquals(now, store.saved!!.updatedAt)
    }

    @Test
    fun joiningAndLeavingUpdateAtOnce() = runTest {
        login()
        startRunning(updater())

        matches.joinMatch(FakeMatchRepository.SEED_MATCH_ID, "demo")
        runCurrent()
        assertEquals("Losa Deportiva Umacollo", store.saved?.next?.courtName)

        matches.leaveMatch(FakeMatchRepository.SEED_MATCH_ID, "demo")
        runCurrent()
        assertNull(store.saved?.next)
    }

    @Test
    fun creatingAMatchUpdatesAtOnce() = runTest {
        login()
        startRunning(updater())

        matches.createMatch("demo", NewMatch("losa-cayma", now.plusHours(3), 10, MatchLevel.ANY))
        runCurrent()

        assertEquals("Losa Deportiva Cayma", store.saved?.next?.courtName)
        assertEquals(9, store.saved?.next?.spotsLeft)
    }

    @Test
    fun newInvitationUpdatesTheCounter() = runTest {
        login()
        startRunning(updater())

        invitations.sendInvitations(FakeMatchRepository.SEED_MATCH_ID, "carlos", listOf("demo"))
        runCurrent()

        assertEquals(1, store.saved?.pendingInvitations)
    }

    @Test
    fun loggingOutHidesTheMatches() = runTest {
        login()
        matches.joinMatch(FakeMatchRepository.SEED_MATCH_ID, "demo")
        startRunning(updater())
        assertTrue(store.saved!!.signedIn)

        auth.logout()
        runCurrent()

        assertEquals(NextMatchState.SignedOut, store.saved)
    }

    @Test
    fun withoutWidgetsNothingIsDone() = runTest {
        store.widgets = false
        val updater = updater()
        startRunning(updater)

        assertTrue(updater.refresh())

        assertNull(store.saved)
    }

    @Test
    fun placingTheFirstWidgetStartsListening() = runTest {
        login()
        store.widgets = false
        val updater = updater()
        startRunning(updater)

        store.widgets = true
        updater.refresh()
        matches.joinMatch(FakeMatchRepository.SEED_MATCH_ID, "demo")
        runCurrent()

        assertEquals("Losa Deportiva Umacollo", store.saved?.next?.courtName)
    }

    @Test
    fun offlineRefreshKeepsTheDataAndItsHourButDropsPastMatches() = runTest {
        login()
        matches.createMatch("demo", NewMatch("losa-cayma", now.plusHours(1), 10, MatchLevel.ANY))
        matches.joinMatch(FakeMatchRepository.SEED_MATCH_ID, "demo")
        val updater = updater()
        updater.refresh()
        val firstUpdate = now

        online.value = false
        now = now.plusHours(2)
        updater.refresh()

        assertEquals(listOf(FakeMatchRepository.SEED_MATCH_ID), store.saved!!.matches.map { it.id })
        assertEquals(firstUpdate, store.saved!!.updatedAt)
    }

    @Test
    fun offlineDataWhileRunningKeepsTheLastRealHour() = runTest {
        login()
        startRunning(updater())
        val firstUpdate = now

        online.value = false
        now = now.plusMinutes(30)
        matches.joinMatch(FakeMatchRepository.SEED_MATCH_ID, "demo")
        runCurrent()

        assertEquals(firstUpdate, store.saved!!.updatedAt)
    }

    @Test
    fun refreshErrorAsksToTryLater() = runTest {
        login()

        assertFalse(updater(FailingMatchRepository(matches, failures = 1)).refresh())
    }

    @Test
    fun listeningRecoversAfterAnError() = runTest {
        login()
        matches.joinMatch(FakeMatchRepository.SEED_MATCH_ID, "demo")
        startRunning(updater(FailingMatchRepository(matches, failures = 1)))
        assertNull(store.saved)

        advanceTimeBy(11_000)
        runCurrent()

        assertEquals("Losa Deportiva Umacollo", store.saved?.next?.courtName)
    }

    private class FakeStore : NextMatchStore {
        var widgets = true
        var saved: NextMatchState? = null
        override fun hasWidgets() = widgets
        override suspend fun current() = saved
        override suspend fun save(state: NextMatchState) {
            saved = state
        }
    }

    private class FailingMatchRepository(
        private val delegate: MatchRepository,
        private var failures: Int,
    ) : MatchRepository by delegate {
        override fun observeMatches(): Flow<List<Match>> = flow {
            if (failures-- > 0) error("Sin permiso")
            delegate.observeMatches().collect { emit(it) }
        }
    }
}
