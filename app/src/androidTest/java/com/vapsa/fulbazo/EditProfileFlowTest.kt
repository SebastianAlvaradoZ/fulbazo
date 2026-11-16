package com.vapsa.fulbazo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Prueba de punta a punta: editar el perfil y cerrar sesión.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EditProfileFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var backgroundJobs: AppBackgroundJobs

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

    private fun loginAndOpenProfile() {
        composeRule.onNode(hasText("Correo electrónico") and hasSetTextAction())
            .performTextInput("demo@fulbazo.com")
        composeRule.onNode(hasText("Contraseña") and hasSetTextAction()).performTextInput("demo123")
        composeRule.onNodeWithText("Iniciar sesión").performClick()
        waitForText("Partidos cercanos")
        composeRule.onNodeWithText("Perfil").performClick()
        waitForText("Editar perfil")
    }

    @Test
    fun editDistrict_showsChangeInProfile() {
        loginAndOpenProfile()
        composeRule.onNodeWithText("Editar perfil").performScrollTo().performClick()
        waitForText("Guardar cambios")

        composeRule.onNodeWithText("Distrito").performScrollTo().performClick()
        // Sachaca está más abajo en la lista de distritos.
        composeRule.onNodeWithText("Sachaca").performScrollTo().performClick()
        composeRule.onNodeWithText("Guardar cambios").performScrollTo().performClick()

        waitForText("Usuario Demo · Sachaca")
    }

    @Test
    fun logout_returnsToLogin_andBackDoesNotReturn() {
        loginAndOpenProfile()
        composeRule.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        waitForText("¿Cerrar sesión?")
        composeRule.onAllNodes(hasText("Cerrar sesión") and hasClickAction())
            .fetchSemanticsNodes().size.let { count ->
                // El botón del diálogo es el último que dice "Cerrar sesión".
                composeRule.onAllNodes(hasText("Cerrar sesión") and hasClickAction())[count - 1].performClick()
            }

        waitForText("Iniciar sesión")
        composeRule.onNodeWithText("¿No tienes cuenta?").assertIsDisplayed()

        // Atrás desde el Login cierra la app en lugar de volver al Perfil.
        // Se envía desde la app para no depender del foco de la ventana (una
        // notificación simulada puede quitárselo).
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitUntil(5_000) {
            composeRule.activityRule.scenario.state == Lifecycle.State.DESTROYED
        }
    }
}
