package com.vapsa.fulbazo

import com.vapsa.fulbazo.data.repository.FakeAiRepository
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.data.repository.FakeCommentRepository
import com.vapsa.fulbazo.data.repository.FakeCourtRepository
import com.vapsa.fulbazo.data.repository.FakeFriendRepository
import com.vapsa.fulbazo.data.repository.FakeGroupRepository
import com.vapsa.fulbazo.data.repository.FakeInvitationRepository
import com.vapsa.fulbazo.data.repository.FakeJoinRequestRepository
import com.vapsa.fulbazo.data.repository.FakeMatchRepository
import com.vapsa.fulbazo.data.repository.FakePlayerRepository
import com.vapsa.fulbazo.data.repository.FakeStatsRepository
import com.vapsa.fulbazo.di.AiModule
import com.vapsa.fulbazo.di.AuthModule
import com.vapsa.fulbazo.di.ConnectivityModule
import com.vapsa.fulbazo.di.RepositoryModule
import com.vapsa.fulbazo.domain.repository.AiRepository
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.CommentRepository
import com.vapsa.fulbazo.domain.repository.ConnectivityMonitor
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
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * En las pruebas instrumentadas todos los datos son los Fake* en memoria (usuario
 * demo@fulbazo.com / demo123 con amigos, partidos y estadísticas de prueba): no
 * dependen de internet ni escriben en Firebase. La IA también es falsa (no llama
 * a Gemini). Cada prueba empieza con datos nuevos.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AuthModule::class, RepositoryModule::class, AiModule::class, ConnectivityModule::class],
)
object FakeRepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository = FakeAuthRepository()

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

    @Provides
    @Singleton
    fun provideCommentRepository(): CommentRepository = FakeCommentRepository()

    @Provides
    @Singleton
    fun provideAiRepository(): AiRepository = FakeAiRepository()

    /** Hay internet salvo que la prueba lo apague con [FakeConnectivityMonitor.isOnline]. */
    @Provides
    @Singleton
    fun provideFakeConnectivityMonitor(): FakeConnectivityMonitor = FakeConnectivityMonitor()

    @Provides
    fun provideConnectivityMonitor(monitor: FakeConnectivityMonitor): ConnectivityMonitor = monitor
}
