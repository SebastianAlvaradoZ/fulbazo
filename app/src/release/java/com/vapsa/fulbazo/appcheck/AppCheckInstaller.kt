package com.vapsa.fulbazo.appcheck

import android.content.Context
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * App Check en builds release: Play Integrity comprueba que las solicitudes a
 * Firebase (Gemini incluido) vienen de la app original instalada desde Play.
 */
object AppCheckInstaller {

    @Suppress("UNUSED_PARAMETER")
    fun install(context: Context) {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
    }
}
