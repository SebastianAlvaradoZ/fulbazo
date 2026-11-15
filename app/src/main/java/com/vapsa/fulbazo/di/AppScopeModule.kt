package com.vapsa.fulbazo.di

import com.vapsa.fulbazo.data.repository.DemoInvitationSimulator
import com.vapsa.fulbazo.data.repository.DemoJoinRequestSimulator
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.InvitationRepository
import com.vapsa.fulbazo.domain.repository.JoinRequestRepository
import com.vapsa.fulbazo.domain.repository.MatchRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** Scope que vive lo mismo que la app, para trabajos que no dependen de una pantalla. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppScopeModule {

    /** SupervisorJob: si un trabajo falla, los demás siguen corriendo. */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Simula invitaciones entrantes mientras no haya backend. */
    @Provides
    @Singleton
    fun provideDemoInvitationSimulator(
        authRepository: AuthRepository,
        invitationRepository: InvitationRepository,
    ): DemoInvitationSimulator = DemoInvitationSimulator(authRepository, invitationRepository)

    /** Simula que otros jugadores piden unirse a tus partidos mientras no haya backend. */
    @Provides
    @Singleton
    fun provideDemoJoinRequestSimulator(
        authRepository: AuthRepository,
        matchRepository: MatchRepository,
        joinRequestRepository: JoinRequestRepository,
    ): DemoJoinRequestSimulator = DemoJoinRequestSimulator(authRepository, matchRepository, joinRequestRepository)
}
