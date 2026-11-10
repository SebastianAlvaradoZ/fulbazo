package com.vapsa.fulbazo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.vapsa.fulbazo.ui.map.MapScreen
import com.vapsa.fulbazo.ui.match.MatchesScreen
import com.vapsa.fulbazo.ui.profile.ProfileScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = AppRoutes.MAP,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(AppRoutes.MAP) { MapScreen() }
        composable(AppRoutes.MATCH) { MatchesScreen() }
        composable(AppRoutes.PROFILE) { ProfileScreen() }
    }
}
