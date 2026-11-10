package com.vapsa.fulbazo.ui.stats

import com.vapsa.fulbazo.MainDispatcherRule
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.data.repository.FakeStatsRepository
import com.vapsa.fulbazo.domain.model.FieldSkill
import com.vapsa.fulbazo.domain.model.Foot
import com.vapsa.fulbazo.domain.model.GoalkeeperSkill
import com.vapsa.fulbazo.domain.model.NewUser
import com.vapsa.fulbazo.domain.model.PlayerProfile
import com.vapsa.fulbazo.domain.model.Position
import com.vapsa.fulbazo.domain.model.SelfAssessment
import com.vapsa.fulbazo.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class MyStatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository(latencyMillis = 0)
    private val statsRepository = FakeStatsRepository(latencyMillis = 0)

    private suspend fun loggedInViewModel(
        email: String = "demo@fulbazo.com",
        password: String = "demo123",
        stats: StatsRepository = statsRepository,
    ): MyStatsViewModel {
        authRepository.login(email, password)
        return MyStatsViewModel(authRepository, stats)
    }

    @Test
    fun withoutSession_loadFails() = runTest {
        val viewModel = MyStatsViewModel(authRepository, statsRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.loadFailed)
    }

    @Test
    fun savedAssessment_prefillsTheForm() = runTest {
        val viewModel = loggedInViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(Foot.RIGHT, state.dominantFoot)
        assertEquals(Position.MIDFIELDER, state.mainPosition)
        assertEquals(5, state.fieldSkills[FieldSkill.PASSING])
    }

    @Test
    fun choosingSamePositionAsMain_clearsSecondary() = runTest {
        val viewModel = loggedInViewModel()
        advanceUntilIdle()

        viewModel.onMainPositionChange(Position.FORWARD)

        assertNull(viewModel.uiState.value.secondaryPosition)
    }

    @Test
    fun goalkeeperSkills_onlyShownForGoalkeepers() = runTest {
        val viewModel = loggedInViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showGoalkeeperSkills)

        viewModel.onSecondaryPositionChange(Position.GOALKEEPER)

        assertTrue(viewModel.uiState.value.showGoalkeeperSkills)
    }

    @Test
    fun skillValues_areClampedToScale() = runTest {
        val viewModel = loggedInViewModel()
        advanceUntilIdle()

        viewModel.onFieldSkillChange(FieldSkill.SPEED, 9)
        viewModel.onGoalkeeperSkillChange(GoalkeeperSkill.SAVES, 0)

        assertEquals(5, viewModel.uiState.value.fieldSkills[FieldSkill.SPEED])
        assertEquals(1, viewModel.uiState.value.goalkeeperSkills[GoalkeeperSkill.SAVES])
    }

    @Test
    fun saveWithoutFootOrPosition_showsErrors() = runTest {
        authRepository.register(
            NewUser(
                firstName = "Ana",
                lastName = "Mamani",
                nickname = "ana9",
                phone = "912345678",
                email = "ana@fulbazo.com",
                birthDate = LocalDate.of(2001, 1, 1),
                district = "Cayma",
                password = "secreto1",
            ),
        )
        val viewModel = MyStatsViewModel(authRepository, statsRepository)
        advanceUntilIdle()

        viewModel.save()

        assertTrue(viewModel.uiState.value.footMissing)
        assertTrue(viewModel.uiState.value.positionMissing)
    }

    @Test
    fun save_storesAssessmentWithoutGoalkeeperSkillsForFieldPlayers() = runTest {
        val viewModel = loggedInViewModel()
        advanceUntilIdle()

        viewModel.onDominantFootChange(Foot.LEFT)
        viewModel.onFieldSkillChange(FieldSkill.SHOOTING, 5)
        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        val saved = statsRepository.observeProfile("demo").first().selfAssessment!!
        assertEquals(Foot.LEFT, saved.dominantFoot)
        assertEquals(5, saved.fieldSkills[FieldSkill.SHOOTING])
        assertNull(saved.goalkeeperSkills)
    }

    @Test
    fun saveError_isShown() = runTest {
        val failing = object : StatsRepository {
            override fun observeProfile(userId: String): Flow<PlayerProfile> =
                flowOf(PlayerProfile(userId))

            override suspend fun saveSelfAssessment(userId: String, assessment: SelfAssessment) =
                Result.failure<Unit>(IllegalStateException("sin red"))
        }
        val viewModel = loggedInViewModel(stats = failing)
        advanceUntilIdle()

        viewModel.onDominantFootChange(Foot.BOTH)
        viewModel.onMainPositionChange(Position.DEFENDER)
        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saveFailed)
        assertFalse(viewModel.uiState.value.isSaved)
    }
}
