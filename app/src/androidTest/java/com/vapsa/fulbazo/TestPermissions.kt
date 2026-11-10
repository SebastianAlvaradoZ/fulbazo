package com.vapsa.fulbazo

import android.Manifest
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Concede el permiso de notificaciones para que el diálogo del sistema no
 * aparezca durante las pruebas (Android 13+).
 */
fun grantNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.uiAutomation.grantRuntimePermission(
        instrumentation.targetContext.packageName,
        Manifest.permission.POST_NOTIFICATIONS,
    )
}
