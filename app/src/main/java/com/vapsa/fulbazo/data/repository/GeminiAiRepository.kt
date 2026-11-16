package com.vapsa.fulbazo.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.vapsa.fulbazo.domain.ai.AiPrompts
import com.vapsa.fulbazo.domain.ai.TeamSplitParser
import com.vapsa.fulbazo.domain.model.CommunityStats
import com.vapsa.fulbazo.domain.model.PlayerComment
import com.vapsa.fulbazo.domain.model.TeamPlayer
import com.vapsa.fulbazo.domain.model.TeamSplit
import com.vapsa.fulbazo.domain.repository.AiRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini con Firebase AI Logic (Gemini Developer API). La clave la guarda
 * Firebase: la app no tiene ninguna clave de Gemini en el código.
 */
@Singleton
class GeminiAiRepository @Inject constructor() : AiRepository {

    private val ai by lazy { Firebase.ai(backend = GenerativeBackend.googleAI()) }

    private val summaryModel: GenerativeModel by lazy {
        ai.generativeModel(
            modelName = MODEL,
            generationConfig = generationConfig { temperature = 0.3f },
            systemInstruction = content { text(AiPrompts.SUMMARY_SYSTEM) },
        )
    }

    /** Responde siempre con el JSON del esquema: así no hay que adivinar el formato. */
    private val teamsModel: GenerativeModel by lazy {
        ai.generativeModel(
            modelName = MODEL,
            generationConfig = generationConfig {
                temperature = 0.2f
                responseMimeType = "application/json"
                responseSchema = Schema.obj(
                    mapOf(
                        TeamSplitParser.TEAM_A to Schema.array(Schema.string()),
                        TeamSplitParser.TEAM_B to Schema.array(Schema.string()),
                        TeamSplitParser.EXPLANATION to Schema.string(),
                    ),
                )
            },
            systemInstruction = content { text(AiPrompts.TEAMS_SYSTEM) },
        )
    }

    override suspend fun summarizeReviews(
        nickname: String,
        comments: List<PlayerComment>,
        community: CommunityStats,
    ): Result<String> = callGemini {
        val text = summaryModel.generateContent(AiPrompts.summaryPrompt(nickname, comments, community)).text
        requireNotNull(text?.trim()?.takeIf { it.isNotEmpty() }) { "Gemini no devolvió texto" }
    }

    override suspend fun buildTeams(players: List<TeamPlayer>): Result<TeamSplit> = callGemini {
        val json = teamsModel.generateContent(AiPrompts.teamsPrompt(players)).text
            ?: error("Gemini no devolvió equipos")
        TeamSplitParser.parse(json, players).getOrThrow()
    }

    private inline fun <T> callGemini(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Error de Gemini", e)
        Result.failure(e)
    }

    private companion object {
        const val TAG = "GeminiAiRepository"

        /** Modelo rápido y estable, disponible en la capa gratuita de la Gemini Developer API. */
        const val MODEL = "gemini-2.5-flash"
    }
}
