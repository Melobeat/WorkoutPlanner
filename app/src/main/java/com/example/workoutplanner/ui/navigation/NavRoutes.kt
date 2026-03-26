package com.example.workoutplanner.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object HistoryRoute
// SettingsGraphRoute is the nested graph key; SettingsRoute is the first destination inside it
@Serializable object SettingsGraphRoute
@Serializable object SettingsRoute
@Serializable object ExercisesRoute
@Serializable object EquipmentRoute
@Serializable object RoutinesRoute
@Serializable data class RoutineDetailRoute(val routineId: String)
@Serializable data class CreateRoutineRoute(val routineId: String? = null)
@Serializable object ActiveWorkoutRoute
