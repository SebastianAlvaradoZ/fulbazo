package com.vapsa.fulbazo.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

/**
 * Transiciones entre pantallas:
 * - Entre pestañas de la barra inferior (y desde el Login): fundido suave.
 * - Al abrir una pantalla de detalle: entra deslizándose desde la derecha.
 * - Al volver: sale deslizándose hacia la derecha.
 */
private const val DURATION_MS = 300

private val topLevelRoutes = TopLevelDestination.entries.map { it.route }.toSet() +
    setOf(AppRoutes.LOGIN)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isBetweenTopLevel(): Boolean =
    initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes

val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    if (isBetweenTopLevel()) {
        fadeIn(tween(DURATION_MS))
    } else {
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(DURATION_MS)) +
            fadeIn(tween(DURATION_MS))
    }
}

val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    if (isBetweenTopLevel()) {
        fadeOut(tween(DURATION_MS))
    } else {
        // La pantalla de atrás se desplaza un poco, como en Material.
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Start,
            tween(DURATION_MS),
            targetOffset = { it / 4 },
        ) + fadeOut(tween(DURATION_MS))
    }
}

val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    if (isBetweenTopLevel()) {
        fadeIn(tween(DURATION_MS))
    } else {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.End,
            tween(DURATION_MS),
            initialOffset = { it / 4 },
        ) + fadeIn(tween(DURATION_MS))
    }
}

val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    if (isBetweenTopLevel()) {
        fadeOut(tween(DURATION_MS))
    } else {
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(DURATION_MS)) +
            fadeOut(tween(DURATION_MS))
    }
}
