package com.vapsa.fulbazo

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Ejecuta las pruebas instrumentadas con [HiltTestApplication], para que los
 * módulos de prueba (como [FakeRepositoryModule]) reemplacen a Firebase.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
