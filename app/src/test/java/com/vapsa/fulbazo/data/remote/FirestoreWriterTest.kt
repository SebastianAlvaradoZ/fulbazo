package com.vapsa.fulbazo.data.remote

import com.vapsa.fulbazo.domain.repository.ConnectivityMonitor
import com.vapsa.fulbazo.domain.repository.NoConnectionException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreWriterTest {

    private val network = object : ConnectivityMonitor {
        override val isOnline = MutableStateFlow(true)
    }
    private val writer = FirestoreWriter(network)

    @Test
    fun online_runsTheWrite() = runTest {
        assertEquals(Result.success(42), writer.write("test") { 42 })
    }

    @Test
    fun offline_failsAtOnce_withoutWriting() = runTest {
        network.isOnline.value = false
        var wrote = false

        val result = writer.write("test") { wrote = true }

        assertTrue(result.exceptionOrNull() is NoConnectionException)
        assertFalse(wrote)
    }

    /** Firestore sin respuesta: no se queda "cargando" para siempre. */
    @Test
    fun serverNeverAnswers_timesOut() = runTest {
        val result = writer.write("test") { awaitCancellation() }

        assertTrue(result.exceptionOrNull() is NoConnectionException)
    }

    @Test
    fun otherErrors_arePassedThrough() = runTest {
        val result = writer.write("test") { error("Permiso denegado") }

        assertEquals("Permiso denegado", result.exceptionOrNull()?.message)
    }
}
