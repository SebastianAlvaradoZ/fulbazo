package com.vapsa.fulbazo.domain.model

/** Datos públicos de un jugador que pueden ver los demás. */
data class PlayerSummary(
    val id: String,
    val nickname: String,
    val fullName: String,
    val district: String,
    val photoUri: String? = null,
)

enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
}

data class FriendRequest(
    val id: String,
    val fromUserId: String,
    val toUserId: String,
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
)

/** Relación entre el usuario actual y otro jugador. */
enum class PlayerRelation {
    NONE,
    FRIEND,
    REQUEST_SENT,
    REQUEST_RECEIVED,
}

data class PlayerSearchResult(
    val player: PlayerSummary,
    val relation: PlayerRelation,
)

/** Grupo de amigos para invitar a varios a la vez. */
data class FriendGroup(
    val id: String,
    val ownerId: String,
    val name: String,
    val memberIds: List<String>,
) {
    companion object {
        const val MAX_NAME_LENGTH = 30
    }
}
