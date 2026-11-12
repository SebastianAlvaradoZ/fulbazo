package com.vapsa.fulbazo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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
import java.time.LocalDate

/**
 * Prueba de punta a punta del detalle de partido y las solicitudes para unirse.
 */
@RunWith(AndroidJUnit4::class)
class MatchDetailFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = grantNotificationPermission()

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

    /** Desplaza la lista del mapa hasta la tarjeta y la toca. */
    private fun openMatchCard(text: String) {
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
        composeRule.onNode(hasText(text) and hasClickAction()).performClick()
        waitForText("Detalle del partido")
    }

    @Test
    fun player_canRequestToJoinAndCancel() {
        loginAsDemo()
        // Partido de jose.crack: ninguna otra prueba se une a él.
        openMatchCard("Losa Deportiva Mariano Melgar")

        composeRule.onNodeWithText("Jugadores (1/8)", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Solicitar unirme").performClick()
        waitForText("Solicitud enviada.")

        composeRule.onNodeWithText("Cancelar solicitud").performClick()
        waitForText("Solicitar unirme")
    }

    @Test
    fun host_receivesSimulatedRequest_andAcceptsIt() {
        loginAsDemo()
        createMatchAt("Losa Deportiva Bustamante · José Luis Bustamante y Rivero")
        openMatchCard("Losa Deportiva Bustamante")

        // El simulador envía dos solicitudes unos segundos después de crear el partido.
        waitForText("Solicitudes (2)", timeoutMillis = 15_000)
        composeRule.onNodeWithText("rodri").assertIsDisplayed()
        composeRule.onAllNodes(hasText("Aceptar") and hasClickAction())[0].performClick()

        waitForText("Solicitudes (1)")
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Jugadores (2/10)"))
        composeRule.onNodeWithText("Jugadores (2/10)").assertIsDisplayed()
    }

    private fun createMatchAt(courtLabel: String) {
        composeRule.onNodeWithText("Crear partido", useUnmergedTree = true).performClick()
        waitForText("Serás el anfitrión")
        composeRule.onNodeWithText("Losa").performClick()
        composeRule.onNodeWithText(courtLabel).performClick()

        val tomorrow = LocalDate.now().plusDays(1)
        composeRule.onNodeWithText("Fecha").performClick()
        if (tomorrow.monthValue != LocalDate.now().monthValue) {
            composeRule.onNodeWithContentDescription("Change to next month").performClick()
        }
        // El emulador está en inglés: "Wednesday, November 11, 2026".
        val day = hasText(" ${tomorrow.dayOfMonth}, ${tomorrow.year}", substring = true) and hasClickAction()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(day).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onAllNodes(day)[0].performClick()
        composeRule.onNodeWithText("Aceptar").performClick()
        composeRule.onNodeWithText("Hora").performClick()
        composeRule.onNodeWithText("Aceptar").performClick()

        composeRule.onNode(hasText("Crear partido") and hasClickAction()).performScrollTo().performClick()
        waitForText("Partidos cercanos")
    }
}
