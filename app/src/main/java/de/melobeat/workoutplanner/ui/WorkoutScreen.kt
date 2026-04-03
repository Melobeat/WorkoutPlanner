package de.melobeat.workoutplanner.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.melobeat.workoutplanner.model.Exercise
import de.melobeat.workoutplanner.ui.theme.WorkoutPlannerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSummary: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveWorkoutViewModel = viewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    ),
    exerciseLibraryViewModel: ExerciseLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exerciseLibState by exerciseLibraryViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.showSummary) {
        if (uiState.showSummary) onNavigateToSummary()
    }

    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(android.os.Vibrator::class.java) }
    LaunchedEffect(Unit) {
        viewModel.restTimerEvents.collect {
            vibrator?.vibrate(
                android.os.VibrationEffect.createWaveform(longArrayOf(0, 50, 100, 50), -1)
            )
        }
    }

    WorkoutScreenContent(
        uiState = uiState,
        restTimer = uiState.restTimer,
        availableExercises = exerciseLibState.exercises,
        onMinimize = { viewModel.setFullScreen(false); onNavigateBack() },
        onCompleteSet = { viewModel.completeCurrentSet() },
        onAddExercise = { viewModel.addExercise(it) },
        onSwapExercise = { exerciseIndex, exercise -> viewModel.swapExercise(exerciseIndex, exercise) },
        onFinishWorkout = { viewModel.requestFinish() },
        onCancelWorkout = { viewModel.cancelWorkout(); onNavigateBack() },
        onIncrementReps = { ei, si -> viewModel.incrementReps(ei, si) },
        onDecrementReps = { ei, si -> viewModel.decrementReps(ei, si) },
        onIncrementWeight = { ei, si -> viewModel.incrementWeight(ei, si) },
        onDecrementWeight = { ei, si -> viewModel.decrementWeight(ei, si) },
        onGoBack = { viewModel.goToPreviousSet() },
        onSkipExercise = { viewModel.skipExercise() },
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreenContent(
    uiState: ActiveWorkoutUiState,
    restTimer: RestTimerUiState?,
    availableExercises: List<Exercise>,
    onMinimize: () -> Unit,
    onCompleteSet: () -> Unit,
    onAddExercise: (Exercise) -> Unit,
    onFinishWorkout: () -> Unit,
    onCancelWorkout: () -> Unit,
    onIncrementReps: (exerciseIndex: Int, setIndex: Int) -> Unit,
    onDecrementReps: (exerciseIndex: Int, setIndex: Int) -> Unit,
    onSwapExercise: (exerciseIndex: Int, Exercise) -> Unit,
    onIncrementWeight: (exerciseIndex: Int, setIndex: Int) -> Unit,
    onDecrementWeight: (exerciseIndex: Int, setIndex: Int) -> Unit,
    onGoBack: () -> Unit,
    onSkipExercise: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showSwapDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val exercises = uiState.exercises
    val ei = uiState.currentExerciseIndex.coerceIn(0, (exercises.size - 1).coerceAtLeast(0))
    val currentExercise = exercises.getOrNull(ei)
    val si = uiState.currentSetIndex.coerceIn(
        0, ((currentExercise?.sets?.size ?: 1) - 1).coerceAtLeast(0)
    )
    val currentSet = currentExercise?.sets?.getOrNull(si)
    val nextExercise = exercises.getOrNull(ei + 1)
    val progress = if (exercises.isEmpty()) 0f
    else (ei.toFloat() + (si.toFloat() / (currentExercise?.sets?.size?.toFloat() ?: 1f))) / exercises.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.workoutDayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = formatElapsedTime(uiState.elapsedTime),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMinimize) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Minimize")
                    }
                },
                actions = {
                    FilledTonalButton(onClick = { showAddExerciseDialog = true }, shape = CircleShape) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Exercise", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.width(4.dp))
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Finish Workout") },
                                onClick = { showMenu = false; onFinishWorkout() }
                            )
                            DropdownMenuItem(
                                text = { Text("Cancel Workout", color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; showCancelDialog = true }
                            )
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXERCISE ${ei + 1} OF ${exercises.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().clip(CircleShape).height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            if (currentExercise != null && currentSet != null) {
                WorkoutExerciseContent(
                    exercise = currentExercise,
                    currentSet = currentSet,
                    nextExercise = nextExercise,
                    exerciseIndex = ei,
                    setIndex = si,
                    totalExercises = exercises.size,
                    restTimer = restTimer,
                    onSwapExercise = { showSwapDialog = true },
                    onIncrementReps = { onIncrementReps(ei, si) },
                    onDecrementReps = { onDecrementReps(ei, si) },
                    onIncrementWeight = { onIncrementWeight(ei, si) },
                    onDecrementWeight = { onDecrementWeight(ei, si) },
                    onCompleteSet = onCompleteSet,
                    onGoBack = onGoBack,
                    onSkipExercise = onSkipExercise
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No exercises in this workout.")
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        ExerciseSelectionDialog(
            title = "Add Exercise",
            exercises = availableExercises,
            onDismiss = { showAddExerciseDialog = false },
            onExerciseSelected = { exercise ->
                onAddExercise(exercise)
                showAddExerciseDialog = false
            }
        )
    }

    if (showSwapDialog) {
        ExerciseSelectionDialog(
            title = "Swap Exercise",
            exercises = availableExercises,
            onDismiss = { showSwapDialog = false },
            onExerciseSelected = { exercise ->
                onSwapExercise(ei, exercise)
                showSwapDialog = false
            }
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Workout?") },
            text = { Text("All progress in this session will be lost.") },
            confirmButton = {
                TextButton(onClick = { onCancelWorkout() }) {
                    Text("Cancel Workout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Keep Going") }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WorkoutScreenContentPreview() {
    WorkoutPlannerTheme {
        WorkoutScreenContent(
            uiState = ActiveWorkoutUiState(
                isActive = true,
                workoutDayName = "Push Day",
                elapsedTime = 1_230_000L,
                isFinished = false,
                currentExerciseIndex = 0,
                currentSetIndex = 1,
                exercises = listOf(
                    ExerciseUiState(
                        exerciseId = "e1",
                        name = "Bench Press",
                        sets = listOf(
                            SetUiState(index = 0, weight = "60", reps = "10", isAmrap = false, isDone = true, originalReps = "10"),
                            SetUiState(index = 1, weight = "60", reps = "10", isAmrap = false, isDone = false, originalReps = "10"),
                            SetUiState(index = 2, weight = "60", reps = "8", isAmrap = true, isDone = false, originalReps = "8")
                        )
                    ),
                    ExerciseUiState(
                        exerciseId = "e2",
                        name = "Overhead Press",
                        sets = listOf(
                            SetUiState(index = 0, weight = "40", reps = "8", isAmrap = false, isDone = false, originalReps = "8")
                        )
                    )
                )
            ),
            restTimer = null,
            availableExercises = listOf(Exercise(id = "e3", name = "Incline Press", muscleGroup = "Chest")),
            onMinimize = {},
            onCompleteSet = {},
            onAddExercise = {},
            onSwapExercise = { _, _ -> },
            onFinishWorkout = {},
            onCancelWorkout = {},
            onIncrementReps = { _, _ -> },
            onDecrementReps = { _, _ -> },
            onIncrementWeight = { _, _ -> },
            onDecrementWeight = { _, _ -> },
            onGoBack = {},
            onSkipExercise = {},
            onNavigateBack = {}
        )
    }
}
