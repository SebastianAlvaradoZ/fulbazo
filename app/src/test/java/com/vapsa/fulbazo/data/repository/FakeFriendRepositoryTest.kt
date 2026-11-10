package com.vapsa.fulbazo.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeFriendRepositoryTest {

    private val repository = FakeFriendRepository(latencyMillis = 0)

    @Test
    fun demo_startsWithTwoFriendsAndOneRequest() = runTest {
        assertEquals(setOf("carlos", "maria"), repository.observeFriendIds("demo").first())
        assertEquals("jose", repository.observePendingRequests("demo").first().single().fromUserId)
    }

    @Test
    fun acceptingRequest_makesThemFriends() = runTest {
        repository.respondToRequest("req-jose", accept = true)

        assertTrue("jose" in repository.observeFriendIds("demo").first())
        assertTrue(repository.observePendingRequests("demo").first().isEmpty())
    }

    @Test
    fun rejectingRequest_doesNotAddFriend() = runTest {
        repository.respondToRequest("req-jose", accept = false)

        assertFalse("jose" in repository.observeFriendIds("demo").first())
    }

    @Test
    fun sendingRequestBack_acceptsTheExistingOne() = runTest {
        repository.sendRequest(fromUserId = "demo", toUserId = "jose")

        assertTrue("jose" in repository.observeFriendIds("demo").first())
    }

    @Test
    fun sentRequest_isPendingUntilAnswered() = runTest {
        repository.sendRequest("demo", "valeria")

        val pending = repository.observePendingRequests("valeria").first()
        assertEquals("demo", pending.single().fromUserId)
        assertFalse("valeria" in repository.observeFriendIds("demo").first())
    }
}
