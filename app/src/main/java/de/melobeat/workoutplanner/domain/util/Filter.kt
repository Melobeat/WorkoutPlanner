package de.melobeat.workoutplanner.domain.util

import de.melobeat.workoutplanner.domain.model.Exercise


internal fun filterExercises(exercises: List<Exercise>, query: String): List<Exercise> {
    if (query.isBlank()) return exercises
    val lower = query.lowercase()
    return exercises.filter {
        it.name.lowercase().contains(lower) || it.muscleGroup.lowercase().contains(lower)
    }
}
