package com.vapsa.fulbazo.ui

import com.vapsa.fulbazo.MainDispatcherRule
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.domain.model.User
import com.vapsa.fulbazo.domain.repository.AuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun withoutSavedSession_startsInLogin() = runTest(dispatcherRule.testDispatcher) {
        val viewModel = SessionViewModel(FakeAuthRepository(latencyMillis = 0))
        advanceUntilIdle()

        assertEquals(SessionStart.SIGNED_OUT, viewModel.start.value)
    }

    @Test
    fun withSavedSession_startsInMap() = runTest(dispatcherRule.testDispatcher) {
        val auth = FakeAuthRepository(latencyMillis = 0)
        auth.login("demo@fulbazo.com", "demo123")

        val viewModel = SessionViewModel(auth)
        advanceUntilIdle()

        assertEquals(SessionStart.SIGNED_IN, viewModel.start.value)
    }

    @Test
    fun whileLooking_isChecking() = runTest(dispatcherRule.testDispatcher) {
        val answer = CompletableDeferred<User?>()
        val auth = object : AuthRepository by FakeAuthRepository(latencyMillis = 0) {
            override suspend fun restoreSession(): User? = answer.await()
        }

        val viewModel = SessionViewModel(auth)
        advanceUntilIdle()
        assertEquals(SessionStart.CHECKING, viewModel.start.value)

        answer.complete(null)
        advanceUntilIdle()
        assertEquals(SessionStart.SIGNED_OUT, viewModel.start.value)
    }
}
