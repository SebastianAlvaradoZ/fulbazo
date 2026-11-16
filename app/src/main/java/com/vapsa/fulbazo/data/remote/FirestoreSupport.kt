package com.vapsa.fulbazo.data.remote

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.LocalDateTime
import java.time.ZoneId

/** Nombres de las colecciones de Firestore (ver `firestore.rules`). */
object Collections {
    const val USERS = "usuarios"
    const val PRIVATE = "privado"
    const val PRIVATE_DOC = "datos"
    const val NICKNAMES = "apodos"
    const val MATCHES = "partidos"
    const val JOIN_REQUESTS = "solicitudes"
    const val INVITATIONS = "invitaciones"
    const val FRIEND_REQUESTS = "solicitudesAmistad"
    const val FRIENDSHIPS = "amistades"
    const val GROUPS = "grupos"
    const val STATS = "estadisticas"
    const val RATINGS = "calificaciones"
    const val COMMENTS = "comentarios"
    const val REPORTS = "reportes"
    const val AI_SUMMARIES = "resumenesIA"
}

/**
 * Todos los jugadores están en Arequipa: las fechas se guardan con la hora de
 * Perú, aunque el teléfono tenga otra zona horaria configurada.
 */
private val PERU = ZoneId.of("America/Lima")

fun LocalDateTime.toTimestamp(): Timestamp = Timestamp(atZone(PERU).toInstant())

fun Timestamp.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(toInstant(), PERU)

/** Escucha la consulta en tiempo real. Al cancelar el Flow se quita el listener. */
fun Query.snapshotFlow(): Flow<QuerySnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) close(error) else if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

/** Escucha el documento en tiempo real. */
fun DocumentReference.snapshotFlow(): Flow<DocumentSnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) close(error) else if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

/** Lee un enum guardado con su nombre; si el valor es desconocido usa [default]. */
inline fun <reified E : Enum<E>> DocumentSnapshot.getEnum(field: String, default: E): E =
    getString(field)?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

/** Ejecuta una escritura y devuelve el error como [Result] (sin tragarse la cancelación). */
inline fun <T> firestoreResult(tag: String, block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Log.w(tag, "Error de Firestore", e)
    Result.failure(e)
}

/** Id de un par de jugadores sin importar el orden (`a_b` con a < b). */
fun pairId(a: String, b: String): String = if (a < b) "${a}_$b" else "${b}_$a"
