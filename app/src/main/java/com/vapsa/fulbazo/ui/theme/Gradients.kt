package com.vapsa.fulbazo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Verde césped oscuro para el final del degradado. */
private val GrassDeep = Color(0xFF1B5E20)
private val GrassNight = Color(0xFF0F3D13)

/**
 * Degradado de la marca para la barra superior y las portadas: verde césped a
 * verde oscuro (en modo oscuro, más apagado para no encandilar). El texto encima
 * va en blanco (contraste AA).
 */
@Composable
fun brandGradient(): Brush {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return Brush.linearGradient(if (dark) listOf(GrassDeep, GrassNight) else listOf(Grass, GrassDeep))
}

/** Texto e íconos sobre el degradado de la marca. */
val OnBrand = Color.White
