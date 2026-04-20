package de.melobeat.workoutplanner.data

import kotlinx.serialization.Serializable

@Serializable
data class InitialEquipment(val id: String, val name: String, val defaultWeight: Double? = null, val weightStep: Double? = null)

@Serializable
data class InitialExercise(
    val name: String,
    val muscleGroup: String,
    val description: String,
    val equipmentId: String?,
    val sideType: String = "Bilateral"
)

@Serializable
data class InitialRoutineSet(
    val reps: Int,
    val weight: Double,
    val isAmrap: Boolean = false,
    val sideType: String = "Bilateral",
    val leftReps: Int? = null,
    val rightReps: Int? = null
)

@Serializable
data class InitialRoutineExercise(
    val exerciseName: String,
    val sets: List<InitialRoutineSet>,
    val sideType: String? = null
)

@Serializable
data class InitialWorkoutDay(
    val name: String,
    val exercises: List<InitialRoutineExercise>
)

@Serializable
data class InitialRoutine(
    val name: String,
    val description: String,
    val days: List<InitialWorkoutDay>
)
