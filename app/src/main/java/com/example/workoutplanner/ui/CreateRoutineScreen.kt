package com.example.workoutplanner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.workoutplanner.model.Exercise
import com.example.workoutplanner.model.RoutineSet
import com.example.workoutplanner.model.WorkoutDay
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineScreen(
    routineId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutinesViewModel = viewModel(),
    exerciseLibraryViewModel: ExerciseLibraryViewModel = viewModel()
) {
    LaunchedEffect(routineId) { routineId?.let { viewModel.loadRoutineDetail(it) } }
    val initialRoutine by if (routineId != null) viewModel.detailRoutine.collectAsStateWithLifecycle()
    else remember { kotlinx.coroutines.flow.MutableStateFlow<com.example.workoutplanner.model.Routine?>(null) }.collectAsStateWithLifecycle()
    val saveComplete by viewModel.saveComplete.collectAsStateWithLifecycle()
    LaunchedEffect(saveComplete) {
        if (saveComplete) {
            viewModel.onSaveHandled()
            onBack()
        }
    }
    val exerciseLibState by exerciseLibraryViewModel.uiState.collectAsStateWithLifecycle()
    val availableExercises = exerciseLibState.exercises

    var name by remember(initialRoutine) { mutableStateOf(initialRoutine?.name ?: "") }
    var description by remember(initialRoutine) { mutableStateOf(initialRoutine?.description ?: "") }
    var days by remember(initialRoutine) { mutableStateOf(initialRoutine?.workoutDays ?: listOf()) }
    var showExercisePickerForDayIndex by remember { mutableStateOf<Int?>(null) }

    // Track expanded state for days
    val expandedDays = remember { mutableStateMapOf<String, Boolean>() }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(if (routineId == null) "New Routine" else "Edit Routine", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveRoutine(name, description, days, routineId) },
                        enabled = name.isNotBlank() && days.isNotEmpty()
                    ) {
                        Text("Save")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
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
                val isExpanded = expandedDays[day.id] ?: true
                DayCard(
                    day = day,
                    isExpanded = isExpanded,
                    onToggleExpand = { expandedDays[day.id] = !isExpanded },
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
                    onMoveUp = if (dayIndex > 0) { {
                        days = days.toMutableList().apply {
                            val item = removeAt(dayIndex)
                            add(dayIndex - 1, item)
                        }
                    } } else null,
                    onMoveDown = if (dayIndex < days.size - 1) { {
                        days = days.toMutableList().apply {
                            val item = removeAt(dayIndex)
                            add(dayIndex + 1, item)
                        }
                    } } else null,
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
                    },
                    onMoveExerciseUp = { exIndex ->
                        if (exIndex > 0) {
                            days = days.toMutableList().apply {
                                val updatedExercises = day.exercises.toMutableList().apply {
                                    val item = removeAt(exIndex)
                                    add(exIndex - 1, item)
                                }
                                this[dayIndex] = day.copy(exercises = updatedExercises)
                            }
                        }
                    },
                    onMoveExerciseDown = { exIndex ->
                        if (exIndex < day.exercises.size - 1) {
                            days = days.toMutableList().apply {
                                val updatedExercises = day.exercises.toMutableList().apply {
                                    val item = removeAt(exIndex)
                                    add(exIndex + 1, item)
                                }
                                this[dayIndex] = day.copy(exercises = updatedExercises)
                            }
                        }
                    }
                )
            }

            item {
                Button(
                    onClick = {
                        val newDayId = UUID.randomUUID().toString()
                        days = days + WorkoutDay(id = newDayId, name = "Day ${days.size + 1}")
                        expandedDays[newDayId] = true
                    },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
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
                        this[dayIndex] = day.copy(exercises = day.exercises + exercise.copy(
                            routineSets = listOf(RoutineSet(10, 0.0), RoutineSet(10, 0.0), RoutineSet(10, 0.0))
                        ))
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
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onNameChange: (String) -> Unit,
    onAddExercise: () -> Unit,
    onRemoveDay: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onUpdateExercise: (Int, Exercise) -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onMoveExerciseUp: (Int) -> Unit,
    onMoveExerciseDown: (Int) -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.clickable { onToggleExpand() }
                )
                OutlinedTextField(
                    value = day.name,
                    onValueChange = onNameChange,
                    label = { Text("Day Name") },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.titleMedium
                )
                onMoveUp?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Day Up")
                    }
                }
                onMoveDown?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Day Down")
                    }
                }
                IconButton(onClick = onRemoveDay) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove Day")
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    day.exercises.forEachIndexed { index, exercise ->
                        ExerciseEditItem(
                            exercise = exercise,
                            onUpdate = { updated -> onUpdateExercise(index, updated) },
                            onRemove = { onRemoveExercise(index) },
                            onMoveUp = if (index > 0) { { onMoveExerciseUp(index) } } else null,
                            onMoveDown = if (index < day.exercises.size - 1) { { onMoveExerciseDown(index) } } else null
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
    }
}

@Composable
fun ExerciseEditItem(
    exercise: Exercise,
    onUpdate: (Exercise) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.clickable { isExpanded = !isExpanded }
            )
            Text(
                exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).clickable { isExpanded = !isExpanded },
                fontWeight = FontWeight.Bold
            )
            onMoveUp?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Exercise Up")
                }
            }
            onMoveDown?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Exercise Down")
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove Exercise")
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                exercise.routineSets.forEachIndexed { index, set ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text("${index + 1}", modifier = Modifier.width(20.dp))

                        var repsText by remember(set.reps) { mutableStateOf(set.reps.toString()) }
                        OutlinedTextField(
                            value = repsText,
                            onValueChange = {
                                repsText = it
                                val newReps = it.toIntOrNull()
                                if (newReps != null) {
                                    val newSets = exercise.routineSets.toMutableList()
                                    newSets[index] = set.copy(reps = newReps)
                                    onUpdate(exercise.copy(routineSets = newSets))
                                }
                            },
                            label = { Text("Reps") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        var weightText by remember(set.weight) { mutableStateOf(set.weight.toString()) }
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = {
                                weightText = it
                                val newWeight = it.toDoubleOrNull()
                                if (newWeight != null) {
                                    val newSets = exercise.routineSets.toMutableList()
                                    newSets[index] = set.copy(weight = newWeight)
                                    onUpdate(exercise.copy(routineSets = newSets))
                                }
                            },
                            label = { Text("Weight") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("AMRAP", style = MaterialTheme.typography.labelSmall)
                            Checkbox(
                                checked = set.isAmrap,
                                onCheckedChange = { isChecked ->
                                    val newSets = exercise.routineSets.toMutableList()
                                    newSets[index] = set.copy(isAmrap = isChecked)
                                    onUpdate(exercise.copy(routineSets = newSets))
                                }
                            )
                        }

                        IconButton(onClick = {
                            val newSets = exercise.routineSets.toMutableList().apply { removeAt(index) }
                            onUpdate(exercise.copy(routineSets = newSets))
                        }) {
                            Icon(Icons.Default.Remove, contentDescription = "Remove Set")
                        }
                    }
                }
                TextButton(
                    onClick = {
                        val newSets = exercise.routineSets + RoutineSet(10, 0.0)
                        onUpdate(exercise.copy(routineSets = newSets))
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Add Set")
                }
            }
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
