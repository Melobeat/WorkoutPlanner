package de.melobeat.workoutplanner.model

data class Routine(
    val id: String,
    val name: String,
    val description: String = "",
    val workoutDays: List<WorkoutDay> = emptyList(),
    val isSelected: Boolean = false,
    val lastCompletedDayIndex: Int = -1
)
