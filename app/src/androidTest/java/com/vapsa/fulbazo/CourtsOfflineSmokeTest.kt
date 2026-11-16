package com.vapsa.fulbazo

import android.util.Log
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vapsa.fulbazo.data.local.FulbazoDatabase
import com.vapsa.fulbazo.data.repository.OfflineFirstCourtRepository
import com.vapsa.fulbazo.di.DataSourceModule
import com.vapsa.fulbazo.domain.repository.CourtSyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba manual contra Realtime Database de verdad: descarga las losas, activa el
 * modo avión, comprueba que siguen las guardadas en Room y vuelve a conectar.
 * Hay que pedirla con `-Pandroid.testInstrumentationRunnerArguments.realCourts=true`.
 * Solo lee; no escribe nada en Firebase.
 */
@RunWith(AndroidJUnit4::class)
class CourtsOfflineSmokeTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var database: FulbazoDatabase
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Before
    fun setUp() {
        assumeTrue("Prueba manual: realCourts=true", InstrumentationRegistry.getArguments().getString("realCourts") == "true")
        database = Room.inMemoryDatabaseBuilder(instrumentation.targetContext, FulbazoDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            setAirplaneMode(false)
            database.close()
        }
        scope.cancel()
    }

    @Test
    fun downloadsCourts_keepsThemOffline_andRecovers() = runBlocking {
        val api = DataSourceModule.provideCourtApi(DataSourceModule.provideOkHttpClient(), DataSourceModule.provideJson())
        val repository = OfflineFirstCourtRepository(api, database.courtDao(), scope)

        // 1. Con internet: al abrir la lista se descarga y se guarda (como en el mapa).
        repository.observeCourts().first()
        assertEquals(CourtSyncStatus.UP_TO_DATE, repository.syncStatus.first { it != CourtSyncStatus.SYNCING })
        val online = repository.observeCourts().first()
        Log.i(TAG, "Con red: ${online.size} losas: ${online.joinToString { it.name }}")
        assertTrue(online.isNotEmpty())

        // 2. Modo avión: falla la descarga, pero siguen las losas guardadas.
        setAirplaneMode(true)
        val offlineResult = repository.refresh()
        val offline = repository.observeCourts().first()
        Log.i(TAG, "Modo avión: refresh=${offlineResult.isSuccess}, estado=${repository.syncStatus.value}, guardadas=${offline.size}")
        assertTrue(offlineResult.isFailure)
        assertEquals(CourtSyncStatus.OFFLINE, repository.syncStatus.value)
        assertEquals(online, offline)

        // 3. Vuelve la red: "Reintentar" actualiza otra vez.
        setAirplaneMode(false)
        waitForNetwork()
        assertTrue(repository.refresh().isSuccess)
        Log.i(TAG, "Reintentar con red: estado=${repository.syncStatus.value}")
        assertEquals(CourtSyncStatus.UP_TO_DATE, repository.syncStatus.value)
    }

    private fun setAirplaneMode(enabled: Boolean) {
        instrumentation.uiAutomation
            .executeShellCommand("cmd connectivity airplane-mode ${if (enabled) "enable" else "disable"}")
            .close()
        Thread.sleep(3_000)
    }

    /** Espera hasta 30 s a que Android vuelva a tener internet. */
    private fun waitForNetwork() {
        repeat(30) {
            val output = instrumentation.uiAutomation.executeShellCommand("dumpsys connectivity").let { fd ->
                android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().use { it.readText() }
            }
            if ("IS_VALIDATED" in output) return
            Thread.sleep(1_000)
        }
    }

    private companion object {
        const val TAG = "CourtsOfflineSmokeTest"
    }
}
