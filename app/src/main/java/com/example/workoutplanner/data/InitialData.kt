package com.example.workoutplanner.data

import kotlinx.serialization.Serializable

@Serializable
data class InitialEquipment(val id: String, val name: String)

@Serializable
data class InitialExercise(
    val name: String,
    val muscleGroup: String,
    val description: String,
    val equipmentId: String?
)
