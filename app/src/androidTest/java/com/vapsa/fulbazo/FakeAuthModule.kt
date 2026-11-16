package com.vapsa.fulbazo

import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.di.AuthModule
import com.vapsa.fulbazo.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * En las pruebas instrumentadas la sesión es falsa (usuario demo@fulbazo.com /
 * demo123): no dependen de internet ni crean cuentas en Firebase.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [AuthModule::class])
object FakeAuthModule {

    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository = FakeAuthRepository()
}
