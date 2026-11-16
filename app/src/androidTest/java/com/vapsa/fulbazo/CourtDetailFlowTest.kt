package com.vapsa.fulbazo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Prueba de punta a punta del detalle de losa desde el mapa.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CourtDetailFlowTest {

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

    private fun loginAsDemo() {
        composeRule.onNode(hasText("Correo electrónico") and hasSetTextAction())
            .performTextInput("demo@fulbazo.com")
        composeRule.onNode(hasText("Contraseña") and hasSetTextAction()).performTextInput("demo123")
        composeRule.onNodeWithText("Iniciar sesión").performClick()
        waitForText("Partidos cercanos")
    }

    @Test
    fun tappingCourtOnGridMap_opensSheet_andCreateMatchHerePreselectsCourt() {
        // Los marcadores de Google Maps no están en el árbol de Compose; con clave,
        // el detalle se prueba desde el filtro (prueba de abajo).
        assumeFalse(BuildConfig.HAS_MAPS_API_KEY)
        loginAsDemo()
        composeRule.onNode(hasContentDescription("Losa Deportiva Mariano Melgar", substring = true)).performClick()

        waitForText("Próximos partidos")
        composeRule.onNodeWithText("Sin iluminación").assertIsDisplayed()
        composeRule.onNodeWithText("Crear partido aquí").performScrollTo().performClick()

        waitForText("Serás el anfitrión")
        composeRule.onNodeWithText("Losa Deportiva Mariano Melgar · Mariano Melgar").assertIsDisplayed()
    }

    @Test
    fun courtFilter_showsOnlyItsMatches_andOpensCourtDetail() {
        loginAsDemo()
        composeRule.onNode(hasText("Losa Deportiva Cayma (", substring = true) and hasClickAction()).performClick()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Anfitrión: vale9").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithText("Anfitrión: carlitos10").fetchSemanticsNodes().isEmpty()
        }

        composeRule.onNodeWithText("Ver losa").performClick()
        waitForText("Próximos partidos")
        composeRule.onNodeWithText("Plaza de Cayma · Cayma").assertIsDisplayed()
    }
}
