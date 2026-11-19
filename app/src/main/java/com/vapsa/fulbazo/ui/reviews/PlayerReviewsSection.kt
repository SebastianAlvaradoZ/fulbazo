package com.vapsa.fulbazo.ui.reviews

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.ai.PlayerReviewSummarizer
import com.vapsa.fulbazo.ui.components.UserAvatar
import java.time.format.DateTimeFormatter
import java.util.Locale

const val AI_SUMMARY_TAG = "ai_summary"

private val commentDateFormatter = DateTimeFormatter.ofPattern("d 'de' MMM yyyy", Locale.forLanguageTag("es-PE"))

/** Resumen de IA y comentarios de un jugador, para su perfil. */
@Composable
fun PlayerReviewsSection(
    playerId: String,
    modifier: Modifier = Modifier,
    viewModel: PlayerReviewsViewModel = hiltViewModel<PlayerReviewsViewModel, PlayerReviewsViewModel.Factory>(
        key = "reviews-$playerId",
    ) { factory -> factory.create(playerId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReviewsMessageToast(uiState.message, viewModel::onMessageShown)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AiSummaryCard(uiState.summary, onRetry = viewModel::retrySummary)
        Text(
            stringResource(R.string.reviews_title, uiState.comments.size),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        if (!uiState.isLoading && uiState.comments.isEmpty()) {
            Text(
                stringResource(R.string.reviews_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        uiState.comments.forEach { comment ->
            CommentRow(
                comment = comment,
                reported = comment.id in uiState.reportedIds,
                onReport = { viewModel.report(comment.id) },
            )
        }
    }
}

/** Solo el resumen de IA: lo ve el anfitrión al revisar una solicitud. */
@Composable
fun RequestAiSummary(
    playerId: String,
    modifier: Modifier = Modifier,
    viewModel: PlayerReviewsViewModel = hiltViewModel<PlayerReviewsViewModel, PlayerReviewsViewModel.Factory>(
        key = "reviews-$playerId",
    ) { factory -> factory.create(playerId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AiSummaryCard(uiState.summary, onRetry = viewModel::retrySummary, modifier = modifier, compact = true)
}

/** Tarjeta con el resumen de Gemini y sus estados: generando, poco material, error o listo. */
@Composable
fun AiSummaryCard(
    state: AiSummaryState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(AI_SUMMARY_TAG),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(if (compact) 12.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_auto_awesome), null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.ai_summary_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            AnimatedContent(targetState = state, label = "ai-summary") { current ->
                when (current) {
                    AiSummaryState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.ai_summary_loading), style = MaterialTheme.typography.bodyMedium)
                    }
                    AiSummaryState.NotEnough -> Text(
                        stringResource(R.string.ai_summary_not_enough, PlayerReviewSummarizer.MIN_COMMENTS),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    is AiSummaryState.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(if (current.offline) R.string.ai_offline else R.string.ai_summary_error),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
                    }
                    is AiSummaryState.Ready -> Text(current.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: CommentItem, reported: Boolean, onReport: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        UserAvatar(photoUri = comment.authorPhotoUri, contentDescription = null, size = 36.dp, name = comment.authorNickname)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${comment.authorNickname} · ${comment.createdAt.format(commentDateFormatter)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(comment.text, style = MaterialTheme.typography.bodyMedium)
            if (!comment.isMine) {
                TextButton(onClick = onReport, enabled = !reported, contentPadding = PaddingValues(0.dp)) {
                    Text(
                        stringResource(if (reported) R.string.reviews_reported else R.string.reviews_report),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewsMessageToast(message: ReviewsMessage?, onShown: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(message) {
        val text = when (message) {
            ReviewsMessage.REPORTED -> R.string.reviews_report_sent
            ReviewsMessage.REPORT_FAILED -> R.string.reviews_report_error
            null -> return@LaunchedEffect
        }
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        onShown()
    }
}
