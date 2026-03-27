package com.example.workoutplanner.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workoutplanner.model.WorkoutDay
import com.example.workoutplanner.ui.theme.Pink40
import com.example.workoutplanner.ui.theme.Purple10
import com.example.workoutplanner.ui.theme.Purple40

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    activeWorkoutViewModel: ActiveWorkoutViewModel = viewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showWorkoutChooser by remember { mutableStateOf(false) }
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val heroBrush = Brush.linearGradient(listOf(Purple10, Purple40, Pink40))

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Hero banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(heroBrush)
                    .padding(top = statusBarPadding + 16.dp, start = 20.dp, end = 20.dp, bottom = 28.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Title row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.FitnessCenter,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Workout Planner",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                Icons.Rounded.Tune,
                                contentDescription = "Settings",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    if (uiState.selectedRoutine != null) {
                        val routine = uiState.selectedRoutine!!
                        val nextDayIndex = (routine.lastCompletedDayIndex + 1) % routine.workoutDays.size
                        val nextDay = routine.workoutDays[nextDayIndex]

                        // Routine label
                        Text(
                            text = "${routine.name.uppercase()} · DAY ${nextDayIndex + 1} OF ${routine.workoutDays.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = nextDay.name,
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            lineHeight = MaterialTheme.typography.displaySmall.lineHeight
                        )
                        Text(
                            text = "${nextDay.exercises.size} exercises",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.65f)
                        )

                        // Exercise chips
                        if (nextDay.exercises.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                nextDay.exercises.take(2).forEach { ex ->
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            ex.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                                if (nextDay.exercises.size > 2) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "+${nextDay.exercises.size - 2} more",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // CTA buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    activeWorkoutViewModel.startWorkout(
                                        day = nextDay,
                                        dayIndex = nextDayIndex,
                                        routineName = routine.name,
                                        routineId = routine.id
                                    )
                                    onStartWorkout()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Purple40
                                )
                            ) {
                                Text("▶ Start Workout", fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = { showWorkoutChooser = true },
                                shape = CircleShape,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Rounded.SwapHoriz, contentDescription = "Swap Day")
                            }
                        }
                    } else {
                        // Empty state
                        Text(
                            "No Active Routine",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Select a routine to start tracking your progress.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToSettings,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Purple40
                            )
                        ) {
                            Text("Manage Routines", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Recent workouts header
        if (uiState.recentHistory.isNotEmpty()) {
            item {
                Text(
                    "RECENT WORKOUTS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
                )
            }
            items(uiState.recentHistory.take(5), key = { it.workout.id }) { session ->
                Box(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
                    WorkoutSessionCard(session, uiState.exerciseNameMap)
                }
            }
        }
    }

    if (showWorkoutChooser && uiState.selectedRoutine != null) {
        WorkoutDayChooserDialog(
            workoutDays = uiState.selectedRoutine!!.workoutDays,
            onDaySelected = { index ->
                val routine = uiState.selectedRoutine ?: return@WorkoutDayChooserDialog
                val totalDays = routine.workoutDays.size
                val lastCompletedIndex = (index + totalDays - 1) % totalDays
                viewModel.updateNextDay(routine.id, lastCompletedIndex)
                showWorkoutChooser = false
            },
            onDismiss = { showWorkoutChooser = false }
        )
    }
}

@Composable
fun WorkoutDayChooserDialog(
    workoutDays: List<WorkoutDay>,
    onDaySelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Next Workout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                workoutDays.forEachIndexed { index, day ->
                    Card(
                        onClick = { onDaySelected(index) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Day ${index + 1}: ${day.name}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            day.exercises.forEach { exercise ->
                                Text("• ${exercise.name}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
