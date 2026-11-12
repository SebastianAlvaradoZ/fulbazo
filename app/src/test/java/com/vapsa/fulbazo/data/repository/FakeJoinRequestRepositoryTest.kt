package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.domain.model.JoinRequestStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeJoinRequestRepositoryTest {

    private val repository = FakeJoinRequestRepository(latencyMillis = 0)

    @Test
    fun sendRequest_isIdempotentWhilePending() = runTest {
        repository.sendRequest("m1", "demo")
        repository.sendRequest("m1", "demo")

        assertEquals(1, repository.observeForMatch("m1").first().size)
        assertEquals(1, repository.observeByUser("demo").first().size)
    }

    @Test
    fun respond_changesStatus_andCannotRespondTwice() = runTest {
        repository.sendRequest("m1", "demo")
        val request = repository.observeForMatch("m1").first().single()

        assertTrue(repository.respond(request.id, accept = false).isSuccess)
        assertEquals(JoinRequestStatus.REJECTED, repository.observeForMatch("m1").first().single().status)
        assertTrue(repository.respond(request.id, accept = true).isFailure)
    }

    @Test
    fun cancelRequest_removesIt() = runTest {
        repository.sendRequest("m1", "demo")
        val request = repository.observeForMatch("m1").first().single()

        repository.cancelRequest(request.id)

        assertTrue(repository.observeForMatch("m1").first().isEmpty())
    }
}
