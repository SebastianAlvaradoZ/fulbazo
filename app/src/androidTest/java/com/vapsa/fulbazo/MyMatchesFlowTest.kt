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
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de punta a punta de la pestaña Partidos ("Mis partidos").
 */
@RunWith(AndroidJUnit4::class)
class MyMatchesFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = grantAppPermissions()

    private fun waitForText(text: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun loginAndOpenMyMatches() {
        composeRule.onNode(hasText("Correo electrónico") and hasSetTextAction())
            .performTextInput("demo@fulbazo.com")
        composeRule.onNode(hasText("Contraseña") and hasSetTextAction()).performTextInput("demo123")
        composeRule.onNodeWithText("Iniciar sesión").performClick()
        waitForText("Partidos cercanos")
        composeRule.onNodeWithText("Partidos").performClick()
        waitForText("Historial (")
    }

    @Test
    fun history_showsPlayedMatches_andOpensFinishedDetail() {
        loginAndOpenMyMatches()
        composeRule.onNodeWithText("Historial (3)").performClick()

        val selvaCard = hasText("Losa del Parque Selva Alegre") and hasClickAction()
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(selvaCard)
        composeRule.onAllNodesWithText("Evaluar jugadores")[0].assertIsDisplayed()

        composeRule.onNode(selvaCard).performClick()
        waitForText("Este partido ya se jugó")
    }

    @Test
    fun requests_showPendingRequest_andCanCancelIt() {
        loginAndOpenMyMatches()
        composeRule.onNodeWithText("Solicitudes (", substring = true).performClick()

        waitForText("Losa Deportiva Cayma")
        composeRule.onNodeWithText("Pendiente").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar solicitud").performClick()

        waitForText("No has pedido unirte a ningún partido")
    }
}
