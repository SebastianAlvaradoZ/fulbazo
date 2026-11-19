package com.vapsa.fulbazo

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vapsa.fulbazo.ui.map.MAP_MATCHES_LIST_TAG
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Capturas para el README con los datos de prueba (usuario demo, sin Firebase).
 * Solo corre si se pide: `-Pandroid.testInstrumentationRunnerArguments.screenshots=claro`
 * (o `oscuro`, con el emulador en modo oscuro). Las imágenes quedan en
 * /sdcard/Android/media/com.vapsa.fulbazo/capturas/.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ScreenshotsTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val suffix: String? = InstrumentationRegistry.getArguments().getString("screenshots")

    @Before
    fun setUp() {
        assumeTrue("Solo con screenshots=claro|oscuro", suffix != null)
        hiltRule.inject()
        grantAppPermissions()
    }

    private fun waitForText(text: String, timeoutMillis: Long = 10_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun capture(name: String) {
        composeRule.waitForIdle()
        Thread.sleep(1_500)
        val dir = File(composeRule.activity.externalMediaDirs.first(), "capturas").apply { mkdirs() }
        File(dir, "${name}_$suffix.png").outputStream().use {
            composeRule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun captureMainScreens() {
        composeRule.onNode(hasText("Correo electrónico") and hasSetTextAction()).performTextInput("demo@fulbazo.com")
        composeRule.onNode(hasText("Contraseña") and hasSetTextAction()).performTextInput("demo123")
        composeRule.onNodeWithText("Iniciar sesión").performClick()
        waitForText("Partidos cercanos")
        Thread.sleep(9_000) // Que cargue el mapa.
        capture("mapa")

        composeRule.onNode(hasTestTag(MAP_MATCHES_LIST_TAG)).performScrollToNode(hasText("Losa Deportiva Umacollo"))
        composeRule.onNode(hasText("Losa Deportiva Umacollo") and hasClickAction()).performClick()
        waitForText("Detalle del partido")
        capture("detalle_partido")
        composeRule.activity.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        // La lista quedó desplazada: se espera la barra de navegación, que siempre se ve.
        waitForText("Amigos")
        composeRule.onNodeWithText("Amigos").performClick()
        waitForText("carlitos10")
        composeRule.onNode(hasText("carlitos10") and hasClickAction()).performClick()
        waitForText("Perfil del jugador")
        waitForText("Sus compañeros destacan")
        capture("perfil_jugador")
        composeRule.activity.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        composeRule.onNodeWithText("Perfil").performClick()
        waitForText("Editar perfil")
        capture("mi_perfil")
    }
}
