package de.melobeat.workoutplanner.domain.model

data class ExerciseHistory(
    val exerciseId: String,
    val reps: Int,
    val weight: Double,
    val setIndex: Int = 1,
    val isAmrap: Boolean = false,
    val sideType: String = "Bilateral",
    val leftReps: Int? = null,
    val rightReps: Int? = null
)