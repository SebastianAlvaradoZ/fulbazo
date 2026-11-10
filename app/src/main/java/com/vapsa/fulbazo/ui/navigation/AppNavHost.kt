package com.vapsa.fulbazo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vapsa.fulbazo.ui.auth.login.LoginRoute
import com.vapsa.fulbazo.ui.auth.register.RegisterRoute
import com.vapsa.fulbazo.ui.friends.FriendsRoute
import com.vapsa.fulbazo.ui.friends.group.GroupEditorRoute
import com.vapsa.fulbazo.ui.invitations.InvitationsRoute
import com.vapsa.fulbazo.ui.map.MapRoute
import com.vapsa.fulbazo.ui.match.MatchesScreen
import com.vapsa.fulbazo.ui.match.create.CreateMatchRoute
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
        composable(AppRoutes.MAP) {
            MapRoute(onCreateMatch = { navController.navigate(AppRoutes.CREATE_MATCH) })
        }
        composable(AppRoutes.MATCH) { MatchesScreen() }
        composable(AppRoutes.FRIENDS) {
            FriendsRoute(onOpenGroup = { groupId -> navController.navigate(AppRoutes.groupEditor(groupId)) })
        }
        composable(AppRoutes.PROFILE) {
            ProfileRoute(onEditStats = { navController.navigate(AppRoutes.MY_STATS) })
        }
        composable(AppRoutes.MY_STATS) {
            MyStatsRoute(onNavigateBack = { navController.popBackStack() })
        }
        composable(AppRoutes.CREATE_MATCH) {
            CreateMatchRoute(
                onNavigateBack = { navController.popBackStack() },
                onCreated = { navController.popBackStack() },
            )
        }
        composable(AppRoutes.INVITATIONS) {
            InvitationsRoute(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = AppRoutes.GROUP_EDITOR,
            arguments = listOf(
                navArgument(AppRoutes.GROUP_ID_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            GroupEditorRoute(
                groupId = entry.arguments?.getString(AppRoutes.GROUP_ID_ARG),
                onDone = { navController.popBackStack() },
            )
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
