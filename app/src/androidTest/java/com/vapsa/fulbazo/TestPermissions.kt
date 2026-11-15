package com.vapsa.fulbazo

import android.Manifest
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Concede los permisos de notificaciones (Android 13+) y de ubicación para que
 * los diálogos del sistema no aparezcan durante las pruebas.
 *
 * No se pueden revocar desde una prueba: Android cierra la app al revocar un
 * permiso. El aviso de "ubicación desactivada" se prueba en [MapScreenTest].
 */
fun grantAppPermissions() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val packageName = instrumentation.targetContext.packageName
    val permissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }
    permissions.forEach { instrumentation.uiAutomation.grantRuntimePermission(packageName, it) }
}
