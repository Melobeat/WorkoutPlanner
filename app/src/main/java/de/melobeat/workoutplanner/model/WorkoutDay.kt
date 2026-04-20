package de.melobeat.workoutplanner.model

data class WorkoutDay(
    val id: String,
    val name: String,
    val exercises: List<Exercise> = emptyList()
)
