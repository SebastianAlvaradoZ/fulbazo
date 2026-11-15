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
import com.vapsa.fulbazo.ui.map.MAP_MATCHES_LIST_TAG
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de punta a punta del perfil de otro jugador.
 */
@RunWith(AndroidJUnit4::class)
class PlayerProfileFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = grantAppPermissions()

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
    fun friendProfile_showsStatsAndMatchesTogether() {
        loginAsDemo()
        composeRule.onNodeWithText("Amigos").performClick()
        waitForText("carlitos10")
        composeRule.onNode(hasText("carlitos10") and hasClickAction()).performClick()

        waitForText("Perfil del jugador")
        composeRule.onNodeWithText("Partidos jugados juntos: 3").assertIsDisplayed()
        composeRule.onNodeWithText("Son amigos").assertIsDisplayed()
        composeRule.onNodeWithText("Cómo lo ven").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("8 calificaciones").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun searchResult_profile_canSendFriendRequest() {
        loginAsDemo()
        composeRule.onNodeWithText("Amigos").performClick()
        waitForText("Solicitudes (")
        composeRule.onNode(hasSetTextAction()).performTextInput("pedri")
        waitForText("pedrito")
        composeRule.onNode(hasText("pedrito") and hasClickAction()).performClick()

        waitForText("Perfil del jugador")
        composeRule.onNodeWithText("Agregar amigo").performClick()
        waitForText("Solicitud enviada")
    }

    @Test
    fun matchDetailPlayer_opensProfile_withGoalkeeperRatings() {
        loginAsDemo()
        composeRule.onNode(hasTestTag(MAP_MATCHES_LIST_TAG)).performScrollToNode(hasText("Losa Deportiva Umacollo"))
        composeRule.onNode(hasText("Losa Deportiva Umacollo") and hasClickAction()).performClick()
        waitForText("Detalle del partido")

        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("mari.gol"))
        composeRule.onNode(hasText("mari.gol") and hasClickAction()).performClick()

        waitForText("Perfil del jugador")
        composeRule.onNodeWithText("Cómo lo ven").performScrollTo()
        composeRule.onAllNodesWithText("Como arquero")[0].performScrollTo().assertIsDisplayed()
    }
}
