package com.example.workoutplanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutplanner.data.WorkoutRepository
import com.example.workoutplanner.model.Exercise
import com.example.workoutplanner.model.RoutineSet
import com.example.workoutplanner.model.WorkoutDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Immutable state classes
data class ActiveWorkoutUiState(
    val isActive: Boolean = false,
    val isFullScreen: Boolean = false,
    val workoutDayName: String = "",
    val exercises: List<ExerciseUiState> = emptyList(),
    val elapsedTime: Long = 0L,
    val isFinished: Boolean = false,
    val error: String? = null
)

data class ExerciseUiState(
    val exerciseId: String,
    val name: String,
    val sets: List<SetUiState>,
    val isExpanded: Boolean = true,
    val lastSets: List<Pair<Double, Int>> = emptyList()
)

data class SetUiState(
    val index: Int,
    val weight: String,
    val reps: String,
    val isAmrap: Boolean,
    val isDone: Boolean = false,
    val originalReps: String
)

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    private var timerJob: Job? = null
    private var currentWorkoutDay: WorkoutDay? = null
    private var currentDayIndex: Int = 0
    private var currentRoutineName: String = ""
    private var currentRoutineId: String? = null

    fun startWorkout(day: WorkoutDay, dayIndex: Int, routineName: String, routineId: String?) {
        viewModelScope.launch {
            try {
                val exerciseStates = day.exercises.map { exercise ->
                    val history = repository.getHistoryForExercise(exercise.id).first()
                    val lastSets = if (history.isNotEmpty()) {
                        val latestWorkoutId = history[0].workoutHistoryId
                        history.filter { it.workoutHistoryId == latestWorkoutId }
                            .sortedBy { it.sets }
                            .map { it.weight to it.reps }
                    } else emptyList()

                    buildExerciseUiState(exercise, lastSets)
                }

                currentWorkoutDay = day
                currentDayIndex = dayIndex
                currentRoutineName = routineName
                currentRoutineId = routineId

                _uiState.update {
                    it.copy(
                        isActive = true,
                        isFullScreen = true,
                        workoutDayName = day.name,
                        exercises = exerciseStates,
                        isFinished = false,
                        error = null
                    )
                }
                startTimer()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to start workout: ${e.message}") }
            }
        }
    }

    private fun buildExerciseUiState(
        exercise: Exercise,
        lastSets: List<Pair<Double, Int>>
    ): ExerciseUiState {
        val predefined = exercise.routineSets
        val numSets = if (predefined.isNotEmpty()) predefined.size else maxOf(3, lastSets.size)
        val sets = (0 until numSets).map { i ->
            val weight = lastSets.getOrNull(i)?.first?.let { formatWeight(it) }
                ?: predefined.getOrNull(i)?.weight?.let { if (it > 0) formatWeight(it) else "0" }
                ?: "0"
            val reps = predefined.getOrNull(i)?.reps?.toString() ?: "0"
            val isAmrap = predefined.getOrNull(i)?.isAmrap ?: false
            SetUiState(index = i, weight = weight, reps = reps, isAmrap = isAmrap, originalReps = reps)
        }
        return ExerciseUiState(
            exerciseId = exercise.id,
            name = exercise.name,
            sets = sets,
            lastSets = lastSets
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        _elapsedTime.value = 0L
        val start = System.currentTimeMillis()
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val elapsed = System.currentTimeMillis() - start
                _elapsedTime.value = elapsed
                _uiState.update { it.copy(elapsedTime = elapsed) }
                delay(1000)
            }
        }
    }

    fun cancelWorkout() {
        timerJob?.cancel()
        timerJob = null
        _elapsedTime.value = 0L
        currentWorkoutDay = null
        _uiState.value = ActiveWorkoutUiState()
    }

    fun finishWorkout() {
        val day = currentWorkoutDay ?: return
        currentWorkoutDay = null
        val duration = _elapsedTime.value
        timerJob?.cancel()
        timerJob = null

        val history = _uiState.value.exercises.flatMap { exercise ->
            exercise.sets
                .filter { it.reps.isNotEmpty() && it.weight.isNotEmpty() }
                .mapIndexed { setIndex, set ->
                    ExerciseHistory(
                        exerciseId = exercise.exerciseId,
                        reps = set.reps.toIntOrNull() ?: 0,
                        weight = set.weight.toDoubleOrNull() ?: 0.0,
                        setIndex = setIndex + 1,
                        isAmrap = set.isAmrap
                    )
                }
        }

        viewModelScope.launch {
            try {
                repository.finishWorkout(
                    history = history,
                    workoutDay = day,
                    dayIndex = currentDayIndex,
                    durationMs = duration,
                    routineName = currentRoutineName,
                    routineId = currentRoutineId
                )
                _uiState.value = ActiveWorkoutUiState(isFinished = true)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to save workout: ${e.message}") }
            }
        }
    }

    fun setFullScreen(fullScreen: Boolean) {
        _uiState.update { it.copy(isFullScreen = fullScreen) }
    }

    fun toggleExerciseExpanded(exerciseIndex: Int) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { i, ex ->
                if (i == exerciseIndex) ex.copy(isExpanded = !ex.isExpanded) else ex
            })
        }
    }

    fun toggleSetDone(exerciseIndex: Int, setIndex: Int) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
                if (ei != exerciseIndex) return@mapIndexed ex
                ex.copy(sets = ex.sets.mapIndexed { si, set ->
                    if (si != setIndex) return@mapIndexed set
                    if (set.isAmrap) {
                        set // AMRAP handled by RepsDialog
                    } else {
                        if (!set.isDone) {
                            set.copy(isDone = true)
                        } else {
                            val currentReps = set.reps.toIntOrNull() ?: 0
                            if (currentReps > 0) {
                                set.copy(reps = (currentReps - 1).toString())
                            } else {
                                set.copy(reps = set.originalReps, isDone = false)
                            }
                        }
                    }
                })
            })
        }
    }

    fun updateSetReps(exerciseIndex: Int, setIndex: Int, reps: String) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
                if (ei != exerciseIndex) return@mapIndexed ex
                ex.copy(sets = ex.sets.mapIndexed { si, set ->
                    if (si == setIndex) set.copy(reps = reps, originalReps = reps, isDone = true)
                    else set
                })
            })
        }
    }

    fun updateSetWeight(exerciseIndex: Int, setIndex: Int, weight: String) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
                if (ei != exerciseIndex) return@mapIndexed ex
                ex.copy(sets = ex.sets.mapIndexed { si, set ->
                    if (si == setIndex) set.copy(weight = weight) else set
                })
            })
        }
    }

    fun addSet(exerciseIndex: Int) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
                if (ei != exerciseIndex) return@mapIndexed ex
                val newIndex = ex.sets.size
                ex.copy(sets = ex.sets + SetUiState(
                    index = newIndex, weight = "0", reps = "0",
                    isAmrap = false, originalReps = "0"
                ))
            })
        }
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
                if (ei != exerciseIndex) return@mapIndexed ex
                if (ex.sets.size <= 1) return@mapIndexed ex
                ex.copy(sets = ex.sets.filterIndexed { si, _ -> si != setIndex }
                    .mapIndexed { i, s -> s.copy(index = i) })
            })
        }
    }

    fun addExercise(exercise: Exercise) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises + buildExerciseUiState(exercise, emptyList()))
        }
    }

    fun swapExercise(exerciseIndex: Int, newExercise: Exercise) {
        val currentSets = _uiState.value.exercises.getOrNull(exerciseIndex)?.sets?.size ?: 3
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
                if (ei != exerciseIndex) return@mapIndexed ex
                buildExerciseUiState(
                    newExercise.copy(
                        routineSets = if (newExercise.routineSets.isNotEmpty())
                            newExercise.routineSets
                        else List(currentSets) { RoutineSet(reps = 0, weight = 0.0) }
                    ),
                    emptyList()
                )
            })
        }
    }

    fun removeExercise(exerciseIndex: Int) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.filterIndexed { i, _ -> i != exerciseIndex })
        }
    }

    fun reorderExercise(from: Int, to: Int) {
        _uiState.update { state ->
            val list = state.exercises.toMutableList()
            val item = list.removeAt(from)
            list.add(to, item)
            state.copy(exercises = list)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
