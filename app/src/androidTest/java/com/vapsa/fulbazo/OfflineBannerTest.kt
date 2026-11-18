package com.vapsa.fulbazo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vapsa.fulbazo.ui.OFFLINE_BANNER_TAG
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** El aviso "Sin conexión" aparece y desaparece según la red (falsa). */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OfflineBannerTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var connectivity: FakeConnectivityMonitor

    @Before
    fun setUp() {
        hiltRule.inject()
        grantAppPermissions()
    }

    @Test
    fun banner_followsConnectivity() {
        composeRule.onNodeWithText("Iniciar sesión").assertIsDisplayed()
        composeRule.onNode(hasTestTag(OFFLINE_BANNER_TAG)).assertDoesNotExist()

        connectivity.isOnline.value = false
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasTestTag(OFFLINE_BANNER_TAG)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Sin conexión", substring = true).assertIsDisplayed()

        connectivity.isOnline.value = true
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasTestTag(OFFLINE_BANNER_TAG)).fetchSemanticsNodes().isEmpty()
        }
    }
}
