package com.vapsa.fulbazo.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.ui.theme.Poppins

/**
 * Avatar circular. Sin foto muestra las iniciales del jugador sobre un color que
 * sale de su nombre (cada jugador tiene siempre el mismo); sin nombre, un icono.
 *
 * @param name nombre completo o apodo, para las iniciales.
 */
@Composable
fun UserAvatar(
    photoUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    name: String? = null,
) {
    val initials = name?.let(::initialsOf)
    val colors = initials?.let { avatarColors(name) }
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = colors?.first ?: MaterialTheme.colorScheme.secondaryContainer,
        contentColor = colors?.second ?: MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        when {
            photoUri != null -> {
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
            initials != null -> Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initials,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (size.value * 0.38f).sp,
                )
            }
            else -> AvatarPlaceholder(size)
        }
    }
}

/** "Carlos Huamán" → "CH"; "carlitos10" → "CA". */
internal fun initialsOf(name: String): String? {
    val words = name.trim().split(Regex("[\\s._-]+")).filter { it.any(Char::isLetter) }
    if (words.isEmpty()) return null
    val letters = if (words.size >= 2) {
        "${words[0].first(Char::isLetter)}${words[1].first(Char::isLetter)}"
    } else {
        words[0].filter(Char::isLetter).take(2)
    }
    return letters.uppercase()
}

/** Fondo y texto del avatar; todos con contraste AA y dentro de la paleta de la app. */
private val AVATAR_COLORS = listOf(
    Color(0xFFB6F2AF) to Color(0xFF002204), // verde
    Color(0xFFFFDCBE) to Color(0xFF2C1600), // naranja
    Color(0xFFBDE9FF) to Color(0xFF001F2A), // celeste
    Color(0xFFE8DDFF) to Color(0xFF21005D), // lila
    Color(0xFFFFD9E2) to Color(0xFF3E001D), // rosa
    Color(0xFFF0E6A8) to Color(0xFF211B00), // amarillo
)

private fun avatarColors(name: String): Pair<Color, Color> =
    AVATAR_COLORS[Math.floorMod(name.trim().lowercase().hashCode(), AVATAR_COLORS.size)]

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
