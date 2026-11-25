package com.vapsa.fulbazo.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Guarda lo último que se supo y redibuja los widgets de la pantalla de inicio. */
interface NextMatchStore {

    /** Si hay al menos un widget puesto; sin widgets no se gasta red ni batería. */
    fun hasWidgets(): Boolean

    suspend fun current(): NextMatchState?

    /** Guarda el estado y redibuja los widgets. */
    suspend fun save(state: NextMatchState)
}

@Singleton
class GlanceNextMatchStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : NextMatchStore {

    override fun hasWidgets(): Boolean =
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, NextMatchWidgetReceiver::class.java))
            .isNotEmpty()

    override suspend fun current(): NextMatchState? = context.nextMatchDataStore.data.first()

    override suspend fun save(state: NextMatchState) {
        context.nextMatchDataStore.updateData { state }
        NextMatchWidget().updateAll(context)
    }
}

/** `null` mientras el widget nunca se haya actualizado. */
internal val Context.nextMatchDataStore: DataStore<NextMatchState?> by dataStore(
    fileName = "widget_proximo_partido.json",
    serializer = NextMatchStateSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler { null },
)

private object NextMatchStateSerializer : Serializer<NextMatchState?> {
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue: NextMatchState? = null

    override suspend fun readFrom(input: InputStream): NextMatchState? = try {
        json.decodeFromString(NextMatchState.serializer().nullable, input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        Log.w("NextMatchStore", "El estado guardado del widget no se pudo leer", e)
        throw CorruptionException("Estado del widget ilegible", e)
    }

    override suspend fun writeTo(t: NextMatchState?, output: OutputStream) {
        output.write(json.encodeToString(NextMatchState.serializer().nullable, t).encodeToByteArray())
    }
}
