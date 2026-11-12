package com.vapsa.fulbazo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.SkillScale
import kotlin.math.roundToInt

/**
 * Deslizador de una habilidad en la escala de 1 a 5, con su nombre y el
 * valor actual ("4 / 5").
 */
@Composable
fun SkillSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.stats_value, value),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = SkillScale.MIN.toFloat()..SkillScale.MAX.toFloat(),
            // 3 pasos intermedios: 1, 2, 3, 4, 5.
            steps = SkillScale.MAX - SkillScale.MIN - 1,
            enabled = enabled,
        )
    }
}
