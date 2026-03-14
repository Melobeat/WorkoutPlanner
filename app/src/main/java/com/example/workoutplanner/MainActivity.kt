package com.example.workoutplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.workoutplanner.model.Routine
import com.example.workoutplanner.model.WorkoutDay
import com.example.workoutplanner.ui.*
import com.example.workoutplanner.ui.theme.WorkoutPlannerTheme
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkoutPlannerTheme {
                WorkoutPlannerApp()
            }
        }
    }
}

@Composable
fun WorkoutPlannerApp() {
    val context = LocalContext.current
    val application = context.applicationContext as WorkoutApplication
    val viewModel: WorkoutViewModel = viewModel(
        factory = WorkoutViewModelFactory(application.database.workoutDao())
    )

    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var isSettingsVisible by rememberSaveable { mutableStateOf(false) }
    var settingsSubDestination by rememberSaveable { mutableStateOf<SettingsDestinations?>(null) }
    
    var selectedRoutineInDetail by rememberSaveable { mutableStateOf<Routine?>(null) }
    var routineToEdit by rememberSaveable { mutableStateOf<Routine?>(null) }
    var isCreatingRoutine by rememberSaveable { mutableStateOf(false) }

    var activeWorkoutDay by remember { mutableStateOf<Pair<WorkoutDay, Int>?>(null) }

    val routines by viewModel.routines.collectAsState()
    val activeRoutine by viewModel.selectedRoutine.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val equipment by viewModel.equipment.collectAsState()

    // Handle back button for nested navigation
    val currentWorkoutDay = activeWorkoutDay
    if (currentWorkoutDay != null) {
        BackHandler {
            activeWorkoutDay = null
        }
    } else if (isSettingsVisible) {
        BackHandler {
            when {
                routineToEdit != null || isCreatingRoutine -> {
                    routineToEdit = null
                    isCreatingRoutine = false
                }
                selectedRoutineInDetail != null -> {
                    selectedRoutineInDetail = null
                }
                settingsSubDestination != null -> {
                    settingsSubDestination = null
                }
                else -> {
                    isSettingsVisible = false
                }
            }
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination && !isSettingsVisible && activeWorkoutDay == null,
                    onClick = {
                        currentDestination = it
                        isSettingsVisible = false
                        settingsSubDestination = null
                        activeWorkoutDay = null
                    }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            
            val workoutDay = activeWorkoutDay
            if (workoutDay != null) {
                WorkoutScreen(
                    workoutDay = workoutDay.first,
                    onFinishWorkout = { history ->
                        history.forEach {
                            // Passing 1 for sets as each history item represents one set
                            viewModel.logExerciseHistory(it.exerciseId, 1, it.reps, it.weight)
                        }
                        activeRoutine?.let {
                            viewModel.completeWorkoutDay(it.id, workoutDay.second)
                        }
                        activeWorkoutDay = null
                    },
                    onBack = { activeWorkoutDay = null },
                    modifier = modifier
                )
            } else if (isSettingsVisible) {
                when (settingsSubDestination) {
                    SettingsDestinations.EXERCISES -> {
                        ExercisesScreen(
                            exercises = exercises,
                            equipmentList = equipment,
                            onAddExercise = { name, muscle, desc, equipId ->
                                viewModel.addExercise(name, muscle, desc, equipId)
                            },
                            onUpdateExercise = { exercise ->
                                viewModel.updateExercise(exercise)
                            },
                            onDeleteExercise = { viewModel.deleteExercise(it) },
                            onBack = { settingsSubDestination = null },
                            modifier = modifier
                        )
                    }
                    SettingsDestinations.EQUIPMENT -> {
                        EquipmentScreen(
                            equipmentList = equipment,
                            onAddEquipment = { viewModel.addEquipment(it) },
                            onUpdateEquipment = { viewModel.updateEquipment(it) },
                            onDeleteEquipment = { viewModel.deleteEquipment(it) },
                            onBack = { settingsSubDestination = null },
                            modifier = modifier
                        )
                    }
                    SettingsDestinations.ROUTINES -> {
                        when {
                            isCreatingRoutine || routineToEdit != null -> {
                                CreateRoutineScreen(
                                    initialRoutine = routineToEdit,
                                    availableExercises = exercises,
                                    onSave = { name, desc, days ->
                                        viewModel.saveRoutine(name, desc, days, routineToEdit?.id)
                                        isCreatingRoutine = false
                                        routineToEdit = null
                                    },
                                    onBack = {
                                        isCreatingRoutine = false
                                        routineToEdit = null
                                    },
                                    modifier = modifier
                                )
                            }
                            selectedRoutineInDetail != null -> {
                                RoutineDetailScreen(
                                    routine = selectedRoutineInDetail!!,
                                    onBackClick = { selectedRoutineInDetail = null },
                                    onEditClick = {
                                        routineToEdit = selectedRoutineInDetail
                                        selectedRoutineInDetail = null
                                    },
                                    modifier = modifier
                                )
                            }
                            else -> {
                                RoutinesScreen(
                                    routines = routines,
                                    onRoutineClick = { selectedRoutineInDetail = it },
                                    onCreateRoutineClick = { isCreatingRoutine = true },
                                    onDeleteRoutine = { viewModel.deleteRoutine(it) },
                                    onSelectRoutine = { viewModel.selectRoutine(it) },
                                    onBack = { settingsSubDestination = null },
                                    modifier = modifier
                                )
                            }
                        }
                    }
                    null -> {
                        SettingsScreen(
                            onNavigateToExercises = { settingsSubDestination = SettingsDestinations.EXERCISES },
                            onNavigateToRoutines = { settingsSubDestination = SettingsDestinations.ROUTINES },
                            onNavigateToEquipment = { settingsSubDestination = SettingsDestinations.EQUIPMENT },
                            onBack = { isSettingsVisible = false },
                            modifier = modifier
                        )
                    }
                }
            } else {
                when (currentDestination) {
                    AppDestinations.HOME -> {
                        HomeScreen(
                            selectedRoutine = activeRoutine,
                            onStartWorkout = { day, index ->
                                activeWorkoutDay = day to index
                            },
                            onSettingsClick = { isSettingsVisible = true },
                            modifier = modifier
                        )
                    }
                    AppDestinations.HISTORY -> {
                        HistoryScreen(
                            viewModel = viewModel,
                            modifier = modifier
                        )
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Home", R.drawable.ic_home),
    HISTORY("History", R.drawable.ic_history),
}

enum class SettingsDestinations {
    EXERCISES,
    ROUTINES,
    EQUIPMENT
}
