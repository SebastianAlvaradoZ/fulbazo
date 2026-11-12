package com.vapsa.fulbazo.di

import com.vapsa.fulbazo.data.repository.DemoInvitationSimulator
import com.vapsa.fulbazo.data.repository.DemoJoinRequestSimulator
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.data.repository.FakeCourtRepository
import com.vapsa.fulbazo.data.repository.FakeFriendRepository
import com.vapsa.fulbazo.data.repository.FakeGroupRepository
import com.vapsa.fulbazo.data.repository.FakeInvitationRepository
import com.vapsa.fulbazo.data.repository.FakeJoinRequestRepository
import com.vapsa.fulbazo.data.repository.FakeMatchRepository
import com.vapsa.fulbazo.data.repository.FakePlayerRepository
import com.vapsa.fulbazo.data.repository.FakeStatsRepository
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.CourtRepository
import com.vapsa.fulbazo.domain.repository.FriendRepository
import com.vapsa.fulbazo.domain.repository.GroupRepository
import com.vapsa.fulbazo.domain.repository.InvitationRepository
import com.vapsa.fulbazo.domain.repository.JoinRequestRepository
import com.vapsa.fulbazo.domain.repository.MatchRepository
import com.vapsa.fulbazo.domain.repository.PlayerRepository
import com.vapsa.fulbazo.domain.repository.RatingRepository
import com.vapsa.fulbazo.domain.repository.StatsRepository

/**
 * Contenedor simple de dependencias compartidas por toda la app.
 *
 * Cumple el papel que luego tendrán los módulos de Hilt: los ViewModels reciben
 * las dependencias por constructor, así que migrar solo cambia este archivo y
 * las factories.
 */
object AppContainer {
    val authRepository: AuthRepository by lazy { FakeAuthRepository() }
    /** Una sola instancia: las calificaciones recalculan las estadísticas "Cómo lo ven". */
    private val fakeStatsRepository by lazy { FakeStatsRepository() }
    val statsRepository: StatsRepository get() = fakeStatsRepository
    val ratingRepository: RatingRepository get() = fakeStatsRepository
    val courtRepository: CourtRepository by lazy { FakeCourtRepository() }
    val matchRepository: MatchRepository by lazy { FakeMatchRepository() }
    val playerRepository: PlayerRepository by lazy { FakePlayerRepository(authRepository) }
    val friendRepository: FriendRepository by lazy { FakeFriendRepository() }
    val groupRepository: GroupRepository by lazy { FakeGroupRepository() }
    val invitationRepository: InvitationRepository by lazy { FakeInvitationRepository() }
    val joinRequestRepository: JoinRequestRepository by lazy { FakeJoinRequestRepository() }

    /** Simula invitaciones entrantes mientras no haya backend. */
    val demoInvitationSimulator by lazy {
        DemoInvitationSimulator(authRepository, invitationRepository)
    }

    /** Simula que otros jugadores piden unirse a tus partidos mientras no haya backend. */
    val demoJoinRequestSimulator by lazy {
        DemoJoinRequestSimulator(authRepository, matchRepository, joinRequestRepository)
    }
}
