package com.example.workoutplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workoutplanner.model.Routine
import com.example.workoutplanner.ui.*
import com.example.workoutplanner.ui.theme.WorkoutPlannerTheme
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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
    val viewModel: WorkoutViewModel = hiltViewModel()

    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var isSettingsVisible by rememberSaveable { mutableStateOf(false) }
    var settingsSubDestination by rememberSaveable { mutableStateOf<SettingsDestinations?>(null) }
    
    var selectedRoutineInDetail by rememberSaveable { mutableStateOf<Routine?>(null) }
    var routineToEdit by rememberSaveable { mutableStateOf<Routine?>(null) }
    var isCreatingRoutine by rememberSaveable { mutableStateOf(false) }

    val activeWorkout by viewModel.activeWorkout.collectAsState()
    var isWorkoutMinimized by rememberSaveable { mutableStateOf(false) }

    val routines by viewModel.routines.collectAsState()
    val activeRoutine by viewModel.selectedRoutine.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val equipment by viewModel.equipment.collectAsState()
    val workoutHistory by viewModel.workoutHistory.collectAsState()

    val exerciseNameMap = remember(exercises) {
        exercises.associate { it.id to it.name }
    }

    // Handle back button for nested navigation
    if (activeWorkout != null && !isWorkoutMinimized) {
        BackHandler {
            isWorkoutMinimized = true
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
                    selected = it == currentDestination && !isSettingsVisible,
                    onClick = {
                        currentDestination = it
                        isSettingsVisible = false
                        settingsSubDestination = null
                        if (activeWorkout != null) {
                            isWorkoutMinimized = true
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                val workout = activeWorkout
                if (workout != null && isWorkoutMinimized) {
                    Surface(
                        onClick = { isWorkoutMinimized = false },
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Active Workout",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        text = workout.workoutDay.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            TextButton(onClick = { isWorkoutMinimized = false }) {
                                Text("Resume")
                            }
                        }
                    }
                }
            }
        ) { scaffoldPadding ->
            val contentModifier = Modifier.padding(scaffoldPadding)
            
            val workout = activeWorkout
            if (workout != null && !isWorkoutMinimized) {
                WorkoutScreen(
                    workoutDay = workout.workoutDay,
                    exerciseStates = workout.exerciseStates,
                    availableExercises = exercises,
                    elapsedTime = workout.elapsedTime,
                    onFinishWorkout = { history ->
                        viewModel.finishWorkout(history)
                        isWorkoutMinimized = false
                    },
                    onCancelWorkout = { 
                        viewModel.cancelWorkout()
                        isWorkoutMinimized = false
                    },
                    onMinimize = { isWorkoutMinimized = true },
                    modifier = contentModifier
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
                            modifier = contentModifier
                        )
                    }
                    SettingsDestinations.EQUIPMENT -> {
                        EquipmentScreen(
                            equipmentList = equipment,
                            onAddEquipment = { viewModel.addEquipment(it) },
                            onUpdateEquipment = { viewModel.updateEquipment(it) },
                            onDeleteEquipment = { viewModel.deleteEquipment(it) },
                            onBack = { settingsSubDestination = null },
                            modifier = contentModifier
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
                                    modifier = contentModifier
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
                                    modifier = contentModifier
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
                                    modifier = contentModifier
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
                            modifier = contentModifier
                        )
                    }
                }
            } else {
                when (currentDestination) {
                    AppDestinations.HOME -> {
                        HomeScreen(
                            selectedRoutine = activeRoutine,
                            workoutHistory = workoutHistory,
                            exerciseNameMap = exerciseNameMap,
                            onStartWorkout = { day, index ->
                                viewModel.startWorkout(day, index)
                                isWorkoutMinimized = false
                            },
                            onUpdateNextDay = { index ->
                                activeRoutine?.let { routine ->
                                    val totalDays = routine.workoutDays.size
                                    val lastCompletedIndex = (index + totalDays - 1) % totalDays
                                    viewModel.completeWorkoutDay(routine.id, lastCompletedIndex)
                                }
                            },
                            onSettingsClick = { isSettingsVisible = true },
                            modifier = contentModifier
                        )
                    }
                    AppDestinations.HISTORY -> {
                        HistoryScreen(
                            viewModel = viewModel,
                            modifier = contentModifier
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
