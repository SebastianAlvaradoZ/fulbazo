import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
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
    debugImplementation(libs.firebase.appcheck.debug)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.org.json)
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