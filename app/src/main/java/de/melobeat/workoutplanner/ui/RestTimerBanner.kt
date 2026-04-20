package de.melobeat.workoutplanner.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.melobeat.workoutplanner.R

@Composable
fun RestTimerBanner(restTimer: RestTimerUiState, modifier: Modifier = Modifier) {
    val elapsed = restTimer.elapsedSeconds
    val hardMilestone = stringResource(R.string.rest_timer_hard_milestone)
    val easyMilestone = stringResource(R.string.rest_timer_easy_milestone)
    val exerciseMilestone = stringResource(R.string.rest_timer_exercise_milestone)

    val milestoneLabel: String? = when (restTimer.context) {
        RestTimerContext.BetweenSets -> when {
            elapsed >= restTimer.hardThresholdSeconds -> hardMilestone
            elapsed >= restTimer.easyThresholdSeconds -> easyMilestone
            else -> null
        }
        RestTimerContext.BetweenExercises -> when {
            elapsed >= restTimer.singleThresholdSeconds -> exerciseMilestone
            else -> null
        }
    }
    val progress: Float = when (restTimer.context) {
        RestTimerContext.BetweenSets -> when {
            elapsed >= restTimer.hardThresholdSeconds -> 1f
            elapsed >= restTimer.easyThresholdSeconds -> {
                val segLen = restTimer.hardThresholdSeconds - restTimer.easyThresholdSeconds
                if (segLen == 0) 1f
                else (elapsed - restTimer.easyThresholdSeconds).toFloat() / segLen.toFloat()
            }
            else -> elapsed.toFloat() / restTimer.easyThresholdSeconds.toFloat()
        }
        RestTimerContext.BetweenExercises -> when {
            elapsed >= restTimer.singleThresholdSeconds -> 1f
            else -> elapsed.toFloat() / restTimer.singleThresholdSeconds.toFloat()
        }
    }.coerceIn(0f, 1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.rest_timer_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                )
                Text(
                    text = formatElapsedTime(elapsed * 1000L),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .height(6.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            )
            if (milestoneLabel != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = milestoneLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
