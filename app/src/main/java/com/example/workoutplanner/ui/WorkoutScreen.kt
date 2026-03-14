package com.example.workoutplanner.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.workoutplanner.model.Exercise
import com.example.workoutplanner.model.WorkoutDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    workoutDay: WorkoutDay,
    onFinishWorkout: (List<ExerciseHistory>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exerciseStates = remember {
        workoutDay.exercises.map { exercise ->
            ExerciseState(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                initialSets = exercise.sets
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workoutDay.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val history = exerciseStates.flatMap { state ->
                            state.sets.filter { it.reps.isNotEmpty() && it.weight.isNotEmpty() }
                                .map { set ->
                                    ExerciseHistory(
                                        exerciseId = state.exerciseId,
                                        reps = set.reps.toIntOrNull() ?: 0,
                                        weight = set.weight.toDoubleOrNull() ?: 0.0
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
            items(exerciseStates) { state ->
                ExerciseCard(
                    state = state,
                    onAddSet = { state.addSet() },
                    onRemoveSet = { state.removeSet(it) }
                )
            }
            
            item {
                Button(
                    onClick = {
                        val history = exerciseStates.flatMap { state ->
                            state.sets.filter { it.reps.isNotEmpty() && it.weight.isNotEmpty() }
                                .map { set ->
                                    ExerciseHistory(
                                        exerciseId = state.exerciseId,
                                        reps = set.reps.toIntOrNull() ?: 0,
                                        weight = set.weight.toDoubleOrNull() ?: 0.0
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
}

@Composable
fun ExerciseCard(
    state: ExerciseState,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit
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
                Text(
                    text = state.exerciseName,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onAddSet) {
                    Icon(Icons.Default.Add, contentDescription = "Add Set")
                }
            }
            
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
        }
    }
}

class ExerciseState(
    val exerciseId: String,
    val exerciseName: String,
    initialSets: Int
) {
    val sets = mutableStateListOf<SetState>().apply {
        addAll(List(initialSets) { SetState() })
    }

    fun addSet() {
        sets.add(SetState())
    }

    fun removeSet(index: Int) {
        if (sets.size > 0) {
            sets.removeAt(index)
        }
    }
}

class SetState {
    var weight by mutableStateOf("")
    var reps by mutableStateOf("")
}

data class ExerciseHistory(
    val exerciseId: String,
    val reps: Int,
    val weight: Double
)
