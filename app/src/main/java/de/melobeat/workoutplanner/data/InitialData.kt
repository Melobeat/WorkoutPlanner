package de.melobeat.workoutplanner.data

import kotlinx.serialization.Serializable

@Serializable
data class InitialEquipment(val id: String, val name: String, val defaultWeight: Double? = null, val weightStep: Double? = null)

@Serializable
data class InitialExercise(
    val name: String,
    val muscleGroup: String,
    val description: String,
    val equipmentId: String?
)
