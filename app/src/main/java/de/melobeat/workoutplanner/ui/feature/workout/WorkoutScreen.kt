package de.melobeat.workoutplanner.ui.feature.workout

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import de.melobeat.workoutplanner.domain.model.Exercise
import de.melobeat.workoutplanner.ui.theme.WorkoutPlannerTheme
import androidx.compose.ui.res.stringResource
import de.melobeat.workoutplanner.R
import de.melobeat.workoutplanner.ui.feature.exercises.ExerciseLibraryViewModel
import de.melobeat.workoutplanner.ui.common.ExerciseCard
import de.melobeat.workoutplanner.ui.common.ExerciseSelectionDialog
import de.melobeat.workoutplanner.ui.common.RestTimerBanner
import de.melobeat.workoutplanner.ui.formatElapsedTime

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
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    LaunchedEffect(Unit) {
        viewModel.restTimerEvents.collect {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 50, 100, 50), -1)
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
        onIncrementLeftReps = { ei, si -> viewModel.incrementLeftReps(ei, si) },
        onDecrementLeftReps = { ei, si -> viewModel.decrementLeftReps(ei, si) },
        onIncrementRightReps = { ei, si -> viewModel.incrementRightReps(ei, si) },
        onDecrementRightReps = { ei, si -> viewModel.decrementRightReps(ei, si) },
        onGoBack = { viewModel.goToPreviousSet() },
        onSkipExercise = { viewModel.skipExercise() },
        onJumpToSet = { exerciseIndex, setIndex -> viewModel.jumpToSet(exerciseIndex, setIndex) },
        onToggleExerciseExpanded = { exerciseIndex -> viewModel.toggleExerciseExpanded(exerciseIndex) },
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
    onIncrementLeftReps: (exerciseIndex: Int, setIndex: Int) -> Unit,
    onDecrementLeftReps: (exerciseIndex: Int, setIndex: Int) -> Unit,
    onIncrementRightReps: (exerciseIndex: Int, setIndex: Int) -> Unit,
    onDecrementRightReps: (exerciseIndex: Int, setIndex: Int) -> Unit,
    onGoBack: () -> Unit,
    onSkipExercise: () -> Unit,
    onJumpToSet: (exerciseIndex: Int, setIndex: Int) -> Unit,
    onToggleExerciseExpanded: (exerciseIndex: Int) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showSwapExerciseIndex by remember { mutableStateOf<Int?>(null) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val exercises = uiState.exercises
    val ei = uiState.currentExerciseIndex
    val si = uiState.currentSetIndex

    val progress = if (exercises.isEmpty()) 0f
    else {
        val currentExercise = exercises.getOrNull(ei)
        (ei.toFloat() + (si.toFloat() / (currentExercise?.sets?.size?.toFloat() ?: 1f))) / exercises.size
    }

    val listState = rememberLazyListState()
    LaunchedEffect(ei) {
        if (exercises.isNotEmpty()) {
            listState.animateScrollToItem(ei.coerceIn(0, exercises.size - 1))
        }
    }

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
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.workout_minimize_cd))
                    }
                },
                actions = {
                    FilledTonalButton(onClick = { showAddExerciseDialog = true }, shape = CircleShape) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.workout_add_exercise), style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.width(4.dp))
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.workout_more_options_cd))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.workout_menu_finish)) },
                                onClick = { showMenu = false; onFinishWorkout() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.workout_menu_cancel), color = MaterialTheme.colorScheme.error) },
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
        ) {
            // Rest timer banner — sticky below TopAppBar
            AnimatedVisibility(
                visible = restTimer != null,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                if (restTimer != null) {
                    RestTimerBanner(
                        restTimer = restTimer,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Progress row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.workout_exercise_counter, ei + 1, exercises.size),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(CircleShape)
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            if (exercises.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.workout_no_exercises))
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(exercises, key = { _, exercise -> exercise.exerciseId }) { exerciseIndex, exercise ->
                        ExerciseCard(
                            exercise = exercise,
                            exerciseIndex = exerciseIndex,
                            totalExercises = exercises.size,
                            currentExerciseIndex = ei,
                            currentSetIndex = si,
                            onActivateSet = { setIndex -> onJumpToSet(exerciseIndex, setIndex) },
                            onToggleExpanded = { onToggleExerciseExpanded(exerciseIndex) },
                            onSwapExercise = { showSwapExerciseIndex = exerciseIndex },
                            onIncrementReps = { setIndex ->
                                onIncrementReps(
                                    exerciseIndex,
                                    setIndex
                                )
                            },
                            onDecrementReps = { setIndex ->
                                onDecrementReps(
                                    exerciseIndex,
                                    setIndex
                                )
                            },
                            onIncrementWeight = { setIndex ->
                                onIncrementWeight(
                                    exerciseIndex,
                                    setIndex
                                )
                            },
                            onDecrementWeight = { setIndex ->
                                onDecrementWeight(
                                    exerciseIndex,
                                    setIndex
                                )
                            },
                            onIncrementLeftReps = { setIndex ->
                                onIncrementLeftReps(
                                    exerciseIndex,
                                    setIndex
                                )
                            },
                            onDecrementLeftReps = { setIndex ->
                                onDecrementLeftReps(
                                    exerciseIndex,
                                    setIndex
                                )
                            },
                            onIncrementRightReps = { setIndex ->
                                onIncrementRightReps(
                                    exerciseIndex,
                                    setIndex
                                )
                            },
                            onDecrementRightReps = { setIndex ->
                                onDecrementRightReps(
                                    exerciseIndex,
                                    setIndex
                                )
                            },
                            onCompleteSet = onCompleteSet,
                            onGoBack = onGoBack,
                            onSkipExercise = onSkipExercise
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddExerciseDialog) {
        ExerciseSelectionDialog(
            title = stringResource(R.string.workout_dialog_add_exercise_title),
            exercises = availableExercises,
            onDismiss = { showAddExerciseDialog = false },
            onExerciseSelected = { exercise ->
                onAddExercise(exercise)
                showAddExerciseDialog = false
            }
        )
    }

    showSwapExerciseIndex?.let { swapIndex ->
        ExerciseSelectionDialog(
            title = stringResource(R.string.workout_dialog_swap_exercise_title),
            exercises = availableExercises,
            onDismiss = { showSwapExerciseIndex = null },
            onExerciseSelected = { exercise ->
                onSwapExercise(swapIndex, exercise)
                showSwapExerciseIndex = null
            }
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(stringResource(R.string.workout_cancel_dialog_title)) },
            text = { Text(stringResource(R.string.workout_cancel_dialog_body)) },
            confirmButton = {
                TextButton(onClick = { onCancelWorkout() }) {
                    Text(stringResource(R.string.workout_menu_cancel), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text(stringResource(R.string.workout_keep_going)) }
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
            onIncrementLeftReps = { _, _ -> },
            onDecrementLeftReps = { _, _ -> },
            onIncrementRightReps = { _, _ -> },
            onDecrementRightReps = { _, _ -> },
            onGoBack = {},
            onSkipExercise = {},
            onJumpToSet = { _, _ -> },
            onToggleExerciseExpanded = {},
            onNavigateBack = {}
        )
    }
}
