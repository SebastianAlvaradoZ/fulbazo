package com.vapsa.fulbazo.ui.match.create

import com.vapsa.fulbazo.MainDispatcherRule
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.data.repository.FakeCourtRepository
import com.vapsa.fulbazo.data.repository.FakeFriendRepository
import com.vapsa.fulbazo.data.repository.FakeGroupRepository
import com.vapsa.fulbazo.data.repository.FakeInvitationRepository
import com.vapsa.fulbazo.data.repository.FakeMatchRepository
import com.vapsa.fulbazo.data.repository.FakePlayerRepository
import com.vapsa.fulbazo.domain.model.MatchLevel
import com.vapsa.fulbazo.domain.model.NewMatch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class CreateMatchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = LocalDateTime.of(2026, 11, 10, 12, 0)
    private val authRepository = FakeAuthRepository(latencyMillis = 0)
    private val matchRepository = FakeMatchRepository(latencyMillis = 0)
    private val invitationRepository = FakeInvitationRepository(latencyMillis = 0)

    private suspend fun viewModel(): CreateMatchViewModel {
        authRepository.login("demo@fulbazo.com", "demo123")
        return CreateMatchViewModel(
            authRepository = authRepository,
            courtRepository = FakeCourtRepository(),
            matchRepository = matchRepository,
            playerRepository = FakePlayerRepository(authRepository),
            friendRepository = FakeFriendRepository(latencyMillis = 0),
            groupRepository = FakeGroupRepository(latencyMillis = 0),
            invitationRepository = invitationRepository,
            now = { now },
        )
    }

    private fun CreateMatchViewModel.fillValid() {
        onCourtChange("losa-cayma")
        onDateChange(LocalDate.of(2026, 11, 12))
        onTimeChange(LocalTime.of(19, 0))
    }

    @Test
    fun loadsCourtsFriendsAndGroups() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(8, state.courts.size)
        assertEquals(setOf("carlos", "maria"), state.friends.map { it.id }.toSet())
        assertEquals(1, state.groups.size)
    }

    @Test
    fun emptyForm_showsRequiredErrors() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.createMatch()

        assertEquals(
            setOf(
                CreateMatchError.COURT_REQUIRED,
                CreateMatchError.DATE_REQUIRED,
                CreateMatchError.TIME_REQUIRED,
            ),
            viewModel.uiState.value.errors,
        )
    }

    @Test
    fun pastDateTime_isRejected() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.fillValid()
        viewModel.onDateChange(LocalDate.of(2026, 11, 10))
        viewModel.onTimeChange(LocalTime.of(11, 0))

        viewModel.createMatch()

        assertTrue(CreateMatchError.DATE_IN_PAST in viewModel.uiState.value.errors)
    }

    @Test
    fun slots_stayWithinLimits() = runTest {
        val viewModel = viewModel()
        viewModel.onSlotsChange(100)
        assertEquals(NewMatch.MAX_SLOTS, viewModel.uiState.value.slots)
        viewModel.onSlotsChange(0)
        assertEquals(NewMatch.MIN_SLOTS, viewModel.uiState.value.slots)
    }

    @Test
    fun createMatch_userIsHost_andFriendsAndGroupMembersAreInvited() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.fillValid()
        viewModel.onLevelChange(MatchLevel.BEGINNER)
        viewModel.onToggleFriend("carlos")
        viewModel.onToggleGroup("group-pichanga") // carlos y maria: carlos no se duplica

        viewModel.createMatch()
        advanceUntilIdle()

        val matchId = viewModel.uiState.value.createdMatchId
        assertNotNull(matchId)
        val match = matchRepository.observeMatches().first().first { it.id == matchId }
        assertEquals("demo", match.hostId)
        assertEquals(listOf("demo"), match.playerIds)
        assertEquals(MatchLevel.BEGINNER, match.level)
        assertEquals(1, invitationRepository.observeReceived("carlos").first().size)
        assertEquals(1, invitationRepository.observeReceived("maria").first().size)
    }
}
