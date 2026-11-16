package com.vapsa.fulbazo.domain.ai

import com.vapsa.fulbazo.domain.model.TeamPlayer
import com.vapsa.fulbazo.domain.model.TeamSplit
import java.util.Locale

/**
 * Reparto de respaldo, sin IA, para cuando Gemini no responde: un arquero por
 * equipo y luego cada jugador (del mejor al más flojo) va al equipo con menos
 * nivel acumulado, sin que los equipos difieran en más de uno.
 */
object LocalTeamBalancer {

    fun split(players: List<TeamPlayer>): TeamSplit {
        val teamA = mutableListOf<TeamPlayer>()
        val teamB = mutableListOf<TeamPlayer>()
        val maxSize = (players.size + 1) / 2

        val (goalkeepers, fieldPlayers) = players.partition { it.isGoalkeeper }
        val ordered = goalkeepers.sortedByDescending { it.goalkeeperLevel ?: it.estimatedLevel } +
            fieldPlayers.sortedByDescending { it.estimatedLevel }

        ordered.forEachIndexed { index, player ->
            val target = when {
                // Los dos primeros arqueros se separan.
                player.isGoalkeeper && index < 2 -> if (index == 0) teamA else teamB
                teamA.size >= maxSize -> teamB
                teamB.size >= maxSize -> teamA
                teamA.level() <= teamB.level() -> teamA
                else -> teamB
            }
            target += player
        }

        return TeamSplit(
            teamA = teamA.map { it.id },
            teamB = teamB.map { it.id },
            explanation = "Reparto automático por nivel: equipo A ${format(teamA.level())}, equipo B ${format(teamB.level())}.",
            generatedByAi = false,
        )
    }

    private fun List<TeamPlayer>.level(): Double = sumOf { it.estimatedLevel }

    private fun format(value: Double) = String.format(Locale.US, "%.1f", value)
}
