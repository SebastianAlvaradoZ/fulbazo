package com.vapsa.fulbazo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vapsa.fulbazo.domain.model.Court
import com.vapsa.fulbazo.ui.map.LocationPermission
import com.vapsa.fulbazo.ui.map.MapActions
import com.vapsa.fulbazo.ui.map.MapScreen
import com.vapsa.fulbazo.ui.map.MapUiState
import com.vapsa.fulbazo.ui.theme.FulbazoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Estados de la pantalla del mapa que no se pueden provocar en la app real
 * desde una prueba (permiso negado, falta la clave de Maps).
 */
@RunWith(AndroidJUnit4::class)
class MapScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val courts = listOf(Court("c1", "Losa Deportiva Cayma", "Cayma", -16.372, -71.545))

    private fun show(state: MapUiState, opensSettings: Boolean = false, onRequestLocation: () -> Unit = {}) {
        composeRule.setContent {
            FulbazoTheme {
                MapScreen(
                    uiState = state,
                    actions = MapActions(onRequestLocation = onRequestLocation),
                    locationRequestOpensSettings = opensSettings,
                )
            }
        }
    }

    @Test
    fun deniedLocation_showsBanner_andAsksAgain() {
        var requests = 0
        show(
            MapUiState(isLoading = false, courts = courts, locationPermission = LocationPermission.DENIED, mapsAvailable = false),
            onRequestLocation = { requests++ },
        )

        composeRule.onNodeWithText("Activa tu ubicación", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Ordenados por fecha").assertIsDisplayed()
        composeRule.onNodeWithText("Permitir ubicación").performClick()
        assertEquals(1, requests)
    }

    @Test
    fun permanentlyDenied_offersSettings() {
        show(
            MapUiState(isLoading = false, courts = courts, locationPermission = LocationPermission.DENIED, mapsAvailable = false),
            opensSettings = true,
        )
        composeRule.onNodeWithText("Abrir ajustes").assertIsDisplayed()
    }

    @Test
    fun grantedLocation_hidesBanner() {
        show(MapUiState(isLoading = false, courts = courts, locationPermission = LocationPermission.GRANTED, mapsAvailable = false))
        composeRule.onNodeWithText("Activa tu ubicación", substring = true).assertDoesNotExist()
    }

    @Test
    fun missingMapsKey_showsNoticeInsteadOfCrashing() {
        show(MapUiState(isLoading = false, courts = courts, mapsAvailable = false))
        composeRule.onNodeWithText("falta configurar la clave de Google Maps", substring = true).assertIsDisplayed()
    }
}
