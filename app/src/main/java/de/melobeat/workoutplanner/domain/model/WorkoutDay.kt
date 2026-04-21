package de.melobeat.workoutplanner.domain.model

data class WorkoutDay(
    val id: String,
    val name: String,
    val exercises: List<Exercise> = emptyList()
)
