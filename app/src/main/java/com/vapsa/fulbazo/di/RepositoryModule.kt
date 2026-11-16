package com.vapsa.fulbazo.di

import com.vapsa.fulbazo.data.repository.FakeCourtRepository
import com.vapsa.fulbazo.data.repository.FirestoreCommentRepository
import com.vapsa.fulbazo.data.repository.FirestoreFriendRepository
import com.vapsa.fulbazo.data.repository.FirestoreGroupRepository
import com.vapsa.fulbazo.data.repository.FirestoreInvitationRepository
import com.vapsa.fulbazo.data.repository.FirestoreJoinRequestRepository
import com.vapsa.fulbazo.data.repository.FirestoreMatchRepository
import com.vapsa.fulbazo.data.repository.FirestorePlayerRepository
import com.vapsa.fulbazo.data.repository.FirestoreStatsRepository
import com.vapsa.fulbazo.domain.repository.CommentRepository
import com.vapsa.fulbazo.domain.repository.CourtRepository
import com.vapsa.fulbazo.domain.repository.FriendRepository
import com.vapsa.fulbazo.domain.repository.GroupRepository
import com.vapsa.fulbazo.domain.repository.InvitationRepository
import com.vapsa.fulbazo.domain.repository.JoinRequestRepository
import com.vapsa.fulbazo.domain.repository.MatchRepository
import com.vapsa.fulbazo.domain.repository.PlayerRepository
import com.vapsa.fulbazo.domain.repository.RatingRepository
import com.vapsa.fulbazo.domain.repository.StatsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repositorios de datos en Cloud Firestore (la autenticación está en [AuthModule]).
 * Las losas siguen siendo una lista local hasta que lleguen de Retrofit + Room.
 *
 * Las pruebas instrumentadas cambian este módulo por los repositorios Fake*.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPlayerRepository(repository: FirestorePlayerRepository): PlayerRepository

    @Binds
    @Singleton
    abstract fun bindMatchRepository(repository: FirestoreMatchRepository): MatchRepository

    @Binds
    @Singleton
    abstract fun bindJoinRequestRepository(repository: FirestoreJoinRequestRepository): JoinRequestRepository

    @Binds
    @Singleton
    abstract fun bindInvitationRepository(repository: FirestoreInvitationRepository): InvitationRepository

    @Binds
    @Singleton
    abstract fun bindFriendRepository(repository: FirestoreFriendRepository): FriendRepository

    @Binds
    @Singleton
    abstract fun bindGroupRepository(repository: FirestoreGroupRepository): GroupRepository

    /** Autoevaluación y calificaciones están en la misma clase. */
    @Binds
    @Singleton
    abstract fun bindStatsRepository(repository: FirestoreStatsRepository): StatsRepository

    @Binds
    @Singleton
    abstract fun bindRatingRepository(repository: FirestoreStatsRepository): RatingRepository

    @Binds
    @Singleton
    abstract fun bindCommentRepository(repository: FirestoreCommentRepository): CommentRepository

    companion object {
        @Provides
        @Singleton
        fun provideCourtRepository(): CourtRepository = FakeCourtRepository()
    }
}
