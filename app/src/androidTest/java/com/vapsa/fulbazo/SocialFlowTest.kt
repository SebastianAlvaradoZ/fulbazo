package com.vapsa.fulbazo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
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
import com.vapsa.fulbazo.ui.map.MAP_MATCHES_LIST_TAG
import java.time.LocalDate
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas de punta a punta: crear partido, amigos e invitaciones.
 */
@RunWith(AndroidJUnit4::class)
class SocialFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = grantAppPermissions()

    private fun waitForText(text: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Desplaza la lista hasta el elemento (LazyColumn solo dibuja lo visible). */
    private fun showInList(text: String) {
        composeRule.onNode(hasTestTag(MAP_MATCHES_LIST_TAG)).performScrollToNode(hasText(text))
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }

    private fun loginAsDemo() {
        composeRule.onNode(hasText("Correo electrónico") and hasSetTextAction())
            .performTextInput("demo@fulbazo.com")
        composeRule.onNode(hasText("Contraseña") and hasSetTextAction()).performTextInput("demo123")
        composeRule.onNodeWithText("Iniciar sesión").performClick()
        waitForText("Partidos cercanos")
    }

    @Test
    fun map_showsNearbyMatchesAndCreateButton() {
        loginAsDemo()

        composeRule.onNodeWithText("Losa Deportiva Umacollo").performScrollTo().assertIsDisplayed()
        // Partido de jose.crack: ninguna otra prueba se une a él.
        showInList("Anfitrión: jose.crack")
        composeRule.onNodeWithText("Crear partido", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun createMatch_withInvitedFriend_appearsAsHostOnMap() {
        loginAsDemo()
        composeRule.onNodeWithText("Crear partido", useUnmergedTree = true).performClick()
        waitForText("Serás el anfitrión")

        composeRule.onNodeWithText("Losa").performClick()
        composeRule.onNodeWithText("Losa Deportiva Sachaca · Sachaca").performClick()

        // Mañana; si cae en el mes siguiente, avanza el calendario.
        val tomorrow = LocalDate.now().plusDays(1)
        composeRule.onNodeWithText("Fecha").performClick()
        if (tomorrow.monthValue != LocalDate.now().monthValue) {
            composeRule.onNodeWithContentDescription("Change to next month").performClick()
        }
        // Cada día se describe con la fecha completa, p. ej. "Wednesday, November 11, 2026"
        // (el emulador está en inglés).
        val day = hasText(" ${tomorrow.dayOfMonth}, ${tomorrow.year}", substring = true) and hasClickAction()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(day).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onAllNodes(day)[0].performClick()
        composeRule.onNodeWithText("Aceptar").performClick()

        composeRule.onNodeWithText("Hora").performClick()
        composeRule.onNodeWithText("Aceptar").performClick() // 18:00 por defecto

        composeRule.onNodeWithText("carlitos10").performScrollTo().performClick()
        waitForText("Se invitará a 1 jugadores")

        // El título también dice "Crear partido"; el botón es el que se puede tocar.
        composeRule.onNode(hasText("Crear partido") and hasClickAction()).performScrollTo().performClick()

        // Vuelve al Mapa cuando termina de guardarse.
        waitForText("Partidos cercanos")
        // La tarjeta de Sachaca debe indicar que soy el anfitrión (puede haber otros partidos míos).
        val sachacaCard = hasText("Losa Deportiva Sachaca") and hasText("Eres el anfitrión")
        composeRule.onNode(hasTestTag(MAP_MATCHES_LIST_TAG)).performScrollToNode(sachacaCard)
        composeRule.onNode(sachacaCard).assertIsDisplayed()
    }

    @Test
    fun friends_searchAndSendRequest() {
        loginAsDemo()
        composeRule.onNodeWithText("Amigos").performClick()
        waitForText("Solicitudes (1)")

        composeRule.onNode(hasSetTextAction()).performTextInput("vale")
        waitForText("vale9")
        composeRule.onNodeWithText("Agregar").performClick()

        waitForText("Enviada")
    }

    @Test
    fun simulatedInvitation_canBeAcceptedFromInbox() {
        loginAsDemo()
        // El simulador envía la invitación unos segundos después de iniciar sesión.
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodes(hasText("1") and !hasClickAction(), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Ver invitaciones").performClick()

        waitForText("carlitos10 te invitó a un partido")
        composeRule.onNodeWithText("Aceptar").performClick()

        waitForText("Aceptaste")
    }
}
