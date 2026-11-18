package com.vapsa.fulbazo.di

import com.vapsa.fulbazo.data.remote.AndroidConnectivityMonitor
import com.vapsa.fulbazo.domain.repository.ConnectivityMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Estado de la red. Las pruebas instrumentadas lo cambian por uno falso. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityModule {

    @Binds
    @Singleton
    abstract fun bindConnectivityMonitor(monitor: AndroidConnectivityMonitor): ConnectivityMonitor
}
