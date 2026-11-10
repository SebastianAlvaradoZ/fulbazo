package com.vapsa.fulbazo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.vapsa.fulbazo.ui.auth.login.LoginRoute
import com.vapsa.fulbazo.ui.auth.register.RegisterRoute
import com.vapsa.fulbazo.ui.map.MapScreen
import com.vapsa.fulbazo.ui.match.MatchesScreen
import com.vapsa.fulbazo.ui.profile.ProfileRoute
import com.vapsa.fulbazo.ui.stats.MyStatsRoute

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = AppRoutes.LOGIN,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(AppRoutes.LOGIN) {
            LoginRoute(
                onLoggedIn = navController::navigateToHome,
                onNavigateToRegister = { navController.navigate(AppRoutes.REGISTER) },
            )
        }
        composable(AppRoutes.REGISTER) {
            RegisterRoute(
                onRegistered = navController::navigateToHome,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(AppRoutes.MAP) { MapScreen() }
        composable(AppRoutes.MATCH) { MatchesScreen() }
        composable(AppRoutes.PROFILE) {
            ProfileRoute(onEditStats = { navController.navigate(AppRoutes.MY_STATS) })
        }
        composable(AppRoutes.MY_STATS) {
            MyStatsRoute(onNavigateBack = { navController.popBackStack() })
        }
    }
}

/**
 * Va a la pantalla principal y borra Login/Registro del historial, para que el
 * botón Atrás no regrese a ellas.
 */
private fun NavHostController.navigateToHome() {
    navigate(AppRoutes.MAP) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
