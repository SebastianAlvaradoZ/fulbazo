package com.vapsa.fulbazo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de punta a punta: evaluar a un compañero desde el historial.
 */
@RunWith(AndroidJUnit4::class)
class RatePlayersFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = grantAppPermissions()

    private fun waitForText(text: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun history_rateTeammateAsGoalkeeper() {
        composeRule.onNode(hasText("Correo electrónico") and hasSetTextAction())
            .performTextInput("demo@fulbazo.com")
        composeRule.onNode(hasText("Contraseña") and hasSetTextAction()).performTextInput("demo123")
        composeRule.onNodeWithText("Iniciar sesión").performClick()
        waitForText("Partidos cercanos")

        composeRule.onNodeWithText("Partidos").performClick()
        waitForText("Historial (")
        composeRule.onNodeWithText("Historial (3)").performClick()

        // El primer partido del historial es el de Selva Alegre (mari.gol, carlitos10 y vale9).
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Evaluar jugadores"))
        composeRule.onAllNodes(hasText("Evaluar jugadores") and hasClickAction())[0].performClick()
        waitForText("Evaluaste a ")

        composeRule.onNode(hasText("vale9")).assertIsDisplayed()
        composeRule.onAllNodes(hasText("Evaluar") and hasClickAction())[0].performClick()
        waitForText("Evaluar a ")

        composeRule.onNodeWithText("Arquero").performClick()
        waitForText("Atajadas")
        composeRule.onNodeWithText("Guardar evaluación").performScrollTo().performClick()

        waitForText("Evaluado como arquero")
    }
}
