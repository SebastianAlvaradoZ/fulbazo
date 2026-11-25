import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Lee MAPS_API_KEY de local.properties (que no se sube a git). Si no está, usa el
 * valor de local.defaults.properties, y la app muestra un aviso en vez del mapa.
 */
val localProperties = Properties().apply {
    listOf("local.defaults.properties", "local.properties")
        .map(rootProject::file)
        .filter { it.exists() }
        .forEach { file -> file.inputStream().use(::load) }
}
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY", "DEFAULT_API_KEY").trim()

/**
 * Token de depuración de App Check (solo builds debug). Se genera en la consola de
 * Firebase y se guarda en local.properties, que no se sube a git.
 */
val appCheckDebugToken: String = localProperties.getProperty("APP_CHECK_DEBUG_TOKEN", "").trim()

/**
 * Dirección de Firebase Realtime Database, de donde se descargan las losas.
 * Se toma de COURTS_BASE_URL en local.properties o, si no está, de
 * app/google-services.json (firebase_url). No es secreta, pero así no queda fija en el código.
 */
val courtsBaseUrl: String = localProperties.getProperty("COURTS_BASE_URL", "").trim().ifEmpty {
    val servicesJson = file("google-services.json")
    if (servicesJson.exists()) {
        Regex("\"firebase_url\"\\s*:\\s*\"([^\"]+)\"").find(servicesJson.readText())?.groupValues?.get(1).orEmpty()
    } else {
        ""
    }
}.let { if (it.isEmpty() || it.endsWith("/")) it else "$it/" }

android {
    namespace = "com.vapsa.fulbazo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.vapsa.fulbazo"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // Usa HiltTestApplication para que las pruebas cambien Firebase por los repositorios falsos.
        testInstrumentationRunner = "com.vapsa.fulbazo.HiltTestRunner"

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField(
            "boolean",
            "HAS_MAPS_API_KEY",
            (mapsApiKey.isNotBlank() && mapsApiKey != "DEFAULT_API_KEY").toString(),
        )
        buildConfigField("String", "COURTS_BASE_URL", "\"$courtsBaseUrl\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "APP_CHECK_DEBUG_TOKEN", "\"$appCheckDebugToken\"")
        }
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        // android.util.Log y similares devuelven valores vacíos en las pruebas unitarias.
        unitTests.isReturnDefaultValues = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.maps.compose)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.ai)
    // App Check: Play Integrity en release; en debug (emulador) un token de depuración.
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    // Widget de la pantalla de inicio: Glance, refresco periódico y último estado guardado.
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.datastore.preferences)
    debugImplementation(libs.firebase.appcheck.debug)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.org.json)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}