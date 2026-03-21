package com.clubdarts.ui.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubdarts.ui.theme.*

@Composable
fun DartNumpad(
    pendingMultiplier: Int,
    onMultiplierChange: (Int) -> Unit,
    onDart: (Int) -> Unit,
    onMiss: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Multiplier row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Single" to 1, "Double" to 2, "Triple" to 3).forEach { (label, mult) ->
                MultiplierButton(
                    label = label,
                    isActive = pendingMultiplier == mult,
                    onClick = { onMultiplierChange(mult) },
                    modifier = Modifier.weight(1f)
                )
            }
            MultiplierButton(
                label = "Miss",
                isActive = false,
                onClick = onMiss,
                modifier = Modifier.weight(1f),
                isMiss = true
            )
        }

        // Number grid 1–20
        val numbers = (1..20).toList()
        val rows = numbers.chunked(5)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { num ->
                    NumpadButton(
                        label = num.toString(),
                        onClick = { onDart(num) },
                        modifier = Modifier.weight(1f),
                        enabled = true
                    )
                }
            }
        }

        // Bottom row: 25 (Bull) + Undo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NumpadButton(
                label = "Bull\n25",
                onClick = { onDart(25) },
                modifier = Modifier.weight(3f),
                enabled = pendingMultiplier != 3
            )
            Box(
                modifier = Modifier
                    .weight(2f)
                    .height(60.dp)
                    .background(Surface3, RoundedCornerShape(8.dp))
                    .clickable(onClick = onUndo),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = Amber,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Undo",
                        style = MaterialTheme.typography.labelMedium,
                        color = Amber
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiplierButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isMiss: Boolean = false
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .background(
                color = if (isActive) Accent else Surface2,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = when {
                isActive   -> Background
                isMiss     -> Red
                else       -> TextSecondary
            }
        )
    }
}

@Composable
private fun NumpadButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .height(60.dp)
            .background(
                color = if (enabled) Surface2 else Surface,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = DmMono,
            color = if (enabled) TextPrimary else TextTertiary
        )
    }
}
