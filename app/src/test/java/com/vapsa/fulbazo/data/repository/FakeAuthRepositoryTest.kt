package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.domain.model.AuthError
import com.vapsa.fulbazo.domain.model.AuthResult
import com.vapsa.fulbazo.domain.model.ProfileUpdate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAuthRepositoryTest {

    private val repository = FakeAuthRepository(latencyMillis = 0)

    private val update = ProfileUpdate(
        firstName = "Usuario",
        lastName = "Demo",
        nickname = "demo.nuevo",
        phone = "999888777",
        district = "Cayma",
        photoUri = null,
    )

    @Test
    fun updateProfile_changesCurrentUser_andLoginKeepsChanges() = runTest {
        repository.login("demo@fulbazo.com", "demo123")

        val result = repository.updateProfile(update)

        assertTrue(result is AuthResult.Success)
        assertEquals("demo.nuevo", repository.currentUser.value?.nickname)
        assertEquals("Cayma", repository.currentUser.value?.district)

        repository.logout()
        assertNull(repository.currentUser.value)
        repository.login("demo@fulbazo.com", "demo123")
        assertEquals("demo.nuevo", repository.currentUser.value?.nickname)
    }

    @Test
    fun updateProfile_keepingOwnNickname_isAllowed() = runTest {
        repository.login("demo@fulbazo.com", "demo123")

        assertTrue(repository.updateProfile(update.copy(nickname = "DEMO")) is AuthResult.Success)
    }

    @Test
    fun updateProfile_withoutSession_fails() = runTest {
        val result = repository.updateProfile(update)

        assertEquals(AuthError.UNKNOWN, (result as AuthResult.Failure).error)
    }
}
