package com.vapsa.fulbazo.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vapsa.fulbazo.ui.navigation.AppNavHost
import com.vapsa.fulbazo.ui.navigation.AppRoutes
import com.vapsa.fulbazo.ui.navigation.TopLevelDestination

/**
 * Contenedor principal: TopAppBar, NavigationBar y el grafo de navegación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FulbazoApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentTopLevel = TopLevelDestination.entries.firstOrNull { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }

    Scaffold(
        topBar = {
            if (currentTopLevel != null) {
                TopAppBar(title = { Text(stringResource(currentTopLevel.labelRes)) })
            }
        },
        bottomBar = {
            // Login y Registro se muestran sin las barras principales.
            if (currentTopLevel != null) NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == currentTopLevel,
                        onClick = {
                            navController.navigate(destination.route) {
                                // Mapa es la raíz del historial después de iniciar sesión.
                                popUpTo(AppRoutes.MAP) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(destination.iconRes),
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }
}
