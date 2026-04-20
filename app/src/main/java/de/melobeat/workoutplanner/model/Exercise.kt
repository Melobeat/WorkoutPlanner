package de.melobeat.workoutplanner.model

data class Exercise(
    val id: String,
    val name: String,
    val description: String = "",
    val muscleGroup: String = "",
    val equipmentId: String? = null,
    val equipmentName: String? = null,
    val isBodyweight: Boolean = false,
    val sideType: SideType = SideType.Bilateral,
    val routineSets: List<RoutineSet> = emptyList()
)
