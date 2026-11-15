package com.vapsa.fulbazo

import android.app.Application
import com.vapsa.fulbazo.di.AppContainer

/** Punto de entrada de la app: entrega el contexto a las dependencias que lo necesitan. */
class FulbazoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}
