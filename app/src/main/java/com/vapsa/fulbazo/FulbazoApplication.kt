package com.vapsa.fulbazo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Punto de entrada de la app y raíz del grafo de Hilt. Arranca los trabajos que
 * viven mientras la app está abierta ([AppBackgroundJobs]).
 */
@HiltAndroidApp
class FulbazoApplication : Application() {

    @Inject lateinit var backgroundJobs: AppBackgroundJobs

    override fun onCreate() {
        super.onCreate()
        backgroundJobs.start()
    }
}
