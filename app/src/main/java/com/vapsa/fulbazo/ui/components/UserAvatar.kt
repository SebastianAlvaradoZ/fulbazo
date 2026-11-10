package com.vapsa.fulbazo.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vapsa.fulbazo.R

/**
 * Avatar circular. Si no hay foto (o no se puede cargar) muestra un icono de persona.
 */
@Composable
fun UserAvatar(
    photoUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        if (photoUri == null) {
            AvatarPlaceholder(size)
        } else {
            val placeholder = painterResource(R.drawable.ic_profile)
            AsyncImage(
                model = photoUri,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                placeholder = placeholder,
                error = placeholder,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
            )
        }
    }
}

@Composable
private fun AvatarPlaceholder(size: Dp) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(R.drawable.ic_profile),
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .padding(size / 5),
        )
    }
}
