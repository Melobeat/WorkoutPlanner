package com.example.workoutplanner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutplanner.data.*
import com.example.workoutplanner.model.*
import com.example.workoutplanner.ui.ExerciseHistory
import com.example.workoutplanner.ui.ExerciseState
import com.example.workoutplanner.ui.SetState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WorkoutViewModel @Inject constructor(private val workoutDao: WorkoutDao) : ViewModel() {

    val routines: StateFlow<List<Routine>> = workoutDao.getAllRoutinesWithDays()
        .map { entities ->
            entities.map { it.toDomain() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedRoutine: StateFlow<Routine?> = workoutDao.getSelectedRoutineWithDays()
        .map { it?.toDomain() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val exercises: StateFlow<List<Exercise>> = workoutDao.getAllExercisesWithEquipment()
        .map { entities ->
            entities.map { it.toDomain() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val equipment: StateFlow<List<Equipment>> = workoutDao.getAllEquipment()
        .map { entities ->
            entities.map { it.toDomain() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHistory: StateFlow<List<ExerciseHistoryEntity>> = workoutDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        
    val workoutHistory: StateFlow<List<WorkoutHistoryWithExercises>> = workoutDao.getAllWorkoutHistoryWithExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Workout State
    private val _activeWorkout = MutableStateFlow<ActiveWorkout?>(null)
    val activeWorkout = _activeWorkout.asStateFlow()

    private var timerJob: Job? = null

    fun startWorkout(workoutDay: WorkoutDay, dayIndex: Int) {
        viewModelScope.launch {
            val exerciseStates = workoutDay.exercises.map { exercise ->
                // Fetch the latest history for this specific exercise
                val exerciseHistory = workoutDao.getHistoryForExercise(exercise.id).first()
                
                val lastSets = if (exerciseHistory.isNotEmpty()) {
                    // Find the most recent session's ID for this exercise
                    val latestWorkoutId = exerciseHistory[0].workoutHistoryId
                    // Get all sets from that specific session
                    exerciseHistory.filter { it.workoutHistoryId == latestWorkoutId }
                        .sortedBy { it.sets }
                        .map { it.weight to it.reps }
                } else {
                    emptyList()
                }
                
                ExerciseState(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    initialSets = if (exercise.routineSets.isNotEmpty()) exercise.routineSets.size else 3,
                    predefinedSets = exercise.routineSets,
                    lastSets = lastSets
                )
            }
            _activeWorkout.value = ActiveWorkout(
                workoutDay = workoutDay,
                dayIndex = dayIndex,
                exerciseStates = mutableStateListOf<ExerciseState>().apply { addAll(exerciseStates) },
                startTime = System.currentTimeMillis()
            )
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                _activeWorkout.value?.let { workout ->
                    workout.elapsedTime = System.currentTimeMillis() - workout.startTime
                }
                delay(1000)
            }
        }
    }

    fun finishWorkout(history: List<ExerciseHistory>) {
        val workout = _activeWorkout.value ?: return
        val duration = workout.elapsedTime
        timerJob?.cancel()
        
        viewModelScope.launch {
            val workoutHistoryId = UUID.randomUUID().toString()
            val workoutHistory = WorkoutHistoryEntity(
                id = workoutHistoryId,
                routineName = selectedRoutine.value?.name ?: "Custom Workout",
                workoutDayName = workout.workoutDay.name,
                date = System.currentTimeMillis(),
                durationMillis = duration
            )
            workoutDao.insertWorkoutHistory(workoutHistory)

            history.forEach {
                logExerciseHistory(workoutHistoryId, it.exerciseId, it.setIndex, it.reps, it.weight)
            }
            selectedRoutine.value?.let {
                completeWorkoutDay(it.id, workout.dayIndex)
            }
            _activeWorkout.value = null
        }
    }

    fun cancelWorkout() {
        timerJob?.cancel()
        _activeWorkout.value = null
    }

    // Equipment
    fun addEquipment(name: String) {
        viewModelScope.launch {
            workoutDao.insertEquipment(EquipmentEntity(name = name))
        }
    }

    fun updateEquipment(equipment: Equipment) {
        viewModelScope.launch {
            workoutDao.insertEquipment(EquipmentEntity(id = equipment.id, name = equipment.name))
        }
    }

    fun deleteEquipment(equipmentId: String) {
        viewModelScope.launch {
            workoutDao.deleteEquipment(equipmentId)
        }
    }

    // Exercises
    fun addExercise(name: String, muscleGroup: String, description: String, equipmentId: String?) {
        viewModelScope.launch {
            workoutDao.insertExercise(
                ExerciseEntity(
                    name = name,
                    muscleGroup = muscleGroup,
                    description = description,
                    equipmentId = equipmentId
                )
            )
        }
    }

    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch {
            workoutDao.insertExercise(
                ExerciseEntity(
                    id = exercise.id,
                    name = exercise.name,
                    muscleGroup = exercise.muscleGroup,
                    description = exercise.description,
                    equipmentId = exercise.equipmentId
                )
            )
        }
    }

    fun deleteExercise(exerciseId: String) {
        viewModelScope.launch {
            workoutDao.deleteExercise(exerciseId)
        }
    }

    fun saveRoutine(name: String, description: String, days: List<WorkoutDay>, id: String? = null) {
        viewModelScope.launch {
            val routineId = id ?: UUID.randomUUID().toString()
            val routineEntity = RoutineEntity(id = routineId, name = name, description = description)
            
            val daysWithExercises = days.mapIndexed { dayIndex, day ->
                val dayId = if (day.id.isBlank() || day.id.startsWith("temp_")) UUID.randomUUID().toString() else day.id
                val dayEntity = WorkoutDayEntity(
                    id = dayId,
                    routineId = routineId,
                    name = day.name,
                    order = dayIndex
                )
                val exerciseEntities = day.exercises.mapIndexed { exIndex, ex ->
                    WorkoutDayExerciseEntity(
                        workoutDayId = dayId,
                        exerciseId = ex.id,
                        routineSets = ex.routineSets,
                        order = exIndex
                    )
                }
                dayEntity to exerciseEntities
            }
            
            workoutDao.upsertRoutine(routineEntity, daysWithExercises)
        }
    }

    fun deleteRoutine(routineId: String) {
        viewModelScope.launch {
            workoutDao.deleteRoutine(routineId)
        }
    }

    fun selectRoutine(routineId: String) {
        viewModelScope.launch {
            workoutDao.selectRoutine(routineId)
        }
    }

    fun completeWorkoutDay(routineId: String, dayIndex: Int) {
        viewModelScope.launch {
            workoutDao.updateLastCompletedDayIndex(routineId, dayIndex)
        }
    }

    fun logExerciseHistory(workoutHistoryId: String, exerciseId: String, sets: Int, reps: Int, weight: Double) {
        viewModelScope.launch {
            workoutDao.insertExerciseHistory(
                ExerciseHistoryEntity(
                    workoutHistoryId = workoutHistoryId,
                    exerciseId = exerciseId,
                    date = System.currentTimeMillis(),
                    sets = sets,
                    reps = reps,
                    weight = weight
                )
            )
        }
    }

    fun getExerciseHistory(exerciseId: String): Flow<List<ExerciseHistoryEntity>> {
        return workoutDao.getHistoryForExercise(exerciseId)
    }
}

class ActiveWorkout(
    val workoutDay: WorkoutDay,
    val dayIndex: Int,
    val exerciseStates: androidx.compose.runtime.snapshots.SnapshotStateList<ExerciseState>,
    val startTime: Long
) {
    var elapsedTime by mutableLongStateOf(0L)
}

// Extension functions to convert between Entity and Domain model
fun EquipmentEntity.toDomain() = Equipment(id, name)

fun ExerciseWithEquipment.toDomain() = Exercise(
    id = exercise.id,
    name = exercise.name,
    description = exercise.description,
    muscleGroup = exercise.muscleGroup,
    equipmentId = exercise.equipmentId,
    equipmentName = equipment?.name
)

fun RoutineWithDays.toDomain() = Routine(
    id = routine.id,
    name = routine.name,
    description = routine.description,
    workoutDays = days.sortedBy { it.day.order }.map { it.toDomain() },
    isSelected = routine.isSelected,
    lastCompletedDayIndex = routine.lastCompletedDayIndex
)

fun WorkoutDayWithExercises.toDomain() = WorkoutDay(
    id = day.id,
    name = day.name,
    exercises = exercises.sortedBy { it.dayExercise.order }.map { it.toDomain() }
)

fun WorkoutDayExerciseWithDetails.toDomain() = Exercise(
    id = exercise.exercise.id,
    name = exercise.exercise.name,
    description = exercise.exercise.description,
    muscleGroup = exercise.exercise.muscleGroup,
    equipmentId = exercise.exercise.equipmentId,
    equipmentName = exercise.equipment?.name,
    routineSets = dayExercise.routineSets
)

fun RoutineEntity.toDomain() = Routine(id, name, description, isSelected = isSelected, lastCompletedDayIndex = lastCompletedDayIndex)
