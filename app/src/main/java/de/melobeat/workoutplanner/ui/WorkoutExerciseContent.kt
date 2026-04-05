package de.melobeat.workoutplanner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.melobeat.workoutplanner.ui.theme.Pink40
import de.melobeat.workoutplanner.ui.theme.Purple40

@Composable
fun WorkoutExerciseContent(
    exercise: ExerciseUiState,
    currentSet: SetUiState,
    nextExercise: ExerciseUiState?,
    exerciseIndex: Int,
    setIndex: Int,
    totalExercises: Int,
    restTimer: RestTimerUiState?,
    onSwapExercise: () -> Unit,
    onIncrementReps: () -> Unit,
    onDecrementReps: () -> Unit,
    onIncrementWeight: () -> Unit,
    onDecrementWeight: () -> Unit,
    onCompleteSet: () -> Unit,
    onGoBack: () -> Unit,
    onSkipExercise: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Exercise header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            FilledTonalButton(onClick = onSwapExercise, shape = CircleShape) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Swap", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Set dot indicators
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            exercise.sets.forEachIndexed { index, set ->
                val width = if (index == setIndex) 28.dp else 8.dp
                Surface(
                    modifier = Modifier.height(8.dp).width(width).clip(CircleShape),
                    color = when {
                        set.isDone -> MaterialTheme.colorScheme.primary
                        index == setIndex -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {}
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "SET ${setIndex + 1} OF ${exercise.sets.size}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(14.dp))

        // Stepper cards row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StepperCard(
                label = "Reps",
                value = currentSet.reps,
                onIncrement = onIncrementReps,
                onDecrement = onDecrementReps,
                modifier = Modifier.weight(1f)
            )
            StepperCard(
                label = "kg",
                value = currentSet.weight,
                onIncrement = onIncrementWeight,
                onDecrement = onDecrementWeight,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        // AMRAP badge — shown only on last set when flagged in routine
        if (currentSet.isAmrap) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = "AMRAP",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // Done CTA
        val isLastSet = setIndex == exercise.sets.size - 1
        val isLastExercise = exerciseIndex == totalExercises - 1
        val ctaLabel = when {
            isLastSet && isLastExercise -> "✓  Finish Workout"
            isLastSet -> "✓  Next Exercise"
            else -> "✓  Done — Set ${setIndex + 2}"
        }
        Surface(
            onClick = onCompleteSet,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(50),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(50))
                    .background(Brush.linearGradient(listOf(Purple40, Pink40))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    ctaLabel,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // Rest timer banner — visible while resting after a set
        Spacer(Modifier.height(8.dp))
        AnimatedVisibility(
            visible = restTimer != null,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            if (restTimer != null) {
                RestTimerBanner(restTimer = restTimer)
            }
        }

        // Navigation row — Back and Skip
        val isAtStart = exerciseIndex == 0 && setIndex == 0
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilledTonalButton(onClick = onGoBack, enabled = !isAtStart, shape = CircleShape) {
                Text("← Back", style = MaterialTheme.typography.labelMedium)
            }
            FilledTonalButton(onClick = onSkipExercise, shape = CircleShape) {
                Text("Skip Exercise →", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Completed sets chips
        val doneSets = exercise.sets.filter { it.isDone }
        AnimatedVisibility(
            visible = doneSets.isNotEmpty(),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                exercise.sets.forEachIndexed { index, set ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (set.isDone) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                    ) {
                        val label = if (set.isDone) "Set ${index + 1}\n${set.reps}×${set.weight}" else "Set ${index + 1}\n—"
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = if (set.isDone) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Next exercise preview
        if (nextExercise != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "NEXT EXERCISE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            nextExercise.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${nextExercise.sets.size} sets · ${nextExercise.sets.firstOrNull()?.reps ?: "0"} reps · ${nextExercise.sets.firstOrNull()?.weight ?: "0"} kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
