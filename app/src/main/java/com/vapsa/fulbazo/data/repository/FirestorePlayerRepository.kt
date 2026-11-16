package com.vapsa.fulbazo.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.vapsa.fulbazo.data.remote.Collections
import com.vapsa.fulbazo.data.remote.snapshotFlow
import com.vapsa.fulbazo.domain.model.PlayerSummary
import com.vapsa.fulbazo.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Directorio de jugadores: la parte pública de `usuarios/{uid}`. Los datos
 * privados (celular, correo, fecha de nacimiento) están en otro documento que
 * solo lee su dueño.
 */
@Singleton
class FirestorePlayerRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : PlayerRepository {

    override fun observePlayers(): Flow<Map<String, PlayerSummary>> =
        firestore.collection(Collections.USERS).snapshotFlow().map { snapshot ->
            snapshot.documents.mapNotNull { it.toPlayerSummary() }.associateBy { it.id }
        }

    /**
     * Busca dentro del directorio ya descargado: con pocos jugadores es más simple
     * que un índice y permite buscar por cualquier parte del apodo.
     */
    override suspend fun searchByNickname(query: String, excludeUserId: String): List<PlayerSummary> {
        val text = query.trim()
        if (text.isEmpty()) return emptyList()
        return observePlayers().first().values
            .filter { it.id != excludeUserId && it.nickname.contains(text, ignoreCase = true) }
            .sortedBy { it.nickname.lowercase() }
    }
}

/** Datos públicos de un jugador, o `null` si el documento está incompleto. */
internal fun DocumentSnapshot.toPlayerSummary(): PlayerSummary? {
    val nickname = getString("apodo") ?: return null
    return PlayerSummary(
        id = id,
        nickname = nickname,
        fullName = listOfNotNull(getString("nombre"), getString("apellido")).joinToString(" "),
        district = getString("distrito").orEmpty(),
        photoUri = getString("foto"),
    )
}
