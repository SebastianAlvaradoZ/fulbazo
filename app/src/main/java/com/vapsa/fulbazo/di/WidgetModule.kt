package com.vapsa.fulbazo.di

import com.vapsa.fulbazo.widget.GlanceNextMatchStore
import com.vapsa.fulbazo.widget.NextMatchStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Dónde guarda sus datos el widget de la pantalla de inicio. */
@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetModule {

    @Binds
    @Singleton
    abstract fun bindNextMatchStore(store: GlanceNextMatchStore): NextMatchStore
}
