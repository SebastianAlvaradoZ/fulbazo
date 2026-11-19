package com.vapsa.fulbazo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vapsa.fulbazo.R

/** Poppins para títulos: redonda y deportiva. Incluida en la app (licencia OFL). */
val Poppins = FontFamily(
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

/** Inter para el texto: muy legible en pantallas pequeñas. Fuente variable (OFL). */
val Inter = FontFamily(
    interWeight(FontWeight.Normal),
    interWeight(FontWeight.Medium),
    interWeight(FontWeight.SemiBold),
    interWeight(FontWeight.Bold),
)

@OptIn(ExperimentalTextApi::class)
private fun interWeight(weight: FontWeight) = Font(
    R.font.inter,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private fun title(size: Int, line: Int, weight: FontWeight = FontWeight.SemiBold, tracking: Double = 0.0) = TextStyle(
    fontFamily = Poppins,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

private fun text(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Double = 0.0) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

/**
 * Escala tipográfica: títulos con Poppins (más peso y tamaño para marcar la
 * jerarquía) y textos y etiquetas con Inter.
 */
val Typography = Typography(
    displayLarge = title(52, 60, FontWeight.Bold, -0.5),
    displayMedium = title(44, 52, FontWeight.Bold),
    displaySmall = title(36, 44, FontWeight.Bold),
    headlineLarge = title(30, 38, FontWeight.Bold),
    headlineMedium = title(26, 34, FontWeight.Bold),
    headlineSmall = title(22, 30),
    titleLarge = title(20, 28),
    titleMedium = title(16, 24),
    titleSmall = title(14, 20, FontWeight.Medium),
    bodyLarge = text(16, 24),
    bodyMedium = text(14, 20),
    bodySmall = text(12, 16),
    labelLarge = text(14, 20, FontWeight.SemiBold, 0.1),
    labelMedium = text(12, 16, FontWeight.SemiBold, 0.3),
    labelSmall = text(11, 16, FontWeight.Medium, 0.4),
)
