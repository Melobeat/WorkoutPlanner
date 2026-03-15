package com.example.workoutplanner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.workoutplanner.model.Exercise
import com.example.workoutplanner.model.RoutineSet
import com.example.workoutplanner.model.WorkoutDay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    workoutDay: WorkoutDay,
    exerciseStates: SnapshotStateList<ExerciseState>,
    availableExercises: List<Exercise>,
    elapsedTime: Long,
    onFinishWorkout: (List<ExerciseHistory>) -> Unit,
    onCancelWorkout: () -> Unit,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var exerciseToSwapIndex by remember { mutableStateOf<Int?>(null) }
    var showCancelDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(workoutDay.name)
                        Text(
                            text = formatElapsedTime(elapsedTime),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showCancelDialog = true }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel Workout")
                    }
                },
                actions = {
                    IconButton(onClick = onMinimize) {
                        Icon(Icons.Default.ExpandMore, contentDescription = "Minimize")
                    }
                    IconButton(onClick = { showAddExerciseDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Exercise")
                    }
                    IconButton(onClick = {
                        val history = exerciseStates.flatMap { state ->
                            state.sets.filter { it.reps.isNotEmpty() && it.weight.isNotEmpty() }
                                .mapIndexed { setIndex, set ->
                                    ExerciseHistory(
                                        exerciseId = state.exerciseId,
                                        reps = set.reps.toIntOrNull() ?: 0,
                                        weight = set.weight.toDoubleOrNull() ?: 0.0,
                                        setIndex = setIndex + 1
                                    )
                                }
                        }
                        onFinishWorkout(history)
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Finish")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(exerciseStates) { index, state ->
                ExerciseCard(
                    state = state,
                    onAddSet = { state.addSet() },
                    onRemoveSet = { state.removeSet(it) },
                    onRemoveExercise = { exerciseStates.removeAt(index) },
                    onSwapExercise = { exerciseToSwapIndex = index },
                    onMoveUp = if (index > 0) { { 
                        val item = exerciseStates.removeAt(index)
                        exerciseStates.add(index - 1, item)
                    } } else null,
                    onMoveDown = if (index < exerciseStates.size - 1) { { 
                        val item = exerciseStates.removeAt(index)
                        exerciseStates.add(index + 1, item)
                    } } else null
                )
            }
            
            item {
                Button(
                    onClick = {
                        val history = exerciseStates.flatMap { state ->
                            state.sets.filter { it.reps.isNotEmpty() && it.weight.isNotEmpty() }
                                .mapIndexed { setIndex, set ->
                                    ExerciseHistory(
                                        exerciseId = state.exerciseId,
                                        reps = set.reps.toIntOrNull() ?: 0,
                                        weight = set.weight.toDoubleOrNull() ?: 0.0,
                                        setIndex = setIndex + 1
                                    )
                                }
                        }
                        onFinishWorkout(history)
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Text("Finish Workout")
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        ExerciseSelectionDialog(
            exercises = availableExercises,
            onDismiss = { },
            onExerciseSelected = { exercise ->
                exerciseStates.add(
                    ExerciseState(
                        exerciseId = exercise.id,
                        exerciseName = exercise.name,
                        initialSets = if (exercise.routineSets.isNotEmpty()) exercise.routineSets.size else 3,
                        predefinedSets = exercise.routineSets
                    )
                )
            }
        )
    }

    if (exerciseToSwapIndex != null) {
        ExerciseSelectionDialog(
            exercises = availableExercises,
            onDismiss = { },
            onExerciseSelected = { exercise ->
                val index = exerciseToSwapIndex!!
                exerciseStates[index] = ExerciseState(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    initialSets = if (exercise.routineSets.isNotEmpty()) exercise.routineSets.size else exerciseStates[index].sets.size,
                    predefinedSets = exercise.routineSets
                )
                exerciseToSwapIndex = null
            }
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { },
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
                TextButton(onClick = { }) {
                    Text("Continue")
                }
            }
        )
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

@Composable
fun ExerciseCard(
    state: ExerciseState,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onRemoveExercise: () -> Unit,
    onSwapExercise: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
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
                    modifier = Modifier.weight(1f).clickable { state.isExpanded = !state.isExpanded }
                ) {
                    Icon(
                        if (state.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = state.exerciseName,
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
                        Icon(Icons.Default.Delete, contentDescription = "Remove Exercise", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            AnimatedVisibility(visible = state.isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    state.sets.forEachIndexed { index, setState ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Set ${index + 1}",
                                modifier = Modifier.width(50.dp)
                            )
                            
                            OutlinedTextField(
                                value = setState.weight,
                                onValueChange = { setState.weight = it },
                                label = { Text("kg") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            
                            OutlinedTextField(
                                value = setState.reps,
                                onValueChange = { setState.reps = it },
                                label = { Text("reps") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

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
                        onClick = onAddSet,
                        modifier = Modifier.align(Alignment.End)
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
                                Text(text = exercise.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(text = exercise.muscleGroup, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End).padding(top = 16.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

class ExerciseState(
    val exerciseId: String,
    val exerciseName: String,
    initialSets: Int,
    predefinedSets: List<RoutineSet> = emptyList(),
    val lastSets: List<Pair<Double, Int>> = emptyList()
) {
    val sets = mutableStateListOf<SetState>().apply {
        val numSets = if (predefinedSets.isNotEmpty()) predefinedSets.size else maxOf(initialSets, lastSets.size)
        
        for (i in 0 until numSets) {
            val weight = lastSets.getOrNull(i)?.first?.let { formatWeight(it) }
                ?: predefinedSets.getOrNull(i)?.weight?.let { if (it > 0) formatWeight(it) else null }
                ?: "0"
            
            val reps = predefinedSets.getOrNull(i)?.reps?.toString() ?: "0"
            
            add(SetState(weight, reps))
        }
    }
    var isExpanded by mutableStateOf(true)

    fun addSet() {
        sets.add(SetState("0", "0"))
    }

    fun removeSet(index: Int) {
        if (sets.isNotEmpty()) {
            sets.removeAt(index)
        }
    }
}

class SetState(initialWeight: String = "0", initialReps: String = "0") {
    var weight by mutableStateOf(initialWeight)
    var reps by mutableStateOf(initialReps)
}

data class ExerciseHistory(
    val exerciseId: String,
    val reps: Int,
    val weight: Double,
    val setIndex: Int = 1
)

fun formatWeight(weight: Double): String {
    return if (weight % 1.0 == 0.0) weight.toInt().toString() else weight.toString()
}
