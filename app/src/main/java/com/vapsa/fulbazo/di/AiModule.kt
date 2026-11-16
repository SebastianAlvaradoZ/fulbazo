package com.vapsa.fulbazo.di

import com.vapsa.fulbazo.data.repository.GeminiAiRepository
import com.vapsa.fulbazo.domain.repository.AiRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Gemini con Firebase AI Logic. Está en su propio módulo para que las pruebas
 * instrumentadas lo cambien por [com.vapsa.fulbazo.data.repository.FakeAiRepository].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindAiRepository(repository: GeminiAiRepository): AiRepository
}
