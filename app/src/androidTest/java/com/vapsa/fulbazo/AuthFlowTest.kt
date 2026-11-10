package com.vapsa.fulbazo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
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
 * Prueba de punta a punta del flujo de acceso con el repositorio falso.
 */
@RunWith(AndroidJUnit4::class)
class AuthFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun typeInto(label: String, text: String) {
        composeRule.onNode(hasText(label) and hasSetTextAction())
            .performScrollTo()
            .performTextInput(text)
    }

    private fun clickRegisterButton() {
        composeRule.onNode(hasText("Crear cuenta") and hasClickAction())
            .performScrollTo()
            .performClick()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun wrongPassword_showsError() {
        typeInto("Correo electrónico", "demo@fulbazo.com")
        typeInto("Contraseña", "incorrecta")
        composeRule.onNodeWithText("Iniciar sesión").performClick()

        waitForText("Correo o contraseña incorrectos")
    }

    @Test
    fun demoUser_logsInAndSeesMap() {
        typeInto("Correo electrónico", "demo@fulbazo.com")
        typeInto("Contraseña", "demo123")
        composeRule.onNodeWithText("Iniciar sesión").performClick()

        waitForText("Partidos")
        composeRule.onNodeWithText("Perfil").assertIsDisplayed()
    }

    @Test
    fun emptyRegister_showsRequiredErrors() {
        composeRule.onNodeWithText("Regístrate").performClick()
        clickRegisterButton()

        composeRule.onAllNodesWithText("Este campo es obligatorio")
            .fetchSemanticsNodes()
            .let { check(it.size >= 5) { "Se esperaban errores de campos obligatorios" } }
    }

    @Test
    fun register_birthDateAndDistrictPickersWork() {
        composeRule.onNodeWithText("Regístrate").performClick()

        // El campo de fecha abre el calendario y "Cancelar" lo cierra.
        composeRule.onNodeWithText("Fecha de nacimiento").performScrollTo().performClick()
        composeRule.onNodeWithText("Aceptar").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.onNodeWithText("Aceptar").assertDoesNotExist()

        // El distrito elegido queda escrito en el campo.
        composeRule.onNodeWithText("Distrito").performScrollTo().performClick()
        composeRule.onNodeWithText("Cayma").performClick()
        composeRule.onNodeWithText("Cayma").assertIsDisplayed()
    }

    @Test
    fun register_backReturnsToLogin() {
        composeRule.onNodeWithText("Regístrate").performClick()
        composeRule.onNodeWithContentDescription("Volver").performClick()

        composeRule.onNodeWithText("Iniciar sesión").assertIsDisplayed()
    }
}
