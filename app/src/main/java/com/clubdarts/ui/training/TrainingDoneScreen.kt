package com.clubdarts.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubdarts.R
import com.clubdarts.data.model.TrainingMode
import com.clubdarts.ui.theme.*

@Composable
fun TrainingDoneScreen(
    uiState: TrainingUiState,
    onRepeat: () -> Unit,
    onBack: () -> Unit
) {
    val result = uiState.lastResult
    val mode   = uiState.mode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.training_done_title),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )

        Spacer(Modifier.height(32.dp))

        if (result != null) {
            val resultText = when (mode) {
                TrainingMode.SCORING_ROUNDS -> "Ø %.1f".format(result.result / 10.0)
                else                         -> "${result.result}"
            }
            val resultUnit = when (mode) {
                TrainingMode.SCORING_ROUNDS -> stringResource(R.string.training_pts_per_round)
                else                         -> stringResource(R.string.training_darts_used)
            }

            Text(
                text = resultText,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DmMono,
                color = Accent
            )
            Text(
                text = resultUnit,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onRepeat,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = stringResource(R.string.training_repeat),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Text(
                text = stringResource(R.string.btn_back),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
        }
    }
}
