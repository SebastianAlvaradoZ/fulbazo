package com.vapsa.fulbazo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vapsa.fulbazo.domain.model.MatchLevel
import com.vapsa.fulbazo.domain.model.NewMatch
import com.vapsa.fulbazo.domain.repository.JoinRequestRepository
import com.vapsa.fulbazo.domain.repository.MatchRepository
import com.vapsa.fulbazo.ui.map.MAP_MATCHES_LIST_TAG
import com.vapsa.fulbazo.ui.match.teams.TEAMS_RESULT_TAG
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comentarios, resumen con IA y equipos con IA, con la IA falsa de
 * [FakeRepositoryModule] (no llama a Gemini).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AiFeaturesFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var backgroundJobs: AppBackgroundJobs

    @Inject
    lateinit var matchRepository: MatchRepository

    @Inject
    lateinit var joinRequestRepository: JoinRequestRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        backgroundJobs.start()
        grantAppPermissions()
    }

    private fun waitForText(text: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun loginAsDemo() {
        composeRule.onNode(hasText("Correo electrónico") and hasSetTextAction())
            .performTextInput("demo@fulbazo.com")
        composeRule.onNode(hasText("Contraseña") and hasSetTextAction()).performTextInput("demo123")
        composeRule.onNodeWithText("Iniciar sesión").performClick()
        waitForText("Partidos cercanos")
    }

    @Test
    fun playerProfile_showsCommentsAndAiSummary() {
        loginAsDemo()
        composeRule.onNodeWithText("Amigos").performClick()
        waitForText("carlitos10")
        composeRule.onNode(hasText("carlitos10") and hasClickAction()).performClick()

        waitForText("Perfil del jugador")
        composeRule.onNodeWithText("Resumen generado por IA").performScrollTo().assertIsDisplayed()
        waitForText("Sus compañeros destacan a carlitos10")
        composeRule.onNodeWithText("Comentarios (3)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Llegó puntual y metió dos goles.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun host_seesAiSummaryOnRequest_andBuildsTeamsWithAi() {
        // Partido de demo con dos jugadores más y una solicitud de carlos.
        runBlocking {
            val match = matchRepository.createMatch(
                hostId = "demo",
                newMatch = NewMatch(
                    courtId = "losa-bustamante",
                    dateTime = LocalDate.now().plusDays(2).atTime(LocalTime.of(18, 0)),
                    slots = 10,
                    level = MatchLevel.ANY,
                ),
            ).getOrThrow()
            matchRepository.joinMatch(match.id, "maria")
            matchRepository.joinMatch(match.id, "pedro")
            joinRequestRepository.sendRequest(match.id, "carlos")
        }
        loginAsDemo()
        composeRule.onNode(hasTestTag(MAP_MATCHES_LIST_TAG)).performScrollToNode(hasText("Losa Deportiva Bustamante"))
        composeRule.onNode(hasText("Losa Deportiva Bustamante") and hasClickAction()).performClick()
        waitForText("Detalle del partido")

        // El anfitrión ve el resumen de IA de quien pide unirse.
        waitForText("Sus compañeros destacan a carlitos10")

        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Armar equipos con IA"))
        composeRule.onNodeWithText("Armar equipos con IA").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasTestTag(TEAMS_RESULT_TAG)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Equipos sugeridos por IA"))
        composeRule.onNodeWithText("Equipos sugeridos por IA").assertIsDisplayed()
    }
}
