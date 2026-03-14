package com.example.workoutplanner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.workoutplanner.model.Exercise
import com.example.workoutplanner.model.Routine
import com.example.workoutplanner.model.WorkoutDay
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineScreen(
    availableExercises: List<Exercise>,
    onSave: (name: String, description: String, days: List<WorkoutDay>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialRoutine: Routine? = null
) {
    var name by remember { mutableStateOf(initialRoutine?.name ?: "") }
    var description by remember { mutableStateOf(initialRoutine?.description ?: "") }
    var days by remember { mutableStateOf(initialRoutine?.workoutDays ?: listOf()) }
    var showExercisePickerForDayIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialRoutine == null) "New Routine" else "Edit Routine") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onSave(name, description, days) },
                        enabled = name.isNotBlank() && days.isNotEmpty()
                    ) {
                        Text("Save")
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
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Routine Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            itemsIndexed(days) { dayIndex, day ->
                DayCard(
                    day = day,
                    onNameChange = { newName ->
                        days = days.toMutableList().apply {
                            this[dayIndex] = day.copy(name = newName)
                        }
                    },
                    onAddExercise = {
                        showExercisePickerForDayIndex = dayIndex
                    },
                    onRemoveDay = {
                        days = days.toMutableList().apply { removeAt(dayIndex) }
                    },
                    onUpdateExercise = { exIndex, updatedEx ->
                        days = days.toMutableList().apply {
                            val updatedExercises = day.exercises.toMutableList().apply {
                                this[exIndex] = updatedEx
                            }
                            this[dayIndex] = day.copy(exercises = updatedExercises)
                        }
                    },
                    onRemoveExercise = { exIndex ->
                        days = days.toMutableList().apply {
                            val updatedExercises = day.exercises.toMutableList().apply {
                                removeAt(exIndex)
                            }
                            this[dayIndex] = day.copy(exercises = updatedExercises)
                        }
                    }
                )
            }

            item {
                Button(
                    onClick = {
                        days = days + WorkoutDay(id = "temp_${UUID.randomUUID()}", name = "Day ${days.size + 1}")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Day")
                }
            }
        }

        if (showExercisePickerForDayIndex != null) {
            ExercisePicker(
                exercises = availableExercises,
                onDismiss = { showExercisePickerForDayIndex = null },
                onExerciseSelected = { exercise ->
                    val dayIndex = showExercisePickerForDayIndex!!
                    days = days.toMutableList().apply {
                        val day = this[dayIndex]
                        this[dayIndex] = day.copy(exercises = day.exercises + exercise.copy(sets = 3, reps = 10))
                    }
                    showExercisePickerForDayIndex = null
                }
            )
        }
    }
}

@Composable
fun DayCard(
    day: WorkoutDay,
    onNameChange: (String) -> Unit,
    onAddExercise: () -> Unit,
    onRemoveDay: () -> Unit,
    onUpdateExercise: (Int, Exercise) -> Unit,
    onRemoveExercise: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = day.name,
                    onValueChange = onNameChange,
                    label = { Text("Day Name") },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onRemoveDay) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove Day")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            day.exercises.forEachIndexed { index, exercise ->
                ExerciseEditItem(
                    exercise = exercise,
                    onUpdate = { updated -> onUpdateExercise(index, updated) },
                    onRemove = { onRemoveExercise(index) }
                )
                if (index < day.exercises.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            TextButton(
                onClick = onAddExercise,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add Exercise")
            }
        }
    }
}

@Composable
fun ExerciseEditItem(
    exercise: Exercise,
    onUpdate: (Exercise) -> Unit,
    onRemove: () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(exercise.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove Exercise")
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = if (exercise.sets == 0) "" else exercise.sets.toString(),
                onValueChange = {
                    val sets = it.toIntOrNull() ?: 0
                    onUpdate(exercise.copy(sets = sets))
                },
                label = { Text("Sets") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = if (exercise.reps == 0) "" else exercise.reps.toString(),
                onValueChange = {
                    val reps = it.toIntOrNull() ?: 0
                    onUpdate(exercise.copy(reps = reps))
                },
                label = { Text("Reps") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = if (exercise.weight == 0.0) "" else exercise.weight.toString(),
                onValueChange = {
                    val weight = it.toDoubleOrNull() ?: 0.0
                    onUpdate(exercise.copy(weight = weight))
                },
                label = { Text("Weight") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

@Composable
fun ExercisePicker(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onExerciseSelected: (Exercise) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Exercise") },
        text = {
            LazyColumn {
                itemsIndexed(exercises) { _, exercise ->
                    ListItem(
                        headlineContent = { Text(exercise.name) },
                        supportingContent = { Text(exercise.muscleGroup) },
                        modifier = Modifier.fillMaxWidth().clickable { onExerciseSelected(exercise) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
