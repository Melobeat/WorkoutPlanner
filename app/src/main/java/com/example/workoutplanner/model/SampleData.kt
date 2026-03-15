package com.example.workoutplanner.model

val sampleExercises = listOf(
    Exercise(id = "1", name = "Push Up", muscleGroup = "Chest", description = "Standard push up"),
    Exercise(id = "2", name = "Squat", muscleGroup = "Legs", description = "Bodyweight squat"),
    Exercise(id = "3", name = "Pull Up", muscleGroup = "Back", description = "Overhand grip pull up"),
    Exercise(id = "4", name = "Plank", muscleGroup = "Core", description = "Hold for time"),
)

val sampleWorkoutDays = listOf(
    WorkoutDay(
        id = "d1",
        name = "Upper Body",
        exercises = listOf(
            sampleExercises[0].copy(routineSets = List(3) { RoutineSet(reps = 15, weight = 0.0) }),
            sampleExercises[2].copy(routineSets = List(3) { RoutineSet(reps = 8, weight = 0.0) })
        )
    ),
    WorkoutDay(
        id = "d2",
        name = "Lower Body",
        exercises = listOf(
            sampleExercises[1].copy(routineSets = List(3) { RoutineSet(reps = 20, weight = 0.0) }),
            sampleExercises[3].copy(routineSets = List(3) { RoutineSet(reps = 1, weight = 0.0) }) // 1 minute maybe
        )
    )
)

val sampleRoutines = listOf(
    Routine(
        id = "r1",
        name = "Full Body Split",
        description = "A simple 2-day split for beginners",
        workoutDays = sampleWorkoutDays
    )
)
