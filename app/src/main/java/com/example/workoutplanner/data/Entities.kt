package com.example.workoutplanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Embedded
import androidx.room.Relation
import java.util.UUID

@Entity(tableName = "equipment")
data class EquipmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String
)

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = EquipmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipmentId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("equipmentId")]
)
data class ExerciseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val muscleGroup: String,
    val description: String,
    val equipmentId: String? = null
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val isSelected: Boolean = false,
    val lastCompletedDayIndex: Int = -1
)

@Entity(
    tableName = "workout_days",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineId")]
)
data class WorkoutDayEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val routineId: String,
    val name: String,
    val order: Int
)

@Entity(
    tableName = "workout_day_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutDayId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutDayId"), Index("exerciseId")]
)
data class WorkoutDayExerciseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workoutDayId: String,
    val exerciseId: String,
    val sets: Int,
    val reps: Int,
    val weight: Double,
    val order: Int
)

@Entity(
    tableName = "exercise_history",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId")]
)
data class ExerciseHistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val exerciseId: String,
    val date: Long,
    val sets: Int,
    val reps: Int,
    val weight: Double
)

data class WorkoutDayExerciseWithDetails(
    @Embedded val dayExercise: WorkoutDayExerciseEntity,
    @Relation(
        entity = ExerciseEntity::class,
        parentColumn = "exerciseId",
        entityColumn = "id"
    )
    val exercise: ExerciseWithEquipment
)

data class WorkoutDayWithExercises(
    @Embedded val day: WorkoutDayEntity,
    @Relation(
        entity = WorkoutDayExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "workoutDayId"
    )
    val exercises: List<WorkoutDayExerciseWithDetails>
)

data class RoutineWithDays(
    @Embedded val routine: RoutineEntity,
    @Relation(
        entity = WorkoutDayEntity::class,
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val days: List<WorkoutDayWithExercises>
)

data class ExerciseWithEquipment(
    @Embedded val exercise: ExerciseEntity,
    @Relation(
        parentColumn = "equipmentId",
        entityColumn = "id"
    )
    val equipment: EquipmentEntity?
)
