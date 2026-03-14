package com.example.workoutplanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    // Equipment
    @Query("SELECT * FROM equipment")
    fun getAllEquipment(): Flow<List<EquipmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(equipment: EquipmentEntity)

    @Query("DELETE FROM equipment WHERE id = :equipmentId")
    suspend fun deleteEquipment(equipmentId: String)

    // Exercises
    @Transaction
    @Query("SELECT * FROM exercises")
    fun getAllExercisesWithEquipment(): Flow<List<ExerciseWithEquipment>>

    @Query("SELECT * FROM exercises")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises LIMIT 1")
    suspend fun getAnyExercise(): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :exerciseId")
    suspend fun deleteExercise(exerciseId: String)

    // Routines
    @Query("SELECT * FROM routines")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Transaction
    @Query("SELECT * FROM routines")
    fun getAllRoutinesWithDays(): Flow<List<RoutineWithDays>>

    @Transaction
    @Query("SELECT * FROM routines WHERE isSelected = 1 LIMIT 1")
    fun getSelectedRoutineWithDays(): Flow<RoutineWithDays?>

    @Query("SELECT * FROM routines WHERE id = :routineId")
    suspend fun getRoutineById(routineId: String): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity)

    @Query("DELETE FROM routines WHERE id = :routineId")
    suspend fun deleteRoutine(routineId: String)

    @Transaction
    suspend fun selectRoutine(routineId: String) {
        deselectAllRoutines()
        setRoutineSelected(routineId, true)
    }

    @Query("UPDATE routines SET isSelected = 0")
    suspend fun deselectAllRoutines()

    @Query("UPDATE routines SET isSelected = :isSelected WHERE id = :routineId")
    suspend fun setRoutineSelected(routineId: String, isSelected: Boolean)

    @Query("UPDATE routines SET lastCompletedDayIndex = :dayIndex WHERE id = :routineId")
    suspend fun updateLastCompletedDayIndex(routineId: String, dayIndex: Int)

    @Query("SELECT * FROM workout_days WHERE routineId = :routineId ORDER BY `order` ASC")
    fun getDaysForRoutine(routineId: String): Flow<List<WorkoutDayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutDay(day: WorkoutDayEntity)

    @Query("DELETE FROM workout_days WHERE routineId = :routineId")
    suspend fun deleteDaysForRoutine(routineId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutDayExercise(dayExercise: WorkoutDayExerciseEntity)

    @Query("DELETE FROM workout_day_exercises WHERE workoutDayId IN (SELECT id FROM workout_days WHERE routineId = :routineId)")
    suspend fun deleteExercisesForRoutine(routineId: String)

    @Transaction
    suspend fun upsertRoutine(routine: RoutineEntity, daysWithExercises: List<Pair<WorkoutDayEntity, List<WorkoutDayExerciseEntity>>>) {
        // If updating, preserve isSelected and lastCompletedDayIndex
        val existing = getRoutineById(routine.id)
        val toInsert = if (existing != null) {
            routine.copy(isSelected = existing.isSelected, lastCompletedDayIndex = existing.lastCompletedDayIndex)
        } else {
            routine
        }
        
        insertRoutine(toInsert)
        deleteExercisesForRoutine(routine.id)
        deleteDaysForRoutine(routine.id)
        daysWithExercises.forEach { (day, exercises) ->
            insertWorkoutDay(day)
            exercises.forEach { exercise ->
                insertWorkoutDayExercise(exercise)
            }
        }
    }

    @Query("SELECT * FROM exercise_history ORDER BY date DESC")
    fun getAllHistory(): Flow<List<ExerciseHistoryEntity>>

    @Query("SELECT * FROM exercise_history WHERE exerciseId = :exerciseId ORDER BY date DESC")
    fun getHistoryForExercise(exerciseId: String): Flow<List<ExerciseHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseHistory(history: ExerciseHistoryEntity)
}
