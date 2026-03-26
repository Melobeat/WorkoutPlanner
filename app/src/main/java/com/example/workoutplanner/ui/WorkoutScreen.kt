package com.example.workoutplanner.ui

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.workoutplanner.model.Exercise
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    ),
    exerciseLibraryViewModel: ExerciseLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exerciseLibState by exerciseLibraryViewModel.uiState.collectAsStateWithLifecycle()
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var exerciseToSwapIndex by remember { mutableStateOf<Int?>(null) }
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text(uiState.workoutDayName)
                    Text(
                        text = formatElapsedTime(uiState.elapsedTime),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }, navigationIcon = {
                IconButton(onClick = { showCancelDialog = true }) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel Workout")
                }
            }, actions = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ExpandMore, contentDescription = "Minimize")
                }
                IconButton(onClick = { showAddExerciseDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Exercise")
                }
                IconButton(onClick = { viewModel.finishWorkout() }) {
                    Icon(Icons.Default.Check, contentDescription = "Finish")
                }
            })
        }, modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(uiState.exercises) { index, state ->
                ExerciseCard(
                    state = state,
                    onToggleExpand = { viewModel.toggleExerciseExpanded(index) },
                    onAddSet = { viewModel.addSet(index) },
                    onRemoveSet = { setIndex -> viewModel.removeSet(index, setIndex) },
                    onRemoveExercise = { viewModel.removeExercise(index) },
                    onSwapExercise = { exerciseToSwapIndex = index },
                    onMoveUp = if (index > 0) {
                        { viewModel.reorderExercise(index, index - 1) }
                    } else null,
                    onMoveDown = if (index < uiState.exercises.size - 1) {
                        { viewModel.reorderExercise(index, index + 1) }
                    } else null,
                    onToggleSetDone = { setIndex -> viewModel.toggleSetDone(index, setIndex) },
                    onUpdateSetReps = { setIndex, reps -> viewModel.updateSetReps(index, setIndex, reps) },
                    onUpdateSetWeight = { setIndex, weight -> viewModel.updateSetWeight(index, setIndex, weight) }
                )
            }

            item {
                Button(
                    onClick = { viewModel.finishWorkout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Text("Finish Workout")
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        ExerciseSelectionDialog(
            exercises = exerciseLibState.exercises,
            onDismiss = { showAddExerciseDialog = false },
            onExerciseSelected = { exercise ->
                viewModel.addExercise(exercise)
                showAddExerciseDialog = false
            })
    }

    if (exerciseToSwapIndex != null) {
        ExerciseSelectionDialog(
            exercises = exerciseLibState.exercises,
            onDismiss = { exerciseToSwapIndex = null },
            onExerciseSelected = { exercise ->
                viewModel.swapExercise(exerciseToSwapIndex!!, exercise)
                exerciseToSwapIndex = null
            })
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Workout?") },
            text = { Text("All progress in this session will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelWorkout()
                    onNavigateBack()
                }) {
                    Text("Cancel Workout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Going")
                }
            })
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExerciseCard(
    state: ExerciseUiState,
    onToggleExpand: () -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onRemoveExercise: () -> Unit,
    onSwapExercise: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onToggleSetDone: (Int) -> Unit,
    onUpdateSetReps: (Int, String) -> Unit,
    onUpdateSetWeight: (Int, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleExpand() }) {
                    Icon(
                        if (state.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = state.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row {
                    onMoveUp?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                        }
                    }
                    onMoveDown?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                        }
                    }
                    IconButton(onClick = onSwapExercise) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Swap Exercise")
                    }
                    IconButton(onClick = onRemoveExercise) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove Exercise",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            AnimatedVisibility(visible = state.isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))

                    state.sets.forEachIndexed { index, setState ->
                        var showRepsDialog by remember { mutableStateOf(false) }

                        if (showRepsDialog) {
                            RepsDialog(
                                initialReps = setState.reps,
                                onDismiss = { showRepsDialog = false },
                                onConfirm = { reps ->
                                    onUpdateSetReps(index, reps)
                                    showRepsDialog = false
                                })
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Set ${index + 1}", modifier = Modifier.width(50.dp)
                            )

                            OutlinedTextField(
                                value = setState.weight,
                                onValueChange = { onUpdateSetWeight(index, it) },
                                label = { Text("kg") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(
                                        if (setState.isDone) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .combinedClickable(onClick = {
                                        if (setState.isAmrap) {
                                            showRepsDialog = true
                                        } else {
                                            onToggleSetDone(index)
                                        }
                                    }, onLongClick = {
                                        showRepsDialog = true
                                    }), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (setState.isAmrap) "${setState.reps}+" else setState.reps,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (setState.isDone) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (setState.isAmrap) "reps+" else "reps",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (setState.isDone) MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                            alpha = 0.7f
                                        )
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            IconButton(onClick = { onRemoveSet(index) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove Set",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = onAddSet, modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Add Set")
                    }
                }
            }
        }
    }
}

@Composable
fun RepsDialog(
    initialReps: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit
) {
    var reps by remember { mutableStateOf(initialReps) }

    AlertDialog(onDismissRequest = onDismiss, title = {
        Text(
            "Edit Reps", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
        )
    }, text = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = {
                val r = reps.toIntOrNull() ?: 0
                if (r > 0) reps = (r - 1).toString()
            }) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }

            OutlinedTextField(
                value = reps,
                onValueChange = { if (it.all { char -> char.isDigit() }) reps = it },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center)
            )

            IconButton(onClick = {
                val r = reps.toIntOrNull() ?: 0
                reps = (r + 1).toString()
            }) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
        }
    }, confirmButton = {
        TextButton(onClick = { onConfirm(reps) }) {
            Text("OK")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    })
}

@Composable
fun ExerciseSelectionDialog(
    exercises: List<Exercise>, onDismiss: () -> Unit, onExerciseSelected: (Exercise) -> Unit
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
                    modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)
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

data class ExerciseHistory(
    val exerciseId: String,
    val reps: Int,
    val weight: Double,
    val setIndex: Int = 1,
    val isAmrap: Boolean = false
)

fun formatWeight(weight: Double): String {
    return if (weight % 1.0 == 0.0) weight.toInt().toString() else weight.toString()
}
