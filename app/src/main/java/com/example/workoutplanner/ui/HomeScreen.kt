package com.example.workoutplanner.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.workoutplanner.model.WorkoutDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    activeWorkoutViewModel: ActiveWorkoutViewModel = viewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showWorkoutChooser by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Planner") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.selectedRoutine != null) {
                item {
                    val routine = uiState.selectedRoutine!!
                    val nextDayIndex = (routine.lastCompletedDayIndex + 1) % routine.workoutDays.size
                    val nextDay = routine.workoutDays[nextDayIndex]

                    NextWorkoutCard(
                        routineName = routine.name,
                        nextDay = nextDay,
                        nextDayIndex = nextDayIndex,
                        totalDays = routine.workoutDays.size,
                        onStartWorkout = {
                            activeWorkoutViewModel.startWorkout(
                                day = nextDay,
                                dayIndex = nextDayIndex,
                                routineName = routine.name,
                                routineId = routine.id
                            )
                            onStartWorkout()
                        },
                        onSwapNextDay = { showWorkoutChooser = true }
                    )
                }
            } else {
                item {
                    EmptyStateCard(onViewRoutines = onNavigateToSettings)
                }
            }

            if (uiState.recentHistory.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(uiState.recentHistory.take(5)) { session ->
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
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(workoutDays) { index, day ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDaySelected(index) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Day ${index + 1}: ${day.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            day.exercises.forEach { exercise ->
                                Text(
                                    text = "• ${exercise.name}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NextWorkoutCard(
    routineName: String,
    nextDay: WorkoutDay,
    nextDayIndex: Int,
    totalDays: Int,
    onStartWorkout: () -> Unit,
    onSwapNextDay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Next Workout: $routineName",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = nextDay.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Day ${nextDayIndex + 1} of $totalDays",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Row {
                    IconButton(onClick = onSwapNextDay) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Swap Day")
                    }
                    Button(onClick = onStartWorkout) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            nextDay.exercises.take(3).forEach { exercise ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    val repsSummary = if (exercise.routineSets.isEmpty()) {
                        "0 sets"
                    } else if (exercise.routineSets.all { it.reps == exercise.routineSets.first().reps }) {
                        "${exercise.routineSets.size}x${exercise.routineSets.first().reps}"
                    } else {
                        exercise.routineSets.joinToString(", ") { it.reps.toString() }
                    }

                    Text(
                        text = "• ${exercise.name} ($repsSummary)",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            if (nextDay.exercises.size > 3) {
                Text(
                    text = "...and ${nextDay.exercises.size - 3} more",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(onViewRoutines: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No Active Routine",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Select a routine to start tracking your progress.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onViewRoutines) {
                Text("Manage Routines")
            }
        }
    }
}
