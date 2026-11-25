package com.vapsa.fulbazo

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vapsa.fulbazo.data.repository.FakeMatchRepository
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.ui.navigation.LaunchTarget
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/** Cómo abre la app: con la sesión guardada y desde los botones del widget. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LaunchFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    @Inject
    lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        grantAppPermissions()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun saveSession() = runBlocking { authRepository.login("demo@fulbazo.com", "demo123") }

    private fun launch(target: LaunchTarget) = ActivityScenario.launch<MainActivity>(
        LaunchTarget.intent(ApplicationProvider.getApplicationContext(), target),
    )

    @Test
    fun savedSession_opensTheMap() {
        saveSession()

        launch(LaunchTarget.Home).use { waitForText("Partidos cercanos") }
    }

    @Test
    fun withoutSession_opensTheLogin() {
        launch(LaunchTarget.Home).use { waitForText("Iniciar sesión") }
    }

    @Test
    fun widgetMatch_opensItsDetail_andBackGoesToTheMap() {
        saveSession()

        launch(LaunchTarget.MatchDetail(FakeMatchRepository.SEED_MATCH_ID)).use { scenario ->
            waitForText("Detalle del partido")
            waitForText("Losa Deportiva Umacollo")

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitForText("Partidos cercanos")
        }
    }

    @Test
    fun widgetCreateMatch_opensTheForm() {
        saveSession()

        launch(LaunchTarget.CreateMatch).use { waitForText("Serás el anfitrión") }
    }

    @Test
    fun widgetInvitations_opensTheInbox() {
        saveSession()

        launch(LaunchTarget.Invitations).use { waitForText("No tienes invitaciones por ahora") }
    }

    @Test
    fun withoutSession_theScreenWaitsForTheLogin() {
        launch(LaunchTarget.CreateMatch).use {
            waitForText("Iniciar sesión")
            composeRule.onNode(hasText("Correo electrónico") and hasSetTextAction())
                .performTextInput("demo@fulbazo.com")
            composeRule.onNode(hasText("Contraseña") and hasSetTextAction()).performTextInput("demo123")
            composeRule.onNodeWithText("Iniciar sesión").performClick()

            waitForText("Serás el anfitrión")
        }
    }
}
