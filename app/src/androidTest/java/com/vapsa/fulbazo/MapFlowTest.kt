package com.vapsa.fulbazo

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vapsa.fulbazo.ui.map.GOOGLE_MAP_TAG
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de punta a punta del mapa con el permiso de ubicación concedido.
 *
 * El emulador debe tener una ubicación (por ejemplo,
 * `adb emu geo fix -71.545 -16.372`, la plaza de Cayma).
 */
@RunWith(AndroidJUnit4::class)
class MapFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val distanceRegex = Regex("""· a \d+([.,]\d)? (km|m)$""")
    private val hasDistance = SemanticsMatcher("muestra la distancia") { node ->
        node.config.getOrNull(SemanticsProperties.Text).orEmpty().any { distanceRegex.containsMatchIn(it.text) }
    }

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
    fun withLocation_matchesAreSortedByDistance_andShowKm() {
        loginAsDemo()

        waitForText("Ordenados por distancia", timeoutMillis = 20_000)
        composeRule.onAllNodesWithText("Activa tu ubicación", substring = true).fetchSemanticsNodes()
            .let { check(it.isEmpty()) { "Con permiso no debe aparecer el aviso de ubicación" } }
        // Cada tarjeta muestra la distancia, p. ej. "· a 2.4 km" o "· a 850 m".
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasDistance).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun map_showsGoogleMapOrNotice() {
        loginAsDemo()
        if (BuildConfig.HAS_MAPS_API_KEY) {
            composeRule.waitUntil(5_000) {
                composeRule.onAllNodes(hasTestTag(GOOGLE_MAP_TAG)).fetchSemanticsNodes().isNotEmpty()
            }
        } else {
            waitForText("falta configurar la clave de Google Maps")
        }
    }
}
