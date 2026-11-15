package com.vapsa.fulbazo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.ui.invitations.InvitationBadgeViewModel
import com.vapsa.fulbazo.ui.navigation.AppNavHost
import com.vapsa.fulbazo.ui.navigation.AppRoutes
import com.vapsa.fulbazo.ui.navigation.TopLevelDestination

/**
 * Contenedor principal: TopAppBar, NavigationBar y el grafo de navegación.
 *
 * @param openInvitationsRequested `true` cuando se tocó una notificación de invitación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FulbazoApp(
    openInvitationsRequested: Boolean = false,
    onInvitationsOpened: () -> Unit = {},
    badgeViewModel: InvitationBadgeViewModel = viewModel(factory = InvitationBadgeViewModel.Factory),
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentTopLevel = TopLevelDestination.entries.firstOrNull { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }
    val isLoggedIn = currentTopLevel != null
    val pendingInvitations by badgeViewModel.pendingCount.collectAsStateWithLifecycle()

    NotificationPermissionRequest(enabled = isLoggedIn)

    // Al tocar la notificación se abre la bandeja (solo si hay sesión).
    LaunchedEffect(openInvitationsRequested, isLoggedIn) {
        if (openInvitationsRequested && isLoggedIn) {
            navController.navigate(AppRoutes.INVITATIONS) { launchSingleTop = true }
            onInvitationsOpened()
        }
    }

    Scaffold(
        topBar = {
            if (currentTopLevel != null) {
                TopAppBar(
                    title = { Text(stringResource(currentTopLevel.labelRes)) },
                    actions = {
                        IconButton(onClick = { navController.navigate(AppRoutes.INVITATIONS) }) {
                            BadgedBox(
                                badge = {
                                    // El contador aparece con una pequeña animación de escala.
                                    // Versión general: el espacio del contador es una fila (RowScope).
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = pendingInvitations > 0,
                                        enter = scaleIn() + fadeIn(),
                                        exit = scaleOut() + fadeOut(),
                                    ) {
                                        Badge { Text(pendingInvitations.toString()) }
                                    }
                                },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_notifications),
                                    contentDescription = stringResource(R.string.invitations_open),
                                )
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            // Login y Registro se muestran sin las barras principales.
            if (currentTopLevel != null) {
                NavigationBar {
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

/**
 * En Android 13+ las notificaciones requieren permiso. Se pide una vez,
 * después de iniciar sesión.
 */
@Composable
private fun NotificationPermissionRequest(enabled: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var alreadyAsked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        alreadyAsked = true
    }
    LaunchedEffect(enabled) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (enabled && !granted && !alreadyAsked) {
            alreadyAsked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
