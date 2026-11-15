package com.vapsa.fulbazo.di

import com.vapsa.fulbazo.data.repository.FusedLocationRepository
import com.vapsa.fulbazo.domain.repository.LocationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Ubicación real del teléfono con Google Play services. */
@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    @Singleton
    abstract fun bindLocationRepository(repository: FusedLocationRepository): LocationRepository
}
