package de.melobeat.workoutplanner.ui

data class ExerciseHistory(
    val exerciseId: String,
    val reps: Int,
    val weight: Double,
    val setIndex: Int = 1,
    val isAmrap: Boolean = false
)

fun formatWeight(weight: Double): String {
    return if (weight % 1.0 == 0.0) weight.toInt().toString() else weight.toString()
}

enum class RestTimerContext { BetweenSets, BetweenExercises }

data class RestTimerUiState(
    val elapsedSeconds: Int = 0,
    val context: RestTimerContext,
    val easyThresholdSeconds: Int,
    val hardThresholdSeconds: Int,
    val singleThresholdSeconds: Int
)

sealed class RestTimerEvent {
    object EasyMilestone : RestTimerEvent()
    object HardMilestone : RestTimerEvent()
    object ExerciseMilestone : RestTimerEvent()
}

data class ActiveWorkoutUiState(
    val isActive: Boolean = false,
    val isFullScreen: Boolean = false,
    val workoutDayName: String = "",
    val exercises: List<ExerciseUiState> = emptyList(),
    val elapsedTime: Long = 0L,
    val isFinished: Boolean = false,
    val showSummary: Boolean = false,
    val summaryDurationMs: Long = 0L,
    val error: String? = null,
    val currentExerciseIndex: Int = 0,
    val currentSetIndex: Int = 0,
    val restTimer: RestTimerUiState? = null
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
