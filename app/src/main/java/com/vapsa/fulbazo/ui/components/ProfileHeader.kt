package com.vapsa.fulbazo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vapsa.fulbazo.ui.theme.Spacing
import com.vapsa.fulbazo.ui.theme.brandGradient

/**
 * Portada del perfil: una banda verde con el degradado de la marca y el avatar
 * encima, mitad sobre la banda y mitad fuera; debajo, el apodo y los datos.
 */
@Composable
fun ProfileHeader(
    nickname: String,
    subtitle: String,
    photoUri: String?,
    name: String?,
    modifier: Modifier = Modifier,
) {
    val avatarSize = 96.dp
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .background(brandGradient()),
            )
            UserAvatar(
                photoUri = photoUri,
                contentDescription = null,
                name = name,
                size = avatarSize,
                modifier = Modifier
                    .padding(top = 88.dp - avatarSize / 2)
                    .clip(CircleShape)
                    .border(4.dp, MaterialTheme.colorScheme.background, CircleShape),
            )
        }
        Text(
            text = nickname,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = Spacing.sm),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.md),
        )
    }
}
