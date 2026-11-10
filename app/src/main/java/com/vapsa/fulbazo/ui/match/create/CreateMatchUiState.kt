package com.vapsa.fulbazo.ui.match.create

import com.vapsa.fulbazo.domain.model.Court
import com.vapsa.fulbazo.domain.model.FriendGroup
import com.vapsa.fulbazo.domain.model.MatchLevel
import com.vapsa.fulbazo.domain.model.NewMatch
import com.vapsa.fulbazo.domain.model.PlayerSummary
import java.time.LocalDate
import java.time.LocalTime

enum class CreateMatchError {
    COURT_REQUIRED,
    DATE_REQUIRED,
    TIME_REQUIRED,
    DATE_IN_PAST,
    DESCRIPTION_TOO_LONG,
}

/**
 * Estado del formulario "Crear partido". El usuario actual será el anfitrión.
 */
data class CreateMatchUiState(
    val isLoading: Boolean = true,
    val courts: List<Court> = emptyList(),
    val friends: List<PlayerSummary> = emptyList(),
    val groups: List<FriendGroup> = emptyList(),
    val courtId: String? = null,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val slots: Int = NewMatch.DEFAULT_SLOTS,
    val level: MatchLevel = MatchLevel.ANY,
    val description: String = "",
    val invitedFriendIds: Set<String> = emptySet(),
    val invitedGroupIds: Set<String> = emptySet(),
    val errors: Set<CreateMatchError> = emptySet(),
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val createdMatchId: String? = null,
) {
    val selectedCourt: Court? get() = courts.firstOrNull { it.id == courtId }

    val canDecreaseSlots: Boolean get() = slots > NewMatch.MIN_SLOTS
    val canIncreaseSlots: Boolean get() = slots < NewMatch.MAX_SLOTS

    /** Jugadores que recibirán invitación: amigos elegidos más miembros de los grupos. */
    val inviteeIds: Set<String>
        get() = invitedFriendIds + groups
            .filter { it.id in invitedGroupIds }
            .flatMap { it.memberIds }
}
