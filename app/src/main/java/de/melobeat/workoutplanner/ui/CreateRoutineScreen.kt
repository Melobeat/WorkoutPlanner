package de.melobeat.workoutplanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.melobeat.workoutplanner.model.Exercise
import de.melobeat.workoutplanner.model.RoutineSet
import de.melobeat.workoutplanner.model.WorkoutDay
import de.melobeat.workoutplanner.ui.theme.WorkoutPlannerTheme
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineScreen(
    routineId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutinesViewModel = hiltViewModel(),
    exerciseLibraryViewModel: ExerciseLibraryViewModel = hiltViewModel()
) {
    LaunchedEffect(routineId) { routineId?.let { viewModel.loadRoutineDetail(it) } }
    val initialRoutine by if (routineId != null) viewModel.detailRoutine.collectAsStateWithLifecycle()
    else remember { kotlinx.coroutines.flow.MutableStateFlow<de.melobeat.workoutplanner.model.Routine?>(null) }.collectAsStateWithLifecycle()
    val saveComplete by viewModel.saveComplete.collectAsStateWithLifecycle()
    LaunchedEffect(saveComplete) {
        if (saveComplete) {
            viewModel.onSaveHandled()
            onBack()
        }
    }
    val exerciseLibState by exerciseLibraryViewModel.uiState.collectAsStateWithLifecycle()

    if (routineId != null && initialRoutine == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    CreateRoutineScreenContent(
        routineId = routineId,
        initialName = initialRoutine?.name ?: "",
        initialDescription = initialRoutine?.description ?: "",
        initialDays = initialRoutine?.workoutDays ?: emptyList(),
        availableExercises = exerciseLibState.exercises,
        onSave = { name, desc, days -> viewModel.saveRoutine(name, desc, days, routineId) },
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineScreenContent(
    routineId: String?,
    initialName: String,
    initialDescription: String,
    initialDays: List<WorkoutDay>,
    availableExercises: List<Exercise>,
    onSave: (name: String, description: String, days: List<WorkoutDay>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var description by remember(initialDescription) { mutableStateOf(initialDescription) }
    var days by remember(initialDays) { mutableStateOf(initialDays) }
    var showExercisePickerForDayIndex by remember { mutableStateOf<Int?>(null) }
    val expandedDays = remember { mutableStateMapOf<String, Boolean>() }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (routineId == null) "New Routine" else "Edit Routine", fontWeight = FontWeight.Black) },
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
                        days = days.toMutableList().apply { this[dayIndex] = day.copy(name = newName) }
                    },
                    onAddExercise = { showExercisePickerForDayIndex = dayIndex },
                    onRemoveDay = {
                        days = days.toMutableList().apply { removeAt(dayIndex) }
                    },
                    onMoveUp = if (dayIndex > 0) { {
                        days = days.toMutableList().apply { val item = removeAt(dayIndex); add(dayIndex - 1, item) }
                    } } else null,
                    onMoveDown = if (dayIndex < days.size - 1) { {
                        days = days.toMutableList().apply { val item = removeAt(dayIndex); add(dayIndex + 1, item) }
                    } } else null,
                    onUpdateExercise = { exIndex, updatedEx ->
                        days = days.toMutableList().apply {
                            this[dayIndex] = day.copy(exercises = day.exercises.toMutableList().apply { this[exIndex] = updatedEx })
                        }
                    },
                    onRemoveExercise = { exIndex ->
                        days = days.toMutableList().apply {
                            this[dayIndex] = day.copy(exercises = day.exercises.toMutableList().apply { removeAt(exIndex) })
                        }
                    },
                    onMoveExerciseUp = { exIndex ->
                        if (exIndex > 0) {
                            days = days.toMutableList().apply {
                                this[dayIndex] = day.copy(exercises = day.exercises.toMutableList().apply {
                                    val item = removeAt(exIndex); add(exIndex - 1, item)
                                })
                            }
                        }
                    },
                    onMoveExerciseDown = { exIndex ->
                        if (exIndex < day.exercises.size - 1) {
                            days = days.toMutableList().apply {
                                this[dayIndex] = day.copy(exercises = day.exercises.toMutableList().apply {
                                    val item = removeAt(exIndex); add(exIndex + 1, item)
                                })
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
                    Icon(Icons.Rounded.Add, contentDescription = null)
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

@Preview(showBackground = true)
@Composable
fun CreateRoutineScreenContentPreview() {
    WorkoutPlannerTheme {
        CreateRoutineScreenContent(
            routineId = null,
            initialName = "Push Pull Legs",
            initialDescription = "3-day strength split",
            initialDays = listOf(
                WorkoutDay(
                    id = "d1",
                    name = "Push Day",
                    exercises = listOf(
                        Exercise(id = "e1", name = "Bench Press", muscleGroup = "Chest",
                            routineSets = listOf(RoutineSet(10, 60.0), RoutineSet(10, 60.0)))
                    )
                ),
                WorkoutDay(id = "d2", name = "Pull Day", exercises = listOf())
            ),
            availableExercises = listOf(
                Exercise(id = "e1", name = "Bench Press", muscleGroup = "Chest"),
                Exercise(id = "e2", name = "Pull-up", muscleGroup = "Back")
            ),
            onSave = { _, _, _ -> },
            onBack = {}
        )
    }
}
