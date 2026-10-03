package com.tuckercr.catsdogs.ui.pets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuckercr.catsdogs.R
import com.tuckercr.catsdogs.ui.theme.WalkColdBg
import com.tuckercr.catsdogs.ui.theme.WalkColdText
import com.tuckercr.catsdogs.ui.theme.WalkGreatBg
import com.tuckercr.catsdogs.ui.theme.WalkGreatText
import com.tuckercr.catsdogs.ui.theme.WalkHotBg
import com.tuckercr.catsdogs.ui.theme.WalkHotText
import com.tuckercr.catsdogs.ui.theme.WalkOkayBg
import com.tuckercr.catsdogs.ui.theme.WalkOkayText
import com.tuckercr.catsdogs.ui.theme.WalkPoorBg
import com.tuckercr.catsdogs.ui.theme.WalkPoorText
import kotlin.math.roundToInt

/** "Best walk time" card: when to take the dog out, with a quick rating chip. */
@Composable
fun WalkCard(
    advice: WalkAdvice,
    modifier: Modifier = Modifier,
) {
    val detail = when {
        advice.rating == WalkRating.GREAT -> stringResource(R.string.walk_now)
        advice.bestTimeLabel != null -> stringResource(R.string.walk_around, advice.bestTimeLabel)
        else -> stringResource(R.string.walk_none)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Pets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.walk_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (advice.rating == WalkRating.PAWS_HOT && advice.pavementNow != null) {
                    Text(
                        text = stringResource(R.string.walk_pavement, advice.pavementNow.roundToInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            RatingChip(advice.rating)
        }
    }
}

@Composable
private fun RatingChip(rating: WalkRating) {
    val (label, bg, fg) = when (rating) {
        WalkRating.GREAT -> Triple(R.string.walk_rating_great, WalkGreatBg, WalkGreatText)
        WalkRating.OKAY -> Triple(R.string.walk_rating_okay, WalkOkayBg, WalkOkayText)
        WalkRating.POOR -> Triple(R.string.walk_rating_poor, WalkPoorBg, WalkPoorText)
        WalkRating.PAWS_HOT -> Triple(R.string.walk_rating_hot, WalkHotBg, WalkHotText)
        WalkRating.TOO_COLD -> Triple(R.string.walk_rating_cold, WalkColdBg, WalkColdText)
    }
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg as Color)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}
