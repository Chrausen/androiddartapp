package com.clubdarts.ui.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.clubdarts.R
import com.clubdarts.data.model.FunRule
import com.clubdarts.data.model.FunRuleCategory
import com.clubdarts.ui.theme.*

@Composable
fun FunRuleOverlay(rule: FunRule, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface2, RoundedCornerShape(16.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (rule.category == FunRuleCategory.PHYSICAL)
                    stringResource(R.string.fun_rule_overlay_new_rule)
                else
                    stringResource(R.string.fun_rule_overlay_new_scoring_rule),
                style = MaterialTheme.typography.labelMedium,
                color = Accent,
            )

            Text(
                text = rule.emoji,
                fontSize = 56.sp,
            )

            Text(
                text = stringResource(rule.titleRes),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(rule.descRes),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Background,
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.fun_rule_overlay_got_it),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
