package com.vapsa.fulbazo.domain.ai

import com.vapsa.fulbazo.domain.model.TeamPlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamSplitParserTest {

    private val players = listOf("a", "b", "c", "d").map { TeamPlayer(it, "jugador-$it") }

    @Test
    fun validJson_isParsed() {
        val json = """{"equipoA": ["a", "c"], "equipoB": ["b", "d"], "explicacion": "Quedan parejos."}"""

        val split = TeamSplitParser.parse(json, players).getOrThrow()

        assertEquals(listOf("a", "c"), split.teamA)
        assertEquals(listOf("b", "d"), split.teamB)
        assertEquals("Quedan parejos.", split.explanation)
        assertTrue(split.generatedByAi)
    }

    @Test
    fun jsonInsideCodeFence_isAccepted() {
        val json = "```json\n{\"equipoA\": [\"a\", \"b\"], \"equipoB\": [\"c\", \"d\"], \"explicacion\": \"ok\"}\n```"

        assertTrue(TeamSplitParser.parse(json, players).isSuccess)
    }

    @Test
    fun missingPlayer_isRejected() {
        val json = """{"equipoA": ["a"], "equipoB": ["b", "c"], "explicacion": ""}"""

        assertTrue(TeamSplitParser.parse(json, players).isFailure)
    }

    @Test
    fun duplicatedPlayer_isRejected() {
        val json = """{"equipoA": ["a", "b"], "equipoB": ["b", "c", "d"], "explicacion": ""}"""

        assertTrue(TeamSplitParser.parse(json, players).isFailure)
    }

    @Test
    fun unknownId_isRejected() {
        val json = """{"equipoA": ["a", "b"], "equipoB": ["c", "intruso"], "explicacion": ""}"""

        assertTrue(TeamSplitParser.parse(json, players).isFailure)
    }

    @Test
    fun unbalancedTeams_areRejected() {
        val json = """{"equipoA": ["a", "b", "c"], "equipoB": ["d"], "explicacion": ""}"""

        assertTrue(TeamSplitParser.parse(json, players).isFailure)
    }

    @Test
    fun invalidJson_isRejected() {
        assertTrue(TeamSplitParser.parse("Aquí tienes los equipos: A y B", players).isFailure)
    }
}
