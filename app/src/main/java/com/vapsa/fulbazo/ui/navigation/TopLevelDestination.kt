package com.vapsa.fulbazo.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.vapsa.fulbazo.R

/**
 * Destinos principales que aparecen en la barra de navegación inferior.
 */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    MAP(AppRoutes.MAP, R.string.nav_map, R.drawable.ic_map),
    MATCHES(AppRoutes.MATCH, R.string.nav_matches, R.drawable.ic_matches),
    PROFILE(AppRoutes.PROFILE, R.string.nav_profile, R.drawable.ic_profile),
}
