package com.vapsa.fulbazo

import android.app.Application
import com.vapsa.fulbazo.appcheck.AppCheckInstaller
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Punto de entrada de la app y raíz del grafo de Hilt. Activa App Check y
 * arranca los trabajos que viven mientras la app está abierta ([AppBackgroundJobs]).
 */
@HiltAndroidApp
class FulbazoApplication : Application() {

    @Inject lateinit var backgroundJobs: AppBackgroundJobs

    override fun onCreate() {
        // Antes de crear cualquier cliente de Firebase (FirebaseApp ya se inició solo).
        AppCheckInstaller.install(this)
        super.onCreate()
        backgroundJobs.start()
    }
}
