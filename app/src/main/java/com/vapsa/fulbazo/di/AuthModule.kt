package com.vapsa.fulbazo.di

import com.vapsa.fulbazo.data.repository.FirebaseAuthRepository
import com.vapsa.fulbazo.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Autenticación con Firebase. Está en su propio módulo para que las pruebas
 * instrumentadas lo cambien por [com.vapsa.fulbazo.data.repository.FakeAuthRepository].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(repository: FirebaseAuthRepository): AuthRepository
}
