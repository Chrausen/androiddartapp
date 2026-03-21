package com.clubdarts.ui.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubdarts.ui.game.VisitRecord
import com.clubdarts.ui.theme.*

@Composable
fun VisitHistory(
    visits: List<VisitRecord>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Player", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.weight(2f))
            Text("D1", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("D2", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("D3", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("Total", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        HorizontalDivider(color = Border)

        // Always show exactly 3 rows so height never changes
        val displayed = visits.take(3)
        displayed.forEach { visit -> VisitRow(visit = visit) }
        repeat(3 - displayed.size) { VisitPlaceholderRow() }
    }
}

@Composable
private fun VisitPlaceholderRow() {
    Spacer(modifier = Modifier.fillMaxWidth().height(28.dp))
}

@Composable
private fun VisitRow(visit: VisitRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(
                color = if (visit.isBust) Red.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = visit.playerName,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            modifier = Modifier.weight(2f)
        )
        DartCell(dart = visit.dart1, modifier = Modifier.weight(1f))
        DartCell(dart = visit.dart2, modifier = Modifier.weight(1f))
        DartCell(dart = visit.dart3, modifier = Modifier.weight(1f))

        // Total
        if (visit.isBust) {
            Text(
                text = "BUST",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Red,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        } else {
            Text(
                text = visit.total.toString(),
                fontSize = 13.sp,
                fontFamily = DmMono,
                fontWeight = FontWeight.SemiBold,
                color = if (visit.total == 180) Accent else TextPrimary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun DartCell(
    dart: com.clubdarts.ui.game.DartInput?,
    modifier: Modifier = Modifier
) {
    Text(
        text = dart?.label() ?: "—",
        style = MaterialTheme.typography.labelSmall,
        fontFamily = DmMono,
        color = if (dart?.score == 0) TextTertiary else TextSecondary,
        modifier = modifier,
        textAlign = TextAlign.Center
    )
}
