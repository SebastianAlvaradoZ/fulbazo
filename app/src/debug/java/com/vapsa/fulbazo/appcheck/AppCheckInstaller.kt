package com.vapsa.fulbazo.appcheck

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.vapsa.fulbazo.BuildConfig

/**
 * App Check en builds debug (emulador y Android Studio): el proveedor de
 * depuración, porque Play Integrity solo funciona con la app instalada desde Play.
 *
 * Si `APP_CHECK_DEBUG_TOKEN` está en local.properties, se usa ese token (el que se
 * registró en la consola), así sirve aunque se reinstale la app. Si no, Firebase
 * genera uno y lo escribe en el Logcat para registrarlo.
 */
object AppCheckInstaller {

    fun install(context: Context) {
        val app = FirebaseApp.getInstance()
        val token = BuildConfig.APP_CHECK_DEBUG_TOKEN
        if (token.isNotBlank()) {
            // El proveedor de depuración lee el token de estas preferencias.
            context.getSharedPreferences(PREFS_TEMPLATE.format(app.persistenceKey), Context.MODE_PRIVATE)
                .edit()
                .putString(DEBUG_SECRET_KEY, token)
                .apply()
        } else {
            Log.w(TAG, "Sin APP_CHECK_DEBUG_TOKEN: busca 'App Check debug token' en el Logcat y regístralo.")
        }
        FirebaseAppCheck.getInstance(app).installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }

    private const val TAG = "AppCheckInstaller"
    private const val PREFS_TEMPLATE = "com.google.firebase.appcheck.debug.store.%s"
    private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
}
