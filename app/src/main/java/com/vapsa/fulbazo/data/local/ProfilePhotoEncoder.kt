package com.vapsa.fulbazo.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Convierte la foto elegida en el teléfono (`content://...`) en un JPEG pequeño
 * dentro de un `data:` URI. Así la foto se guarda en el documento de Firestore,
 * la ven los demás jugadores y no hace falta Firebase Storage. Coil sabe dibujar
 * `data:` URIs.
 */
@Singleton
class ProfilePhotoEncoder @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Devuelve la foto lista para guardar, o `null` si no hay foto o no se pudo leer.
     * Una foto que ya está codificada (o es una URL) se devuelve igual.
     */
    suspend fun encode(photoUri: String?): String? = withContext(Dispatchers.IO) {
        when {
            photoUri == null -> null
            photoUri.startsWith(DATA_PREFIX) || photoUri.startsWith("http") -> photoUri
            else -> runCatching { DATA_PREFIX + Base64.encodeToString(compress(Uri.parse(photoUri)), Base64.NO_WRAP) }
                .getOrNull()
        }
    }

    private fun compress(uri: Uri): ByteArray {
        val resolver = context.contentResolver
        // Primero solo las medidas, para no cargar en memoria una foto de 12 MP.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE_PX) sample *= 2

        val decoded = resolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("No se pudo leer la foto")
        val scale = MAX_SIDE_PX.toFloat() / max(decoded.width, decoded.height)
        val bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
        } else {
            decoded
        }
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
    }

    private companion object {
        const val DATA_PREFIX = "data:image/jpeg;base64,"

        /** 256 px de lado: unos 15–25 KB, muy por debajo del límite de 1 MB por documento. */
        const val MAX_SIDE_PX = 256
        const val JPEG_QUALITY = 80
    }
}
