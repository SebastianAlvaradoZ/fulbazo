package com.vapsa.fulbazo.di

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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repositorios de la app. Por ahora son los Fake* en memoria; cuando llegue
 * Firebase solo cambia este módulo.
 *
 * Todos son @Singleton: los ViewModels comparten los mismos datos.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository = FakeAuthRepository()

    /** Una sola instancia: las calificaciones recalculan las estadísticas "Cómo lo ven". */
    @Provides
    @Singleton
    fun provideFakeStatsRepository(): FakeStatsRepository = FakeStatsRepository()

    @Provides
    fun provideStatsRepository(repository: FakeStatsRepository): StatsRepository = repository

    @Provides
    fun provideRatingRepository(repository: FakeStatsRepository): RatingRepository = repository

    @Provides
    @Singleton
    fun provideCourtRepository(): CourtRepository = FakeCourtRepository()

    @Provides
    @Singleton
    fun provideMatchRepository(): MatchRepository = FakeMatchRepository()

    @Provides
    @Singleton
    fun providePlayerRepository(authRepository: AuthRepository): PlayerRepository =
        FakePlayerRepository(authRepository)

    @Provides
    @Singleton
    fun provideFriendRepository(): FriendRepository = FakeFriendRepository()

    @Provides
    @Singleton
    fun provideGroupRepository(): GroupRepository = FakeGroupRepository()

    @Provides
    @Singleton
    fun provideInvitationRepository(): InvitationRepository = FakeInvitationRepository()

    @Provides
    @Singleton
    fun provideJoinRequestRepository(): JoinRequestRepository = FakeJoinRequestRepository()
}
