package de.melobeat.workoutplanner.model

data class Equipment(
    val id: String,
    val name: String,
    val defaultWeight: Double? = null,
    val weightStep: Double = 2.5
)
