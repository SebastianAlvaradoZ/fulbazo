package com.vapsa.fulbazo.domain.ai

import com.vapsa.fulbazo.domain.model.TeamPlayer
import com.vapsa.fulbazo.domain.model.TeamSplit
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

/**
 * Lee y valida el JSON de equipos que devuelve Gemini:
 * `{"equipoA": ["id", ...], "equipoB": ["id", ...], "explicacion": "..."}`.
 *
 * No se confía en la respuesta: si falta un jugador, hay uno repetido o un id
 * que no existe, se rechaza.
 */
object TeamSplitParser {

    const val TEAM_A = "equipoA"
    const val TEAM_B = "equipoB"
    const val EXPLANATION = "explicacion"

    fun parse(json: String, players: List<TeamPlayer>): Result<TeamSplit> = runCatching {
        val root = JSONObject(stripCodeFence(json))
        val teamA = root.getJSONArray(TEAM_A).toStringList()
        val teamB = root.getJSONArray(TEAM_B).toStringList()
        val explanation = root.optString(EXPLANATION).trim()

        val expected = players.map { it.id }.toSet()
        val all = teamA + teamB
        require(all.size == all.toSet().size) { "Hay jugadores repetidos" }
        require(all.toSet() == expected) { "Faltan jugadores o hay ids desconocidos" }
        require(teamA.isNotEmpty() && teamB.isNotEmpty()) { "Un equipo quedó vacío" }
        require(abs(teamA.size - teamB.size) <= 1) { "Los equipos no tienen el mismo tamaño" }

        TeamSplit(
            teamA = teamA,
            teamB = teamB,
            explanation = AiPrompts.clean(explanation, MAX_EXPLANATION),
            generatedByAi = true,
        )
    }

    /** A veces el modelo envuelve el JSON en ```json ... ```. */
    private fun stripCodeFence(text: String): String =
        text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

    private fun JSONArray.toStringList(): List<String> = List(length()) { getString(it) }

    private const val MAX_EXPLANATION = 300
}
