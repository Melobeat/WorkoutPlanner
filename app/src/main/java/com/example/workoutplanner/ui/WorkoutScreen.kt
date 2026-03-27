package com.example.workoutplanner.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workoutplanner.model.Exercise
import com.example.workoutplanner.ui.theme.Pink40
import com.example.workoutplanner.ui.theme.Purple40
import com.example.workoutplanner.ui.theme.WorkoutPlannerTheme
import java.util.Locale

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

    WorkoutScreenContent(
        uiState = uiState,
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
                    FilledTonalButton(
                        onClick = { showAddExerciseDialog = true },
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Exercise", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.width(4.dp))
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Finish Workout") },
                                onClick = {
                                    showMenu = false
                                    onFinishWorkout()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Cancel Workout", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    showCancelDialog = true
                                }
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
            // Progress bar
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
                // Exercise header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentExercise.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    FilledTonalButton(
                        onClick = { showSwapDialog = true },
                        shape = CircleShape
                    ) {
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
                    currentExercise.sets.forEachIndexed { index, set ->
                        val width = if (index == si) 28.dp else 8.dp
                        Surface(
                            modifier = Modifier.height(8.dp).width(width).clip(CircleShape),
                            color = when {
                                set.isDone -> MaterialTheme.colorScheme.primary
                                index == si -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {}
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "SET ${si + 1} OF ${currentExercise.sets.size}",
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
                        onIncrement = { onIncrementReps(ei, si) },
                        onDecrement = { onDecrementReps(ei, si) },
                        modifier = Modifier.weight(1f)
                    )
                    StepperCard(
                        label = "kg",
                        value = currentSet.weight,
                        onIncrement = { onIncrementWeight(ei, si) },
                        onDecrement = { onDecrementWeight(ei, si) },
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
                val isLastSet = si == currentExercise.sets.size - 1
                val isLastExercise = ei == exercises.size - 1
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

                // Navigation row — Back and Skip
                val isAtStart = ei == 0 && si == 0
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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

                Spacer(Modifier.height(12.dp))

                // Completed sets chips
                val doneSets = currentExercise.sets.filter { it.isDone }
                AnimatedVisibility(
                    visible = doneSets.isNotEmpty(),
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        currentExercise.sets.forEachIndexed { index, set ->
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
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No exercises in this workout.")
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        ExerciseSelectionDialog(
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
                TextButton(onClick = {
                    onCancelWorkout()
                }) {
                    Text("Cancel Workout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Keep Going") }
            }
        )
    }
}

@Composable
fun StepperCard(
    label: String,
    value: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onDecrement,
                    shape = CircleShape,
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("−", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onIncrement,
                    shape = CircleShape,
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ExerciseSelectionDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onExerciseSelected: (Exercise) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select Exercise",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(exercises) { _, exercise ->
                        Surface(
                            onClick = { onExerciseSelected(exercise) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = exercise.muscleGroup,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 16.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

fun formatElapsedTime(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
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
            availableExercises = listOf(
                Exercise(id = "e3", name = "Incline Press", muscleGroup = "Chest")
            ),
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

@Preview(showBackground = true)
@Composable
fun StepperCardPreview() {
    WorkoutPlannerTheme {
        StepperCard(
            label = "Reps",
            value = "10",
            onIncrement = {},
            onDecrement = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ExerciseSelectionDialogPreview() {
    WorkoutPlannerTheme {
        ExerciseSelectionDialog(
            exercises = listOf(
                Exercise(id = "e1", name = "Bench Press", muscleGroup = "Chest"),
                Exercise(id = "e2", name = "Squat", muscleGroup = "Legs"),
                Exercise(id = "e3", name = "Pull-up", muscleGroup = "Back")
            ),
            onDismiss = {},
            onExerciseSelected = {}
        )
    }
}