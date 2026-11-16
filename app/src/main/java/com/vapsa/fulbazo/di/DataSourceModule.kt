package com.vapsa.fulbazo.di

import android.content.Context
import androidx.room.Room
import com.vapsa.fulbazo.BuildConfig
import com.vapsa.fulbazo.data.local.CourtDao
import com.vapsa.fulbazo.data.local.FulbazoDatabase
import com.vapsa.fulbazo.data.remote.CourtApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** Red (Retrofit + OkHttp) y base de datos local (Room). */
@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    /** Sin dirección configurada, las solicitudes fallan y se usan las losas guardadas. */
    private const val MISSING_BASE_URL = "https://servidor-de-losas-no-configurado.invalid/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // Campos nuevos en el servidor no rompen la app.
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideCourtApi(client: OkHttpClient, json: Json): CourtApi = Retrofit.Builder()
        .baseUrl(BuildConfig.COURTS_BASE_URL.ifBlank { MISSING_BASE_URL })
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(CourtApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FulbazoDatabase =
        Room.databaseBuilder(context, FulbazoDatabase::class.java, FulbazoDatabase.NAME).build()

    @Provides
    fun provideCourtDao(database: FulbazoDatabase): CourtDao = database.courtDao()
}
