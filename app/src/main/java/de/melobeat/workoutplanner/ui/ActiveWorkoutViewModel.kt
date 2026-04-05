package de.melobeat.workoutplanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.melobeat.workoutplanner.data.RestTimerPreferencesRepository
import de.melobeat.workoutplanner.data.RestTimerSettings
import de.melobeat.workoutplanner.data.WorkoutRepository
import de.melobeat.workoutplanner.model.Exercise
import de.melobeat.workoutplanner.model.RoutineSet
import de.melobeat.workoutplanner.model.WorkoutDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val timerPrefs: RestTimerPreferencesRepository
) : ViewModel() {

    companion object {
        private const val WEIGHT_STEP = 2.5
    }

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    private var timerJob: Job? = null
    private var restTimerJob: Job? = null
    private var currentWorkoutDay: WorkoutDay? = null
    private var currentDayIndex: Int = 0
    private var currentRoutineName: String = ""
    private var currentRoutineId: String? = null

    private val _restTimerEvents = MutableSharedFlow<RestTimerEvent>()
    val restTimerEvents: SharedFlow<RestTimerEvent> = _restTimerEvents.asSharedFlow()

    private val timerSettings = timerPrefs.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = RestTimerSettings()
    )

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
                        error = null,
                        currentExerciseIndex = 0,
                        currentSetIndex = 0
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

    private fun startRestTimer(context: RestTimerContext) {
        restTimerJob?.cancel()
        val settings = timerSettings.value
        val restState = RestTimerUiState(
            context = context,
            easyThresholdSeconds = settings.betweenSetsEasySeconds,
            hardThresholdSeconds = settings.betweenSetsHardSeconds,
            singleThresholdSeconds = settings.betweenExercisesSeconds
        )
        _uiState.update { it.copy(restTimer = restState) }
        restTimerJob = viewModelScope.launch(Dispatchers.Default) {
            var seconds = 0
            var easyFired = false
            var hardFired = false
            var singleFired = false
            while (isActive) {
                delay(1000)
                seconds++
                _uiState.update { s -> s.copy(restTimer = s.restTimer?.copy(elapsedSeconds = seconds)) }
                when (context) {
                    RestTimerContext.BetweenSets -> {
                        if (!easyFired && seconds >= restState.easyThresholdSeconds) {
                            easyFired = true
                            _restTimerEvents.emit(RestTimerEvent.EasyMilestone)
                        }
                        if (!hardFired && seconds >= restState.hardThresholdSeconds) {
                            hardFired = true
                            _restTimerEvents.emit(RestTimerEvent.HardMilestone)
                        }
                    }
                    RestTimerContext.BetweenExercises -> {
                        if (!singleFired && seconds >= restState.singleThresholdSeconds) {
                            singleFired = true
                            _restTimerEvents.emit(RestTimerEvent.ExerciseMilestone)
                        }
                    }
                }
            }
        }
    }

    private fun cancelRestTimer() {
        restTimerJob?.cancel()
        restTimerJob = null
        _uiState.update { it.copy(restTimer = null) }
    }

    private fun startTimer() {
        timerJob?.cancel()
        _elapsedTime.value = 0L
        val start = System.currentTimeMillis()
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
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
        cancelRestTimer()
        _elapsedTime.value = 0L
        currentWorkoutDay = null
        _uiState.value = ActiveWorkoutUiState()
    }

    fun requestFinish() {
        val capturedDuration = _elapsedTime.value
        timerJob?.cancel()
        timerJob = null
        cancelRestTimer()
        _uiState.update { it.copy(showSummary = true, summaryDurationMs = capturedDuration) }
    }

    fun resumeWorkout() {
        _uiState.update { it.copy(showSummary = false) }
        val resumeFrom = _uiState.value.summaryDurationMs
        val start = System.currentTimeMillis() - resumeFrom
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - start
                _elapsedTime.value = elapsed
                _uiState.update { it.copy(elapsedTime = elapsed) }
                delay(1000)
            }
        }
    }

    fun finishWorkout() {
        val day = currentWorkoutDay ?: return
        currentWorkoutDay = null
        val duration = _uiState.value.summaryDurationMs
        timerJob?.cancel()
        timerJob = null

        var hasInvalidSets = false
        val history = _uiState.value.exercises.flatMap { exercise ->
            exercise.sets
                .filter { it.reps.isNotEmpty() && it.weight.isNotEmpty() }
                .mapIndexedNotNull { setIndex, set ->
                    val reps = set.reps.toIntOrNull()
                    val weight = set.weight.toDoubleOrNull()
                    if (reps == null || weight == null) {
                        hasInvalidSets = true
                        android.util.Log.w("ActiveWorkoutViewModel", "Skipping set with invalid reps='${set.reps}' weight='${set.weight}'")
                        null
                    } else {
                        ExerciseHistory(
                            exerciseId = exercise.exerciseId,
                            reps = reps,
                            weight = weight,
                            setIndex = setIndex + 1,
                            isAmrap = set.isAmrap
                        )
                    }
                }
        }
        if (hasInvalidSets) {
            _uiState.update { it.copy(error = "Some sets had invalid values and were skipped.") }
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

    fun incrementReps(exerciseIndex: Int, setIndex: Int) {
        val current = _uiState.value.exercises
            .getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex)
            ?.reps?.toIntOrNull() ?: 0
        setRepsValue(exerciseIndex, setIndex, (current + 1).toString())
    }

    fun decrementReps(exerciseIndex: Int, setIndex: Int) {
        val current = _uiState.value.exercises
            .getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex)
            ?.reps?.toIntOrNull() ?: 0
        if (current > 0) setRepsValue(exerciseIndex, setIndex, (current - 1).toString())
    }

    fun incrementWeight(exerciseIndex: Int, setIndex: Int) {
        val current = _uiState.value.exercises
            .getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex)
            ?.weight?.toDoubleOrNull() ?: 0.0
        setWeightValue(exerciseIndex, setIndex, formatWeight(current + WEIGHT_STEP))
    }

    fun decrementWeight(exerciseIndex: Int, setIndex: Int) {
        val current = _uiState.value.exercises
            .getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex)
            ?.weight?.toDoubleOrNull() ?: 0.0
        if (current >= WEIGHT_STEP) setWeightValue(exerciseIndex, setIndex, formatWeight(current - WEIGHT_STEP))
    }

    fun completeCurrentSet() {
        val state = _uiState.value
        val ei = state.currentExerciseIndex
        val si = state.currentSetIndex
        cancelRestTimer()
        _uiState.update { s ->
            s.copy(exercises = s.exercises.mapIndexed { eIdx, ex ->
                if (eIdx != ei) ex
                else ex.copy(sets = ex.sets.mapIndexed { sIdx, set ->
                    if (sIdx == si) set.copy(isDone = true) else set
                })
            })
        }
        val exercise = _uiState.value.exercises.getOrNull(ei) ?: return
        when {
            si < exercise.sets.size - 1 -> {
                _uiState.update { it.copy(currentSetIndex = si + 1) }
                startRestTimer(RestTimerContext.BetweenSets)
            }
            ei < _uiState.value.exercises.size - 1 -> {
                _uiState.update { it.copy(currentExerciseIndex = ei + 1, currentSetIndex = 0) }
                startRestTimer(RestTimerContext.BetweenExercises)
            }
            else -> requestFinish()
        }
    }

    fun goToPreviousSet() {
        val state = _uiState.value
        val ei = state.currentExerciseIndex
        val si = state.currentSetIndex
        when {
            si > 0 -> _uiState.update { it.copy(currentSetIndex = si - 1) }
            ei > 0 -> {
                val prevExercise = state.exercises[ei - 1]
                _uiState.update {
                    it.copy(
                        currentExerciseIndex = ei - 1,
                        currentSetIndex = prevExercise.sets.size - 1
                    )
                }
            }
            // else: already at start — no-op
        }
    }

    fun skipExercise() {
        val state = _uiState.value
        val ei = state.currentExerciseIndex
        cancelRestTimer()
        if (ei < state.exercises.size - 1) {
            _uiState.update { it.copy(currentExerciseIndex = ei + 1, currentSetIndex = 0) }
            startRestTimer(RestTimerContext.BetweenExercises)
        } else {
            requestFinish()
        }
    }

    // Value-only updates (do not flip isDone) used by steppers
    private fun setRepsValue(exerciseIndex: Int, setIndex: Int, reps: String) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
                if (ei != exerciseIndex) return@mapIndexed ex
                ex.copy(sets = ex.sets.mapIndexed { si, set ->
                    if (si == setIndex) set.copy(reps = reps, originalReps = reps) else set
                })
            })
        }
    }

    private fun setWeightValue(exerciseIndex: Int, setIndex: Int, weight: String) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
                if (ei != exerciseIndex) return@mapIndexed ex
                ex.copy(sets = ex.sets.mapIndexed { si, set ->
                    if (si == setIndex) set.copy(weight = weight) else set
                })
            })
        }
    }
}
