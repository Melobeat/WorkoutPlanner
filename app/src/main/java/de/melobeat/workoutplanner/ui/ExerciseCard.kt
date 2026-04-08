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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.melobeat.workoutplanner.ui.theme.GradientCardEnd
import de.melobeat.workoutplanner.ui.theme.GradientCardStart
import de.melobeat.workoutplanner.ui.theme.GradientHeroMid
import de.melobeat.workoutplanner.ui.theme.GradientHeroEnd

@Composable
private fun ActiveCardHeader(
    exercise: ExerciseUiState,
    exerciseIndex: Int,
    totalExercises: Int,
    onSwapExercise: () -> Unit,
) {
    val cardGradient = Brush.linearGradient(
        colors = listOf(GradientCardStart, GradientCardEnd),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = cardGradient,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Text(
                text = "EXERCISE ${exerciseIndex + 1} OF $totalExercises",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f),
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = onSwapExercise,
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.18f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Rounded.SwapHoriz,
                        contentDescription = "Swap exercise",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Swap", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

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

    if (isActive) {
        // ── Active card: gradient header strip + body ──────────────────────
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
        ) {
            ActiveCardHeader(
                exercise = exercise,
                exerciseIndex = exerciseIndex,
                totalExercises = totalExercises,
                onSwapExercise = onSwapExercise
            )
            // Card body
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    AnimatedVisibility(
                        visible = exercise.isExpanded,
                        enter = expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                        exit = shrinkVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    ) {
                        Column {
                            exercise.sets.forEachIndexed { si, set ->
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                val isActiveSet = si == currentSetIndex

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
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
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
                        }
                    }
                }
            }
        }
    } else {
        // ── Non-active card: existing Card layout ──────────────────────────
        val borderColor = when {
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
                // ── Header row ──────────────────────────────────────────────
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
                            if (isUpNext) {
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

                    IconButton(onClick = onToggleExpanded) {
                        Icon(
                            if (exercise.isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (exercise.isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Expanded body ───────────────────────────────────────────
                AnimatedVisibility(
                    visible = exercise.isExpanded,
                    enter = expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                    exit = shrinkVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                ) {
                    Column {
                        exercise.sets.forEachIndexed { si, set ->
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            when {
                                set.isDone -> {
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

                                else -> {
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
                        if (!allDone) {
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
}
