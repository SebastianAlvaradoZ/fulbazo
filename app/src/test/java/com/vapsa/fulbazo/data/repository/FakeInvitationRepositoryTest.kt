package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.domain.model.InvitationStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeInvitationRepositoryTest {

    private val repository = FakeInvitationRepository(latencyMillis = 0)

    @Test
    fun sendInvitations_skipsHostAndDuplicates() = runTest {
        repository.sendInvitations("m1", "demo", listOf("carlos", "maria", "demo", "carlos"))
        repository.sendInvitations("m1", "demo", listOf("maria"))

        assertEquals(1, repository.observeReceived("carlos").first().size)
        assertEquals(1, repository.observeReceived("maria").first().size)
        assertEquals(0, repository.observeReceived("demo").first().size)
    }

    @Test
    fun respond_updatesStatus() = runTest {
        repository.sendInvitations("m1", "carlos", listOf("demo"))
        val invitation = repository.observeReceived("demo").first().single()

        repository.respond(invitation.id, accept = true)

        assertEquals(InvitationStatus.ACCEPTED, repository.observeReceived("demo").first().single().status)
    }
}
