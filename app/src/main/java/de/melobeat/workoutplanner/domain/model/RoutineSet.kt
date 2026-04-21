package de.melobeat.workoutplanner.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RoutineSet(
    val reps: Int,
    val weight: Double,
    val isAmrap: Boolean = false,
    val sideType: String = "Bilateral",
    val leftReps: Int? = null,
    val rightReps: Int? = null
)
