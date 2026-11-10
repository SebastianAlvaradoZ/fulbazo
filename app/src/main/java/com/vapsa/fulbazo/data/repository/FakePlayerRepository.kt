package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.domain.model.PlayerSummary
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Directorio en memoria: jugadores de prueba más el usuario con sesión iniciada.
 */
class FakePlayerRepository(
    private val authRepository: AuthRepository,
) : PlayerRepository {

    override fun observePlayers(): Flow<Map<String, PlayerSummary>> =
        authRepository.currentUser.map { user ->
            val all = if (user == null) SEED_PLAYERS else SEED_PLAYERS + PlayerSummary(
                id = user.id,
                nickname = user.nickname,
                fullName = user.fullName,
                district = user.district,
                photoUri = user.photoUri,
            )
            all.associateBy { it.id }
        }

    override suspend fun searchByNickname(query: String, excludeUserId: String): List<PlayerSummary> {
        val text = query.trim()
        if (text.isEmpty()) return emptyList()
        return observePlayers().first().values
            .filter { it.id != excludeUserId && it.nickname.contains(text, ignoreCase = true) }
            .sortedBy { it.nickname.lowercase() }
    }

    companion object {
        val SEED_PLAYERS = listOf(
            PlayerSummary("carlos", "carlitos10", "Carlos Huamán", "Yanahuara"),
            PlayerSummary("maria", "mari.gol", "María Condori", "Cayma"),
            PlayerSummary("jose", "jose.crack", "José Mamani", "Mariano Melgar"),
            PlayerSummary("rodrigo", "rodri", "Rodrigo Salas", "Cerro Colorado"),
            PlayerSummary("valeria", "vale9", "Valeria Flores", "Cayma"),
            PlayerSummary("pedro", "pedrito", "Pedro Quispe", "Paucarpata"),
            PlayerSummary("luis", "lucho.arq", "Luis Ccama", "Sachaca"),
        )
    }
}
