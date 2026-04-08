package de.melobeat.workoutplanner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.melobeat.workoutplanner.ui.theme.GradientHeroMid
import de.melobeat.workoutplanner.ui.theme.GradientHeroEnd

@Composable
fun ExerciseCard(
    exercise: ExerciseUiState,
    exerciseIndex: Int,
    totalExercises: Int,
    currentExerciseIndex: Int,
    currentSetIndex: Int,
    onActivateSet: (setIndex: Int) -> Unit,
    onToggleExpanded: () -> Unit,
    onSwapExercise: () -> Unit,
    onIncrementReps: (setIndex: Int) -> Unit,
    onDecrementReps: (setIndex: Int) -> Unit,
    onIncrementWeight: (setIndex: Int) -> Unit,
    onDecrementWeight: (setIndex: Int) -> Unit,
    onCompleteSet: () -> Unit,
    onGoBack: () -> Unit,
    onSkipExercise: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = exerciseIndex == currentExerciseIndex
    val allDone = exercise.sets.isNotEmpty() && exercise.sets.all { it.isDone }
    val hasAnyDone = exercise.sets.any { it.isDone }
    val isIncompleteAway = !isActive && hasAnyDone && !allDone
    val isUpNext = !isActive && !allDone && !isIncompleteAway &&
            exerciseIndex == currentExerciseIndex + 1

    val borderColor = when {
        isActive -> MaterialTheme.colorScheme.primary
        isIncompleteAway -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val cardAlpha = if (allDone) 0.55f else 1f

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor.copy(alpha = cardAlpha))
    ) {
        Column(modifier = Modifier.then(
            if (allDone) Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = cardAlpha))
            else Modifier
        )) {

            // ── Header row ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (allDone || isIncompleteAway) Modifier.clickable { onToggleExpanded() } else Modifier)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (allDone) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Column {
                        if (isActive) {
                            Text(
                                "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (isUpNext) {
                            Text(
                                "UP NEXT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            exercise.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        // Subtitle for collapsed states
                        if (!exercise.isExpanded) {
                            val subtitle = when {
                                allDone -> "${exercise.sets.size} sets done"
                                isIncompleteAway -> {
                                    val doneCount = exercise.sets.count { it.isDone }
                                    "Set $doneCount done · Set ${doneCount + 1} incomplete"
                                }
                                else -> {
                                    val first = exercise.sets.firstOrNull()
                                    "${exercise.sets.size} sets · ${first?.reps ?: "0"} reps · ${first?.weight ?: "0"} kg"
                                }
                            }
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isActive) {
                        FilledTonalButton(onClick = onSwapExercise, shape = CircleShape) {
                            Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Swap", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(onClick = onToggleExpanded) {
                        Icon(
                            if (exercise.isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (exercise.isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Expanded body ───────────────────────────────────────────────
            AnimatedVisibility(
                visible = exercise.isExpanded,
                enter = expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = shrinkVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            ) {
                Column {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    exercise.sets.forEachIndexed { si, set ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        val isActiveSet = isActive && si == currentSetIndex

                        when {
                            set.isDone -> {
                                // Completed set row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onActivateSet(si) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Set ${si + 1}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(0.6f)
                                    )
                                    Text(
                                        "${set.reps} × ${set.weight} kg",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = "Done",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            isActiveSet -> {
                                // Active set — expanded with steppers + CTA
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .drawBehind {
                                            drawRect(
                                                color = primaryColor,
                                                size = Size(3.dp.toPx(), size.height)
                                            )
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                                            .padding(start = 15.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
                                    ) {
                                        Text(
                                            "SET ${si + 1} — ACTIVE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        // Stepper cards
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            StepperCard(
                                                label = "Reps",
                                                value = set.reps,
                                                onIncrement = { onIncrementReps(si) },
                                                onDecrement = { onDecrementReps(si) },
                                                isAmrap = set.isAmrap,
                                                modifier = Modifier.weight(1f)
                                            )
                                            StepperCard(
                                                label = "kg",
                                                value = set.weight,
                                                onIncrement = { onIncrementWeight(si) },
                                                onDecrement = { onDecrementWeight(si) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        // Done CTA
                                        val isLastSet = si == exercise.sets.size - 1
                                        val isLastExercise = exerciseIndex == totalExercises - 1
                                        val ctaLabel = when {
                                            isLastSet && isLastExercise -> "✓  Finish Workout"
                                            isLastSet -> "✓  Next Exercise"
                                            else -> "✓  Done — Set ${si + 2}"
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
                                                    .background(Brush.linearGradient(listOf(GradientHeroMid, GradientHeroEnd))),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    ctaLabel,
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        // Back / Skip row
                                        val isAtStart = exerciseIndex == 0 && si == 0
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            FilledTonalButton(
                                                onClick = onGoBack,
                                                enabled = !isAtStart,
                                                shape = CircleShape
                                            ) {
                                                Text("← Back", style = MaterialTheme.typography.labelMedium)
                                            }
                                            FilledTonalButton(
                                                onClick = onSkipExercise,
                                                shape = CircleShape
                                            ) {
                                                Text("Skip Exercise →", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                // Pending set row — tappable to jump
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onActivateSet(si) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Set ${si + 1}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.weight(0.6f)
                                    )
                                    Text(
                                        "${set.reps} × ${set.weight} kg",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    )
                                }
                            }
                        }
                    }

                    // Hint text for non-active expanded upcoming cards
                    if (!isActive && !allDone) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Text(
                            "Tap any set to jump to it",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
