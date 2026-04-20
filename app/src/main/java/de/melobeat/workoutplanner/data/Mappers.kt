package de.melobeat.workoutplanner.data

import de.melobeat.workoutplanner.model.Equipment
import de.melobeat.workoutplanner.model.Exercise
import de.melobeat.workoutplanner.model.Routine
import de.melobeat.workoutplanner.model.SideType
import de.melobeat.workoutplanner.model.WorkoutDay

fun EquipmentEntity.toDomain() = Equipment(id = id, name = name, defaultWeight = defaultWeight, weightStep = weightStep)

fun ExerciseWithEquipment.toDomain() = Exercise(
    id = exercise.id,
    name = exercise.name,
    description = exercise.description,
    muscleGroup = exercise.muscleGroup,
    equipmentId = exercise.equipmentId,
    equipmentName = equipment?.name,
    isBodyweight = exercise.isBodyweight,
    sideType = SideType.valueOf(exercise.sideType)
)

fun RoutineWithDays.toDomain() = Routine(
    id = routine.id,
    name = routine.name,
    description = routine.description,
    workoutDays = days.sortedBy { it.day.order }.map { it.toDomain() },
    isSelected = routine.isSelected,
    lastCompletedDayIndex = routine.lastCompletedDayIndex
)

fun WorkoutDayWithExercises.toDomain() = WorkoutDay(
    id = day.id,
    name = day.name,
    exercises = exercises.sortedBy { it.dayExercise.order }.map { it.toDomain() }
)

fun WorkoutDayExerciseWithDetails.toDomain() = Exercise(
    id = exercise.exercise.id,
    name = exercise.exercise.name,
    description = exercise.exercise.description,
    muscleGroup = exercise.exercise.muscleGroup,
    equipmentId = exercise.exercise.equipmentId,
    equipmentName = exercise.exercise?.name,
    isBodyweight = exercise.exercise.isBodyweight,
    sideType = dayExercise.sideType?.let { SideType.valueOf(it) }
        ?: SideType.valueOf(exercise.exercise.sideType),
    routineSets = dayExercise.routineSets
)
