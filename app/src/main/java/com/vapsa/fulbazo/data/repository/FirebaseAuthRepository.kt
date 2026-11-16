package com.vapsa.fulbazo.data.repository

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.vapsa.fulbazo.data.local.ProfilePhotoEncoder
import com.vapsa.fulbazo.data.remote.Collections
import com.vapsa.fulbazo.domain.model.AuthError
import com.vapsa.fulbazo.domain.model.AuthResult
import com.vapsa.fulbazo.domain.model.NewUser
import com.vapsa.fulbazo.domain.model.ProfileUpdate
import com.vapsa.fulbazo.domain.model.User
import com.vapsa.fulbazo.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Autenticación con Firebase Auth (correo y contraseña). El perfil público del
 * jugador vive en Firestore en `usuarios/{uid}` y sus datos privados (celular,
 * correo, fecha de nacimiento) en `usuarios/{uid}/privado/datos`. Cada apodo se
 * reserva en `apodos/{apodo en minúsculas}` para que no se repita.
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val photoEncoder: ProfilePhotoEncoder,
) : AuthRepository {

    private val users get() = firestore.collection(USERS)
    private val nicknames get() = firestore.collection(NICKNAMES)

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun login(email: String, password: String): AuthResult = runCatchingAuth(isRegister = false) {
        val uid = requireNotNull(auth.signInWithEmailAndPassword(email.trim(), password).await().user).uid
        val user = loadUser(uid)
            // Cuenta sin perfil (p. ej. si el registro se cortó a la mitad).
            ?: return AuthResult.Failure(AuthError.UNKNOWN).also { auth.signOut() }
        _currentUser.value = user
        AuthResult.Success(user)
    }

    override suspend fun register(newUser: NewUser): AuthResult = runCatchingAuth(isRegister = true) {
        val nicknameKey = newUser.nickname.trim().lowercase()
        // Revisión rápida antes de crear la cuenta; la transacción de abajo evita carreras.
        if (nicknames.document(nicknameKey).get().await().exists()) {
            return AuthResult.Failure(AuthError.NICKNAME_ALREADY_IN_USE)
        }

        val account = requireNotNull(
            auth.createUserWithEmailAndPassword(newUser.email.trim(), newUser.password).await().user,
        )
        val user = User(
            id = account.uid,
            firstName = newUser.firstName,
            lastName = newUser.lastName,
            nickname = newUser.nickname,
            phone = newUser.phone,
            email = newUser.email.trim().lowercase(),
            birthDate = newUser.birthDate,
            district = newUser.district,
            photoUri = photoEncoder.encode(newUser.photoUri),
        )
        val saved = try {
            firestore.runTransaction { tx ->
                val nicknameRef = nicknames.document(nicknameKey)
                if (tx.get(nicknameRef).exists()) return@runTransaction false
                tx.set(nicknameRef, mapOf(FIELD_UID to user.id))
                tx.set(users.document(user.id), user.toPublicDocument())
                tx.set(privateDoc(user.id), user.toPrivateDocument())
                true
            }.await()
        } catch (e: Exception) {
            // Sin perfil la cuenta no sirve: se borra para poder registrarse de nuevo.
            runCatching { account.delete().await() }
            throw e
        }
        if (!saved) {
            runCatching { account.delete().await() }
            return AuthResult.Failure(AuthError.NICKNAME_ALREADY_IN_USE)
        }
        _currentUser.value = user
        AuthResult.Success(user)
    }

    override suspend fun updateProfile(update: ProfileUpdate): AuthResult = runCatchingAuth(isRegister = false) {
        val current = _currentUser.value ?: return AuthResult.Failure(AuthError.UNKNOWN)
        val updated = current.copy(
            firstName = update.firstName,
            lastName = update.lastName,
            nickname = update.nickname,
            phone = update.phone,
            district = update.district,
            photoUri = photoEncoder.encode(update.photoUri),
        )
        val oldKey = current.nickname.lowercase()
        val newKey = updated.nickname.trim().lowercase()
        val saved = firestore.runTransaction { tx ->
            if (newKey != oldKey) {
                val newRef = nicknames.document(newKey)
                val owner = tx.get(newRef).getString(FIELD_UID)
                if (owner != null && owner != current.id) return@runTransaction false
                tx.set(newRef, mapOf(FIELD_UID to current.id))
                tx.delete(nicknames.document(oldKey))
            }
            tx.set(users.document(current.id), updated.toPublicDocument())
            tx.set(privateDoc(current.id), updated.toPrivateDocument())
            true
        }.await()
        if (!saved) return AuthResult.Failure(AuthError.NICKNAME_ALREADY_IN_USE)
        _currentUser.value = updated
        AuthResult.Success(updated)
    }

    override suspend fun logout() {
        auth.signOut()
        _currentUser.value = null
    }

    /** Traduce los errores de Firebase a [AuthError]; la pantalla muestra el texto en español. */
    private inline fun runCatchingAuth(isRegister: Boolean, block: () -> AuthResult): AuthResult = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Error de Firebase", e)
        AuthResult.Failure(e.toAuthError(isRegister))
    }

    private fun Exception.toAuthError(isRegister: Boolean): AuthError = when (this) {
        is FirebaseAuthUserCollisionException -> AuthError.EMAIL_ALREADY_IN_USE
        // Al registrarse, "credenciales inválidas" significa un correo con formato raro.
        is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException ->
            if (isRegister) AuthError.UNKNOWN else AuthError.INVALID_CREDENTIALS
        is FirebaseNetworkException -> AuthError.NETWORK
        is FirebaseFirestoreException ->
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE) AuthError.NETWORK else AuthError.UNKNOWN
        else -> AuthError.UNKNOWN
    }

    private fun privateDoc(uid: String) =
        users.document(uid).collection(Collections.PRIVATE).document(Collections.PRIVATE_DOC)

    /**
     * Lee el perfil completo: la parte pública y la privada. Las cuentas creadas
     * antes de separar los datos privados se migran aquí.
     */
    private suspend fun loadUser(uid: String): User? {
        val publicSnap = users.document(uid).get().await()
        if (!publicSnap.exists()) return null
        val privateSnap = privateDoc(uid).get().await()
        if (!privateSnap.exists() && publicSnap.contains(FIELD_PHONE)) {
            val user = toUser(publicSnap, publicSnap) ?: return null
            firestore.runBatch { batch ->
                batch.set(privateDoc(uid), user.toPrivateDocument())
                batch.update(
                    users.document(uid),
                    mapOf(
                        FIELD_PHONE to FieldValue.delete(),
                        FIELD_EMAIL to FieldValue.delete(),
                        FIELD_BIRTH_DATE to FieldValue.delete(),
                    ),
                )
            }.await()
            return user
        }
        return toUser(publicSnap, privateSnap)
    }

    /** Lo que ven los demás jugadores. */
    private fun User.toPublicDocument(): Map<String, Any?> = mapOf(
        "nombre" to firstName,
        "apellido" to lastName,
        "apodo" to nickname,
        "apodoMinusculas" to nickname.lowercase(),
        "distrito" to district,
        "foto" to photoUri,
    )

    /** Solo lo lee su dueño (`usuarios/{uid}/privado/datos`). */
    private fun User.toPrivateDocument(): Map<String, Any?> = mapOf(
        FIELD_PHONE to phone,
        FIELD_EMAIL to email,
        FIELD_BIRTH_DATE to birthDate.toString(),
    )

    private fun toUser(publicSnap: DocumentSnapshot, privateSnap: DocumentSnapshot): User? {
        return User(
            id = publicSnap.id,
            firstName = publicSnap.getString("nombre") ?: return null,
            lastName = publicSnap.getString("apellido") ?: return null,
            nickname = publicSnap.getString("apodo") ?: return null,
            phone = privateSnap.getString(FIELD_PHONE).orEmpty(),
            email = privateSnap.getString(FIELD_EMAIL).orEmpty(),
            birthDate = privateSnap.getString(FIELD_BIRTH_DATE)?.let(LocalDate::parse) ?: return null,
            district = publicSnap.getString("distrito").orEmpty(),
            photoUri = publicSnap.getString("foto"),
        )
    }

    private companion object {
        const val TAG = "FirebaseAuthRepository"
        const val USERS = Collections.USERS
        const val NICKNAMES = Collections.NICKNAMES
        const val FIELD_UID = "uid"
        const val FIELD_PHONE = "celular"
        const val FIELD_EMAIL = "correo"
        const val FIELD_BIRTH_DATE = "fechaNacimiento"
    }
}
