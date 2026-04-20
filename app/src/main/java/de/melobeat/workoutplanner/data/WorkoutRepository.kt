package de.melobeat.workoutplanner.data

import de.melobeat.workoutplanner.di.IoDispatcher
import de.melobeat.workoutplanner.model.Equipment
import de.melobeat.workoutplanner.model.Exercise
import de.melobeat.workoutplanner.model.Routine
import de.melobeat.workoutplanner.model.WorkoutDay
import de.melobeat.workoutplanner.ui.ExerciseHistory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val dao: WorkoutDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {

    fun getRoutinesStream(): Flow<List<Routine>> =
        dao.getAllRoutinesWithDays().map { it.map(RoutineWithDays::toDomain) }

    fun getSelectedRoutineStream(): Flow<Routine?> =
        dao.getSelectedRoutineWithDays().map { it?.toDomain() }

    fun getRoutineStream(routineId: String): Flow<Routine?> =
        dao.getRoutineWithDays(routineId).map { it?.toDomain() }

    fun getExercisesStream(): Flow<List<Exercise>> =
        dao.getAllExercisesWithEquipment().map { it.map(ExerciseWithEquipment::toDomain) }

    fun getEquipmentStream(): Flow<List<Equipment>> =
        dao.getAllEquipment().map { it.map(EquipmentEntity::toDomain) }

    fun getWorkoutHistoryStream(): Flow<List<WorkoutHistoryWithExercises>> =
        dao.getAllWorkoutHistoryWithExercises()

    fun getHistoryForExercise(exerciseId: String): Flow<List<ExerciseHistoryEntity>> =
        dao.getHistoryForExercise(exerciseId)

    suspend fun selectRoutine(routineId: String) = withContext(dispatcher) {
        dao.selectRoutine(routineId)
    }

    suspend fun saveRoutine(
        name: String,
        description: String,
        days: List<WorkoutDay>,
        existingId: String?
    ) = withContext(dispatcher) {
        val routineId = existingId ?: UUID.randomUUID().toString()
        val routineEntity = RoutineEntity(id = routineId, name = name, description = description)
        val daysWithExercises = days.mapIndexed { dayIndex, day ->
            val dayId = if (day.id.isBlank() || day.id.startsWith("temp_"))
                UUID.randomUUID().toString() else day.id
            val dayEntity = WorkoutDayEntity(
                id = dayId, routineId = routineId, name = day.name, order = dayIndex
            )
            val exerciseEntities = day.exercises.mapIndexed { exIndex, ex ->
                WorkoutDayExerciseEntity(
                    workoutDayId = dayId,
                    exerciseId = ex.id,
                    routineSets = ex.routineSets,
                    order = exIndex,
                    sideType = ex.sideType.name
                )
            }
            dayEntity to exerciseEntities
        }
        dao.upsertRoutine(routineEntity, daysWithExercises)
    }

    suspend fun deleteRoutine(routineId: String) = withContext(dispatcher) {
        dao.deleteRoutine(routineId)
    }

    suspend fun updateLastCompletedDayIndex(routineId: String, dayIndex: Int) =
        withContext(dispatcher) {
            dao.updateLastCompletedDayIndex(routineId, dayIndex)
        }

    suspend fun saveExercise(
        name: String,
        muscleGroup: String,
        description: String,
        equipmentId: String?,
        existingId: String?,
        isBodyweight: Boolean = false,
        sideType: String = "Bilateral"
    ) = withContext(dispatcher) {
        dao.insertExercise(
            ExerciseEntity(
                id = existingId ?: UUID.randomUUID().toString(),
                name = name,
                muscleGroup = muscleGroup,
                description = description,
                equipmentId = equipmentId,
                isBodyweight = isBodyweight,
                sideType = sideType
            )
        )
    }

    suspend fun deleteExercise(exerciseId: String) = withContext(dispatcher) {
        dao.deleteExercise(exerciseId)
    }

    suspend fun saveEquipment(
        name: String,
        existingId: String?,
        defaultWeight: Double? = null,
        weightStep: Double = 2.5
    ) = withContext(dispatcher) {
        dao.insertEquipment(
            EquipmentEntity(
                id = existingId ?: UUID.randomUUID().toString(),
                name = name,
                defaultWeight = defaultWeight,
                weightStep = weightStep
            )
        )
    }

    suspend fun deleteEquipment(equipmentId: String) = withContext(dispatcher) {
        dao.deleteEquipment(equipmentId)
    }

    suspend fun finishWorkout(
        history: List<ExerciseHistory>,
        workoutDay: WorkoutDay,
        dayIndex: Int,
        durationMs: Long,
        routineName: String,
        routineId: String?
    ) = withContext(dispatcher) {
        val workoutHistoryId = UUID.randomUUID().toString()
        dao.insertWorkoutHistory(
            WorkoutHistoryEntity(
                id = workoutHistoryId,
                routineName = routineName,
                workoutDayName = workoutDay.name,
                date = System.currentTimeMillis(),
                durationMillis = durationMs
            )
        )
        history.forEach { entry ->
            dao.insertExerciseHistory(
                ExerciseHistoryEntity(
                    workoutHistoryId = workoutHistoryId,
                    exerciseId = entry.exerciseId,
                    date = System.currentTimeMillis(),
                    sets = entry.setIndex,
                    reps = entry.reps,
                    weight = entry.weight,
                    isAmrap = entry.isAmrap
                )
            )
        }
        routineId?.let { dao.updateLastCompletedDayIndex(it, dayIndex) }
    }
}
