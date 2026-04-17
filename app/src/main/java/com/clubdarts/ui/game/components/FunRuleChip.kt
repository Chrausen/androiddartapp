package com.clubdarts.ui.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clubdarts.data.model.FunRule
import com.clubdarts.ui.theme.*

@Composable
fun FunRuleChip(rule: FunRule, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(AccentDim, RoundedCornerShape(8.dp))
            .border(1.dp, Accent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = rule.emoji, style = MaterialTheme.typography.labelMedium)
        Text(
            text = rule.titleDe,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Accent,
        )
    }
}
