package com.vapsa.fulbazo.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vapsa.fulbazo.R

/** Indicador de carga centrado. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/**
 * Mensaje para listas vacías con una ilustración (un arco esperando la pelota).
 * [iconRes] agrega un ícono pequeño que dice de qué es la lista (partidos, amigos…).
 */
@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier, iconRes: Int? = null) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Image(
                painter = painterResource(R.drawable.ill_empty),
                contentDescription = null,
                modifier = Modifier.size(width = 160.dp, height = 120.dp),
            )
            iconRes?.let {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(it), contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        Text(
            text = message,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Mensaje de error con botón opcional para reintentar. */
@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.ill_error),
            contentDescription = null,
            modifier = Modifier.size(width = 160.dp, height = 120.dp),
        )
        Text(text = message, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        onRetry?.let {
            Button(onClick = it) { Text(stringResource(R.string.action_retry)) }
        }
    }
}

/** Fase de una pantalla que carga datos. */
enum class ScreenPhase { LOADING, ERROR, EMPTY, CONTENT }

/**
 * Cambia entre carga, error, vacío y contenido con un fundido suave en lugar
 * de reemplazar la pantalla de golpe.
 */
@Composable
fun PhaseCrossfade(
    phase: ScreenPhase,
    modifier: Modifier = Modifier,
    content: @Composable (ScreenPhase) -> Unit,
) {
    Crossfade(targetState = phase, modifier = modifier, label = "screenPhase", content = content)
}
