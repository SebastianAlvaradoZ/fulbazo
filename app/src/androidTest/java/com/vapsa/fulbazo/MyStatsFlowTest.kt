package com.vapsa.fulbazo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de punta a punta: Perfil → Mis estadísticas → guardar → Perfil actualizado.
 */
@RunWith(AndroidJUnit4::class)
class MyStatsFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun loginAsDemoAndOpenProfile() {
        composeRule.onNode(hasText("Correo electrónico") and hasSetTextAction())
            .performTextInput("demo@fulbazo.com")
        composeRule.onNode(hasText("Contraseña") and hasSetTextAction())
            .performTextInput("demo123")
        composeRule.onNodeWithText("Iniciar sesión").performClick()
        waitForText("Perfil")
        composeRule.onNodeWithText("Perfil").performClick()
        waitForText("Editar mis estadísticas")
    }

    @Test
    fun profile_showsNicknameAndBothStatsCards() {
        loginAsDemoAndOpenProfile()

        composeRule.onNodeWithText("demo").assertIsDisplayed()
        composeRule.onNodeWithText("Cómo me veo").assertIsDisplayed()
        composeRule.onNodeWithText("Cómo me ven").assertIsDisplayed()
        composeRule.onNodeWithText("6 calificaciones").assertIsDisplayed()
    }

    @Test
    fun becomingGoalkeeper_showsGoalkeeperSkills_andSavesToProfile() {
        loginAsDemoAndOpenProfile()
        composeRule.onNodeWithText("Editar mis estadísticas").performClick()
        waitForText("Habilidades de campo")

        composeRule.onNodeWithText("Habilidades de arquero").assertDoesNotExist()

        // Arquero como posición principal: aparecen sus habilidades.
        composeRule.onAllNodesWithText("Arquero")[0].performScrollTo().performClick()
        composeRule.onNodeWithText("Habilidades de arquero").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Guardar").performScrollTo().performClick()
        waitForText("Estadísticas guardadas")

        composeRule.onNodeWithContentDescription("Volver").performClick()
        waitForText("Cómo me veo")
        composeRule.onAllNodesWithText("Como arquero")[0].assertIsDisplayed()
    }
}
