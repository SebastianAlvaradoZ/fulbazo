package com.vapsa.fulbazo.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.vapsa.fulbazo.domain.ai.AiPrompts
import com.vapsa.fulbazo.domain.ai.TeamSplitParser
import com.vapsa.fulbazo.domain.model.CommunityStats
import com.vapsa.fulbazo.domain.model.PlayerComment
import com.vapsa.fulbazo.domain.model.TeamPlayer
import com.vapsa.fulbazo.domain.model.TeamSplit
import com.vapsa.fulbazo.domain.repository.AiRepository
import com.vapsa.fulbazo.domain.repository.ConnectivityMonitor
import com.vapsa.fulbazo.domain.repository.NoConnectionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini con Firebase AI Logic (Gemini Developer API). La clave la guarda
 * Firebase: la app no tiene ninguna clave de Gemini en el código.
 */
@Singleton
class GeminiAiRepository @Inject constructor(
    private val connectivity: ConnectivityMonitor,
) : AiRepository {

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

    /**
     * Llama a Gemini y devuelve el error como [Result]. Si el servidor está
     * saturado (pasa a veces con picos de uso), reintenta un par de veces.
     */
    private suspend fun <T> callGemini(block: suspend () -> T): Result<T> {
        // Sin internet ni se intenta: la pantalla muestra "Sin conexión" con Reintentar.
        if (!connectivity.isOnline.value) return Result.failure(NoConnectionException())
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                return Result.success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: ServerException) {
                val busy = e.message.orEmpty().contains("high demand", ignoreCase = true)
                if (!busy || attempt == MAX_ATTEMPTS - 1) {
                    Log.w(TAG, "Error de Gemini", e)
                    return Result.failure(e)
                }
                delay(RETRY_DELAY_MILLIS * (attempt + 1))
            } catch (e: IOException) {
                // Se cortó la red a mitad de la llamada (o no hay DNS).
                Log.w(TAG, "Gemini sin conexión", e)
                return Result.failure(NoConnectionException())
            } catch (e: Exception) {
                Log.w(TAG, "Error de Gemini", e)
                return Result.failure(if (e.cause is IOException) NoConnectionException() else e)
            }
        }
        error("Sin intentos")
    }

    private companion object {
        const val TAG = "GeminiAiRepository"
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MILLIS = 2_000L

        /**
         * Modelo flash ligero con cuota en la capa gratuita de la Gemini Developer API.
         * gemini-2.5-flash ya no está disponible para proyectos nuevos y los flash
         * completos agotan la cuota gratuita enseguida.
         */
        const val MODEL = "gemini-flash-lite-latest"
    }
}
