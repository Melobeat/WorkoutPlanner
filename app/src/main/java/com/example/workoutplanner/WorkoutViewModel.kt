package com.example.workoutplanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.workoutplanner.data.*
import com.example.workoutplanner.model.Equipment
import com.example.workoutplanner.model.Exercise
import com.example.workoutplanner.model.Routine
import com.example.workoutplanner.model.WorkoutDay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class WorkoutViewModel(private val workoutDao: WorkoutDao) : ViewModel() {

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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                        sets = ex.sets,
                        reps = ex.reps,
                        weight = ex.weight,
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

    fun logExerciseHistory(exerciseId: String, sets: Int, reps: Int, weight: Double) {
        viewModelScope.launch {
            workoutDao.insertExerciseHistory(
                ExerciseHistoryEntity(
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
    sets = dayExercise.sets,
    reps = dayExercise.reps,
    weight = dayExercise.weight
)

fun RoutineEntity.toDomain() = Routine(id, name, description, isSelected = isSelected, lastCompletedDayIndex = lastCompletedDayIndex)

class WorkoutViewModelFactory(private val workoutDao: WorkoutDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutViewModel(workoutDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
