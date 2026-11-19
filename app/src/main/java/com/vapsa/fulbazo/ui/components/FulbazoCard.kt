package com.vapsa.fulbazo.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * Tarjeta de la app: blanca con una sombra suave sobre el fondo verdoso (en modo
 * oscuro, un gris verdoso un poco más claro que el fondo). Reemplaza a la
 * tarjeta gris de Material por defecto.
 */
@Composable
fun FulbazoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = cardColor(), contentColor = MaterialTheme.colorScheme.onSurface)
    val elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, colors = colors, elevation = elevation, content = content)
    } else {
        Card(modifier = modifier, colors = colors, elevation = elevation, content = content)
    }
}

@Composable
private fun cardColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.background.luminance() > 0.5f) scheme.surfaceContainerLowest else scheme.surfaceContainerHigh
}
