package de.melobeat.workoutplanner.ui

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
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.melobeat.workoutplanner.model.Exercise
import de.melobeat.workoutplanner.model.Routine
import de.melobeat.workoutplanner.model.RoutineSet
import de.melobeat.workoutplanner.model.WorkoutDay
import de.melobeat.workoutplanner.ui.theme.Pink40
import de.melobeat.workoutplanner.ui.theme.Purple10
import de.melobeat.workoutplanner.ui.theme.Purple40
import de.melobeat.workoutplanner.ui.theme.WorkoutPlannerTheme

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
    HomeScreenContent(
        uiState = uiState,
        onNavigateToSettings = onNavigateToSettings,
        onStartWorkout = { day, dayIndex, routineName, routineId ->
            activeWorkoutViewModel.startWorkout(
                day = day,
                dayIndex = dayIndex,
                routineName = routineName,
                routineId = routineId
            )
            onStartWorkout()
        },
        onUpdateNextDay = { routineId, lastCompletedIndex ->
            viewModel.updateNextDay(routineId, lastCompletedIndex)
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onNavigateToSettings: () -> Unit,
    onStartWorkout: (day: WorkoutDay, dayIndex: Int, routineName: String, routineId: String) -> Unit,
    onUpdateNextDay: (routineId: String, lastCompletedIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
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
                                    onStartWorkout(nextDay, nextDayIndex, routine.name, routine.id)
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
                onUpdateNextDay(routine.id, lastCompletedIndex)
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
                    Surface(
                        onClick = { onDaySelected(index) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    "Day ${index + 1}: ${day.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            supportingContent = {
                                Text(
                                    day.exercises.joinToString(", ") { it.name },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingContent = {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        confirmButton = {}
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenContentPreview() {
    WorkoutPlannerTheme {
        HomeScreenContent(
            uiState = HomeUiState(
                selectedRoutine = Routine(
                    id = "r1",
                    name = "Push Pull Legs",
                    isSelected = true,
                    lastCompletedDayIndex = -1,
                    workoutDays = listOf(
                        WorkoutDay(
                            id = "d1",
                            name = "Push Day",
                            exercises = listOf(
                                Exercise(id = "e1", name = "Bench Press", muscleGroup = "Chest",
                                    routineSets = listOf(RoutineSet(10, 60.0), RoutineSet(10, 60.0), RoutineSet(8, 60.0))),
                                Exercise(id = "e2", name = "Overhead Press", muscleGroup = "Shoulders",
                                    routineSets = listOf(RoutineSet(8, 40.0))),
                                Exercise(id = "e3", name = "Tricep Pushdown", muscleGroup = "Triceps",
                                    routineSets = listOf(RoutineSet(12, 25.0)))
                            )
                        ),
                        WorkoutDay(id = "d2", name = "Pull Day", exercises = listOf()),
                        WorkoutDay(id = "d3", name = "Leg Day", exercises = listOf())
                    )
                ),
                recentHistory = emptyList(),
                exerciseNameMap = emptyMap(),
                isLoading = false
            ),
            onNavigateToSettings = {},
            onStartWorkout = { _, _, _, _ -> },
            onUpdateNextDay = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WorkoutDayChooserDialogPreview() {
    WorkoutPlannerTheme {
        WorkoutDayChooserDialog(
            workoutDays = listOf(
                WorkoutDay(id = "d1", name = "Push Day", exercises = listOf(
                    Exercise(id = "e1", name = "Bench Press", muscleGroup = "Chest"),
                    Exercise(id = "e2", name = "Overhead Press", muscleGroup = "Shoulders")
                )),
                WorkoutDay(id = "d2", name = "Pull Day", exercises = listOf(
                    Exercise(id = "e3", name = "Pull-up", muscleGroup = "Back")
                )),
                WorkoutDay(id = "d3", name = "Leg Day", exercises = listOf())
            ),
            onDaySelected = {},
            onDismiss = {}
        )
    }
}