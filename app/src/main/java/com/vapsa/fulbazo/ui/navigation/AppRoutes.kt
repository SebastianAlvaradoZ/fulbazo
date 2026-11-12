package com.vapsa.fulbazo.ui.navigation

/**
 * Rutas de navegación de la aplicación.
 */
object AppRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAP = "map"
    const val MATCH = "match"
    const val FRIENDS = "friends"
    const val PROFILE = "profile"
    const val MY_STATS = "my_stats"
    const val CREATE_MATCH = "create_match"
    const val INVITATIONS = "invitations"

    const val MATCH_ID_ARG = "matchId"
    const val MATCH_DETAIL = "match_detail/{$MATCH_ID_ARG}"

    fun matchDetail(matchId: String): String = "match_detail/$matchId"

    const val RATE = "rate/{$MATCH_ID_ARG}"

    fun rate(matchId: String): String = "rate/$matchId"

    const val PLAYER_ID_ARG = "playerId"
    const val PLAYER = "player/{$PLAYER_ID_ARG}"

    fun player(playerId: String): String = "player/$playerId"

    const val GROUP_ID_ARG = "groupId"
    const val GROUP_EDITOR = "group_editor?$GROUP_ID_ARG={$GROUP_ID_ARG}"

    /** Sin [groupId] abre el editor para crear un grupo nuevo. */
    fun groupEditor(groupId: String? = null): String =
        if (groupId == null) "group_editor" else "group_editor?$GROUP_ID_ARG=$groupId"
}
