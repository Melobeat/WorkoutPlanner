package com.example.workoutplanner.model

data class Exercise(
    val id: String,
    val name: String,
    val description: String = "",
    val muscleGroup: String = "",
    val equipmentId: String? = null,
    val equipmentName: String? = null,
    val routineSets: List<RoutineSet> = emptyList()
)
