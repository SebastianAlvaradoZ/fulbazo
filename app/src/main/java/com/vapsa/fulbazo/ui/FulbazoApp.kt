package com.vapsa.fulbazo.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.ui.invitations.InvitationBadgeViewModel
import com.vapsa.fulbazo.ui.navigation.AppNavHost
import com.vapsa.fulbazo.ui.navigation.AppRoutes
import com.vapsa.fulbazo.ui.navigation.TopLevelDestination
import com.vapsa.fulbazo.ui.theme.OnBrand
import com.vapsa.fulbazo.ui.theme.brandGradient

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
    badgeViewModel: InvitationBadgeViewModel = hiltViewModel(),
    connectivityViewModel: ConnectivityViewModel = hiltViewModel(),
) {
    val isOnline by connectivityViewModel.isOnline.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentTopLevel = TopLevelDestination.entries.firstOrNull { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }
    val isLoggedIn = currentTopLevel != null

    // Pantallas internas cuya barra superior también lleva el degradado verde.
    val brandHeaderRoute = currentDestination?.route in setOf(AppRoutes.MATCH_DETAIL, AppRoutes.PLAYER)

    // Íconos claros en la barra de estado mientras se ve una barra verde.
    StatusBarIcons(lightIcons = isLoggedIn || brandHeaderRoute || isSystemInDarkTheme())
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
        // Las pantallas internas tienen su propia barra y se encargan de la barra de
        // estado; así su color (por ejemplo, el degradado verde) llega hasta arriba.
        contentWindowInsets = if (currentTopLevel != null) ScaffoldDefaults.contentWindowInsets else WindowInsets(0, 0, 0, 0),
        topBar = {
            if (currentTopLevel != null) {
                // Barra con el degradado verde de la marca y el logo.
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.ic_fulbazo_logo),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(currentTopLevel.labelRes))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = OnBrand,
                        actionIconContentColor = OnBrand,
                    ),
                    modifier = Modifier.background(brandGradient()),
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
        Column(
            Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            OfflineBanner(visible = !isOnline)
            AppNavHost(navController = navController, modifier = Modifier.weight(1f))
        }
    }
}

/** Cambia el color de los íconos de la barra de estado (hora, batería…). */
@Composable
private fun StatusBarIcons(lightIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !lightIcons
    }
}

/** Aviso discreto arriba de todas las pantallas cuando no hay internet. */
@Composable
private fun OfflineBanner(visible: Boolean) {
    AnimatedVisibility(visible = visible, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(OFFLINE_BANNER_TAG),
        ) {
            Text(
                stringResource(R.string.offline_banner),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
    }
}

const val OFFLINE_BANNER_TAG = "offline_banner"

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
