package de.melobeat.workoutplanner.ui.feature.workout

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.melobeat.workoutplanner.data.RestTimerPreferencesRepository
import de.melobeat.workoutplanner.data.RestTimerSettings
import de.melobeat.workoutplanner.data.WorkoutRepository
import de.melobeat.workoutplanner.model.Equipment
import de.melobeat.workoutplanner.model.Exercise
import de.melobeat.workoutplanner.model.RoutineSet
import de.melobeat.workoutplanner.model.WorkoutDay
import dagger.hilt.android.lifecycle.HiltViewModel
import de.melobeat.workoutplanner.model.SideType
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

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    private var timerJob: Job? = null
    private var restTimerJob: Job? = null
    private var currentWorkoutDay: WorkoutDay? = null
    private var currentDayIndex: Int = 0
    private var currentRoutineName: String = ""
    private var currentRoutineId: String? = null
    private var equipmentCache: List<Equipment> = emptyList()

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
                equipmentCache = repository.getEquipmentStream().first()
                
                val exerciseStates = day.exercises.mapIndexed { index, exercise ->
                    val history = repository.getHistoryForExercise(exercise.id).first()
                    val lastSets = if (history.isNotEmpty()) {
                        val latestWorkoutId = history[0].workoutHistoryId
                        history.filter { it.workoutHistoryId == latestWorkoutId }
                            .sortedBy { it.sets }
                            .map { it.weight to it.reps }
                    } else emptyList()

                    buildExerciseUiState(exercise, lastSets).copy(isExpanded = index == 0)
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

    private fun resolveWeightStep(exercise: Exercise): Double {
        if (exercise.isBodyweight) return 1.0
        val equipment = equipmentCache.find { it.id == exercise.equipmentId }
        return equipment?.weightStep ?: 1.0
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
            val routineSetSideType = predefined.getOrNull(i)?.sideType?.let { setSideType ->
                if (setSideType == "Bilateral" && exercise.sideType.name == "Unilateral") exercise.sideType.name else setSideType
            } ?: exercise.sideType.name
            val isUnilateral = routineSetSideType == "Unilateral"
            SetUiState(
                index = i,
                weight = weight,
                reps = reps,
                isAmrap = isAmrap,
                originalReps = reps,
                sideType = routineSetSideType,
                leftReps = if (isUnilateral) predefined.getOrNull(i)?.leftReps ?: 0 else null,
                rightReps = if (isUnilateral) predefined.getOrNull(i)?.rightReps ?: 0 else null,
                leftOriginalReps = if (isUnilateral) predefined.getOrNull(i)?.leftReps else null,
                rightOriginalReps = if (isUnilateral) predefined.getOrNull(i)?.rightReps else null,
            )
        }
        return ExerciseUiState(
            exerciseId = exercise.id,
            name = exercise.name,
            sets = sets,
            lastSets = lastSets,
            weightStep = resolveWeightStep(exercise),
            sideType = exercise.sideType.name
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
                .filter { set ->
                    if (set.sideType == "Unilateral") {
                        (set.leftReps ?: 0) > 0 || (set.rightReps ?: 0) > 0
                    } else {
                        set.reps.isNotEmpty() && set.weight.isNotEmpty()
                    }
                }
                .mapIndexedNotNull { setIndex, set ->
                    val weight = set.weight.toDoubleOrNull()
                    if (weight == null) {
                        hasInvalidSets = true
                        Log.w("ActiveWorkoutViewModel", "Skipping set with invalid weight='${set.weight}'")
                        null
                    } else {
                        if (set.sideType == "Unilateral") {
                            val leftReps = set.leftReps ?: 0
                            val rightReps = set.rightReps ?: 0
                            ExerciseHistory(
                                exerciseId = exercise.exerciseId,
                                reps = leftReps + rightReps,
                                weight = weight,
                                setIndex = setIndex + 1,
                                isAmrap = set.isAmrap,
                                sideType = "Unilateral",
                                leftReps = leftReps,
                                rightReps = rightReps
                            )
                        } else {
                            val reps = set.reps.toIntOrNull()
                            if (reps == null) {
                                hasInvalidSets = true
                                Log.w("ActiveWorkoutViewModel", "Skipping set with invalid reps='${set.reps}'")
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

    fun jumpToSet(exerciseIndex: Int, setIndex: Int) {
        _uiState.update { state ->
            state.copy(
                currentExerciseIndex = exerciseIndex,
                currentSetIndex = setIndex,
                exercises = state.exercises.mapIndexed { i, ex ->
                    if (i == exerciseIndex) ex.copy(isExpanded = true) else ex
                }
            )
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
                        if (set.sideType == "Unilateral") {
                            val left = set.leftReps ?: 0
                            val right = set.rightReps ?: 0
                            if (!set.isDone) {
                                if (left > 0 && right > 0) set.copy(isDone = true) else set
                            } else {
                                val newLeft = maxOf(0, left - 1)
                                val newRight = maxOf(0, right - 1)
                                if (newLeft == 0 && newRight == 0) {
                                    set.copy(
                                        leftReps = set.leftOriginalReps,
                                        rightReps = set.rightOriginalReps,
                                        isDone = false
                                    )
                                } else {
                                    set.copy(leftReps = newLeft, rightReps = newRight)
                                }
                            }
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
                val isUnilateral = ex.sideType == "Unilateral"
                ex.copy(sets = ex.sets + SetUiState(
                    index = newIndex, weight = "0", reps = "0",
                    isAmrap = false, originalReps = "0",
                    sideType = ex.sideType,
                    leftReps = if (isUnilateral) 0 else null,
                    rightReps = if (isUnilateral) 0 else null,
                    leftOriginalReps = if (isUnilateral) 0 else null,
                    rightOriginalReps = if (isUnilateral) 0 else null,
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
        viewModelScope.launch {
            if (equipmentCache.isEmpty()) {
                equipmentCache = repository.getEquipmentStream().first()
            }
            _uiState.update { state ->
                state.copy(exercises = state.exercises + buildExerciseUiState(exercise, emptyList()))
            }
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
                        else {
                            val isUnilateral = newExercise.sideType == SideType.Unilateral
                            List(currentSets) {
                                RoutineSet(
                                    reps = 0, weight = 0.0,
                                    sideType = newExercise.sideType.name,
                                    leftReps = if (isUnilateral) 0 else null,
                                    rightReps = if (isUnilateral) 0 else null
                                )
                            }
                        }
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

    fun setLeftReps(exerciseIndex: Int, setIndex: Int, reps: Int) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
                if (ei != exerciseIndex) return@mapIndexed ex
                ex.copy(sets = ex.sets.mapIndexed { si, set ->
                    if (si == setIndex) set.copy(leftReps = reps) else set
                })
            })
        }
    }

    fun setRightReps(exerciseIndex: Int, setIndex: Int, reps: Int) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
                if (ei != exerciseIndex) return@mapIndexed ex
                ex.copy(sets = ex.sets.mapIndexed { si, set ->
                    if (si == setIndex) set.copy(rightReps = reps) else set
                })
            })
        }
    }

    fun incrementLeftReps(exerciseIndex: Int, setIndex: Int) {
        val current = _uiState.value.exercises.getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex)?.leftReps ?: 0
        setLeftReps(exerciseIndex, setIndex, current + 1)
    }

    fun decrementLeftReps(exerciseIndex: Int, setIndex: Int) {
        val current = _uiState.value.exercises.getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex)?.leftReps ?: 0
        if (current > 0) setLeftReps(exerciseIndex, setIndex, current - 1)
    }

    fun incrementRightReps(exerciseIndex: Int, setIndex: Int) {
        val current = _uiState.value.exercises.getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex)?.rightReps ?: 0
        setRightReps(exerciseIndex, setIndex, current + 1)
    }

    fun decrementRightReps(exerciseIndex: Int, setIndex: Int) {
        val current = _uiState.value.exercises.getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex)?.rightReps ?: 0
        if (current > 0) setRightReps(exerciseIndex, setIndex, current - 1)
    }

    fun incrementWeight(exerciseIndex: Int, setIndex: Int) {
        val exercise = _uiState.value.exercises.getOrNull(exerciseIndex) ?: return
        val set = exercise.sets.getOrNull(setIndex) ?: return
        val current = set.weight.toDoubleOrNull() ?: 0.0
        setWeightValue(exerciseIndex, setIndex, formatWeight(current + exercise.weightStep))
    }

    fun decrementWeight(exerciseIndex: Int, setIndex: Int) {
        val exercise = _uiState.value.exercises.getOrNull(exerciseIndex) ?: return
        val set = exercise.sets.getOrNull(setIndex) ?: return
        val current = set.weight.toDoubleOrNull() ?: 0.0
        if (current >= exercise.weightStep) setWeightValue(exerciseIndex, setIndex, formatWeight(current - exercise.weightStep))
    }

    fun completeCurrentSet() {
        val state = _uiState.value
        val ei = state.currentExerciseIndex
        val si = state.currentSetIndex
        cancelRestTimer()
        val exercise = state.exercises.getOrNull(ei) ?: return
        val set = exercise.sets.getOrNull(si) ?: return

        _uiState.update { s ->
            s.copy(exercises = s.exercises.mapIndexed { eIdx, ex ->
                if (eIdx != ei) ex
                else ex.copy(sets = ex.sets.mapIndexed { sIdx, s ->
                    if (sIdx != si) s
                    else {
                        if (s.sideType == "Unilateral") {
                            val left = s.leftReps ?: 0
                            val right = s.rightReps ?: 0
                            if (left > 0 && right > 0) s.copy(isDone = true) else s
                        } else {
                            s.copy(isDone = true)
                        }
                    }
                })
            })
        }
        val updatedExercise = _uiState.value.exercises.getOrNull(ei) ?: return
        val updatedSet = updatedExercise.sets.getOrNull(si) ?: return
        if (!updatedSet.isDone) return
        when {
            si < updatedExercise.sets.size - 1 -> {
                _uiState.update { it.copy(currentSetIndex = si + 1) }
                startRestTimer(RestTimerContext.BetweenSets)
            }
            ei < _uiState.value.exercises.size - 1 -> {
                _uiState.update { state ->
                    state.copy(
                        currentExerciseIndex = ei + 1,
                        currentSetIndex = 0,
                        exercises = state.exercises.mapIndexed { i, ex ->
                            when (i) {
                                ei -> ex.copy(isExpanded = false)
                                ei + 1 -> ex.copy(isExpanded = true)
                                else -> ex
                            }
                        }
                    )
                }
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
                _uiState.update { s ->
                    s.copy(
                        currentExerciseIndex = ei - 1,
                        currentSetIndex = prevExercise.sets.size - 1,
                        exercises = s.exercises.mapIndexed { idx, ex ->
                            if (idx == ei - 1) ex.copy(isExpanded = true) else ex
                        }
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
