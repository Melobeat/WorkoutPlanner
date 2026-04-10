package de.melobeat.workoutplanner.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.melobeat.workoutplanner.ui.theme.GradientCardStart
import de.melobeat.workoutplanner.ui.theme.GradientCtaEnd
import de.melobeat.workoutplanner.ui.theme.GradientCtaStart
import de.melobeat.workoutplanner.ui.theme.GradientHeroEnd
import de.melobeat.workoutplanner.ui.theme.GradientHeroMid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummaryScreen(
    onResumeWorkout: () -> Unit,
    onWorkoutFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveWorkoutViewModel = viewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(onBack = onResumeWorkout)

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) onWorkoutFinished()
    }

    val totalVolumeKg = uiState.exercises.sumOf { exercise ->
        exercise.sets.filter { it.isDone }.sumOf { set ->
            (set.weight.toDoubleOrNull() ?: 0.0) * (set.reps.toIntOrNull() ?: 0)
        }
    }

    val summaryHeaderBrush = Brush.linearGradient(
        colors = listOf(GradientCardStart, GradientHeroMid, GradientHeroEnd),
        start = Offset(0f, Float.POSITIVE_INFINITY),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    Scaffold(
        topBar = {
            Box(modifier = Modifier.background(summaryHeaderBrush)) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                uiState.workoutDayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                formatElapsedTime(uiState.summaryDurationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onResumeWorkout) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to workout")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Total volume stat
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Total Volume",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${totalVolumeKg.toInt()} kg",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Exercises list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(uiState.exercises) { _, exercise ->
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(
                            exercise.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        exercise.sets.forEachIndexed { index, set ->
                            val isSkipped = !set.isDone
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Set ${index + 1}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSkipped)
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (set.isAmrap && !isSkipped) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                "AMRAP",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                if (isSkipped) {
                                    Text(
                                        "Skipped",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                } else {
                                    Text(
                                        "${set.reps} reps × ${set.weight} kg",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            // Action buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.finishWorkout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(GradientCtaStart, GradientCtaEnd)),
                            shape = CircleShape
                        ),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    )
                ) {
                    Text("Save & Exit", fontWeight = FontWeight.Black)
                }
                OutlinedButton(
                    onClick = onResumeWorkout,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Resume Workout")
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
