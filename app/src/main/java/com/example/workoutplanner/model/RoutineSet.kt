package com.example.workoutplanner.model

import kotlinx.serialization.Serializable

@Serializable
data class RoutineSet(
    val reps: Int,
    val weight: Double,
    val isAmrap: Boolean = false
)
