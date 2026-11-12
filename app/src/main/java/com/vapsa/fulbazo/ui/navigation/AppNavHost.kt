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
import com.vapsa.fulbazo.ui.match.create.CreateMatchRoute
import com.vapsa.fulbazo.ui.match.detail.MatchDetailRoute
import com.vapsa.fulbazo.ui.match.mine.MyMatchesRoute
import com.vapsa.fulbazo.ui.player.PlayerProfileRoute
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
            MapRoute(
                onCreateMatch = { navController.navigate(AppRoutes.CREATE_MATCH) },
                onOpenMatch = { matchId -> navController.navigate(AppRoutes.matchDetail(matchId)) },
            )
        }
        composable(AppRoutes.MATCH) {
            MyMatchesRoute(
                onOpenMatch = { matchId -> navController.navigate(AppRoutes.matchDetail(matchId)) },
                onCreateMatch = { navController.navigate(AppRoutes.CREATE_MATCH) },
                onExploreMap = {
                    navController.navigate(AppRoutes.MAP) {
                        popUpTo(AppRoutes.MAP) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
        composable(AppRoutes.FRIENDS) {
            FriendsRoute(
                onOpenGroup = { groupId -> navController.navigate(AppRoutes.groupEditor(groupId)) },
                onOpenPlayer = { playerId -> navController.navigate(AppRoutes.player(playerId)) },
            )
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
        composable(
            route = AppRoutes.MATCH_DETAIL,
            arguments = listOf(navArgument(AppRoutes.MATCH_ID_ARG) { type = NavType.StringType }),
        ) { entry ->
            MatchDetailRoute(
                matchId = requireNotNull(entry.arguments?.getString(AppRoutes.MATCH_ID_ARG)),
                onNavigateBack = { navController.popBackStack() },
                onOpenPlayer = { playerId -> navController.navigate(AppRoutes.player(playerId)) },
            )
        }
        composable(
            route = AppRoutes.PLAYER,
            arguments = listOf(navArgument(AppRoutes.PLAYER_ID_ARG) { type = NavType.StringType }),
        ) { entry ->
            PlayerProfileRoute(
                playerId = requireNotNull(entry.arguments?.getString(AppRoutes.PLAYER_ID_ARG)),
                onNavigateBack = { navController.popBackStack() },
                onOpenOwnProfile = {
                    // Mi propio perfil vive en la pestaña Perfil.
                    navController.popBackStack()
                    navController.navigate(AppRoutes.PROFILE) {
                        popUpTo(AppRoutes.MAP) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
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
