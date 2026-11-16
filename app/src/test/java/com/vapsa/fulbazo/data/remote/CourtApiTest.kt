package com.vapsa.fulbazo.data.remote

import com.vapsa.fulbazo.di.DataSourceModule
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Retrofit leyendo el formato de Firebase Realtime Database desde un servidor de prueba. */
class CourtApiTest {

    private val server = MockWebServer()
    private lateinit var api: CourtApi

    @Before
    fun setUp() {
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(DataSourceModule.provideJson().asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CourtApi::class.java)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun parsesCourtsKeyedById_andIgnoresUnknownFields() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "losa-cayma": {"nombre": "Losa Deportiva Cayma", "distrito": "Cayma", "lat": -16.372,
                                 "lng": -71.545, "direccion": "Plaza de Cayma", "iluminacion": false,
                                 "campoNuevo": "se ignora"},
                  "losa-incompleta": {"nombre": "Sin coordenadas"}
                }
                """.trimIndent(),
            ),
        )

        val courts = api.getCourts()

        assertEquals("/losas.json", server.takeRequest().path)
        assertEquals("Losa Deportiva Cayma", courts.getValue("losa-cayma").name)
        assertEquals(-71.545, courts.getValue("losa-cayma").longitude!!, 0.0)
        assertNull(courts.getValue("losa-incompleta").latitude)
    }

    /** Nodo vacío: Firebase responde `null`; el repositorio lo trata como error. */
    @Test(expected = SerializationException::class)
    fun emptyNode_throws() = runTest {
        server.enqueue(MockResponse().setBody("null"))

        api.getCourts()
    }

    @Test(expected = HttpException::class)
    fun permissionDenied_throws() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error": "Permission denied"}"""))

        api.getCourts()
    }
}
