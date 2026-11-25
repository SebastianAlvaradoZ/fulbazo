package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.domain.model.AuthError
import com.vapsa.fulbazo.domain.model.AuthResult
import com.vapsa.fulbazo.domain.model.NewUser
import com.vapsa.fulbazo.domain.model.ProfileUpdate
import com.vapsa.fulbazo.domain.model.User
import com.vapsa.fulbazo.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.UUID

/**
 * Implementación en memoria de [AuthRepository] para desarrollar sin backend.
 * Los datos se pierden al cerrar la app. Se reemplazará por Firebase Auth.
 */
class FakeAuthRepository(
    private val latencyMillis: Long = 800,
) : AuthRepository {

    private data class StoredUser(val user: User, val password: String)

    private val mutex = Mutex()
    private val users = mutableMapOf<String, StoredUser>().apply {
        // Usuario de prueba para no tener que registrarse en cada ejecución.
        val demo = User(
            id = "demo",
            firstName = "Usuario",
            lastName = "Demo",
            nickname = "demo",
            phone = "987654321",
            email = "demo@fulbazo.com",
            birthDate = LocalDate.of(2000, 1, 1),
            district = "Yanahuara",
        )
        put(demo.email, StoredUser(demo, "demo123"))
    }

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override fun observeSessionUserId(): Flow<String?> = currentUser.map { it?.id }

    /** En memoria no hay sesión guardada: solo sigue la de esta ejecución. */
    override suspend fun restoreSession(): User? = _currentUser.value

    override suspend fun login(email: String, password: String): AuthResult {
        delay(latencyMillis)
        val stored = mutex.withLock { users[email.lowercase()] }
        if (stored == null || stored.password != password) {
            return AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
        }
        _currentUser.value = stored.user
        return AuthResult.Success(stored.user)
    }

    override suspend fun register(newUser: NewUser): AuthResult {
        delay(latencyMillis)
        val email = newUser.email.lowercase()
        val user = mutex.withLock {
            if (users.containsKey(email)) {
                return AuthResult.Failure(AuthError.EMAIL_ALREADY_IN_USE)
            }
            if (users.values.any { it.user.nickname.equals(newUser.nickname, ignoreCase = true) }) {
                return AuthResult.Failure(AuthError.NICKNAME_ALREADY_IN_USE)
            }
            User(
                id = UUID.randomUUID().toString(),
                firstName = newUser.firstName,
                lastName = newUser.lastName,
                nickname = newUser.nickname,
                phone = newUser.phone,
                email = email,
                birthDate = newUser.birthDate,
                district = newUser.district,
                photoUri = newUser.photoUri,
            ).also { users[email] = StoredUser(it, newUser.password) }
        }
        _currentUser.value = user
        return AuthResult.Success(user)
    }

    override suspend fun updateProfile(update: ProfileUpdate): AuthResult {
        delay(latencyMillis)
        val current = _currentUser.value ?: return AuthResult.Failure(AuthError.UNKNOWN)
        val updated = mutex.withLock {
            val nicknameTaken = users.values.any {
                it.user.id != current.id && it.user.nickname.equals(update.nickname, ignoreCase = true)
            }
            if (nicknameTaken) return AuthResult.Failure(AuthError.NICKNAME_ALREADY_IN_USE)
            val stored = users.getValue(current.email)
            stored.user.copy(
                firstName = update.firstName,
                lastName = update.lastName,
                nickname = update.nickname,
                phone = update.phone,
                district = update.district,
                photoUri = update.photoUri,
            ).also { users[current.email] = stored.copy(user = it) }
        }
        _currentUser.value = updated
        return AuthResult.Success(updated)
    }

    override suspend fun logout() {
        _currentUser.value = null
    }
}
