package com.vapsa.fulbazo.ui.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.vapsa.fulbazo.R

/**
 * Abre el Photo Picker del sistema (no necesita permisos de almacenamiento) y
 * entrega el Uri de la foto elegida. Devuelve la función que lo abre.
 */
@Composable
fun rememberPhotoPicker(onPicked: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            // Conserva el acceso a la foto aunque se reinicie la app.
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            onPicked(uri.toString())
        }
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

/** Avatar con los botones para elegir, cambiar o quitar la foto de perfil. */
@Composable
fun ProfilePhotoSection(
    photoUri: String?,
    enabled: Boolean,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        UserAvatar(
            photoUri = photoUri,
            contentDescription = stringResource(R.string.register_photo),
            modifier = Modifier.clickable(enabled = enabled, onClick = onPickPhoto),
        )
        Row {
            TextButton(onClick = onPickPhoto, enabled = enabled) {
                Text(
                    stringResource(
                        if (photoUri == null) R.string.register_add_photo else R.string.register_change_photo,
                    ),
                )
            }
            if (photoUri != null) {
                TextButton(onClick = onRemovePhoto, enabled = enabled) {
                    Text(stringResource(R.string.register_remove_photo))
                }
            }
        }
    }
}
