package com.vapsa.fulbazo.ui.profile

import com.vapsa.fulbazo.MainDispatcherRule
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.data.repository.FakeStatsRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun logout_clearsSessionAndSignalsNavigation() = runTest {
        val authRepository = FakeAuthRepository(latencyMillis = 0)
        authRepository.login("demo@fulbazo.com", "demo123")
        val viewModel = ProfileViewModel(authRepository, FakeStatsRepository(latencyMillis = 0))

        viewModel.logout()
        advanceUntilIdle()

        assertNull(authRepository.currentUser.value)
        assertTrue(viewModel.loggedOut.value)
    }
}
