package de.melobeat.workoutplanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import de.melobeat.workoutplanner.domain.model.RoutineSet
import java.util.UUID

@Database(
    entities = [
        ExerciseEntity::class,
        RoutineEntity::class,
        WorkoutDayEntity::class,
        WorkoutDayExerciseEntity::class,
        WorkoutHistoryEntity::class,
        ExerciseHistoryEntity::class,
        EquipmentEntity::class
    ],
    version = 11,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var Instance: WorkoutDatabase? = null

        fun getDatabase(context: Context): WorkoutDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, WorkoutDatabase::class.java, "workout_database")
                    .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seedDatabase(context)
                        }

                        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                            super.onDestructiveMigration(db)
                            seedDatabase(context)
                        }
                    })
                    .build()
                    .also { Instance = it }
            }
        }

        private fun seedDatabase(context: Context) {
            val databaseContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = Instance!!.workoutDao()
                    val json = Json { ignoreUnknownKeys = true }

                    val equipmentJson = databaseContext.assets.open("equipment.json")
                        .bufferedReader().use { it.readText() }
                    val equipmentList = json.decodeFromString<List<InitialEquipment>>(equipmentJson)

                    val exercisesJson = databaseContext.assets.open("exercises.json")
                        .bufferedReader().use { it.readText() }
                    val exercisesList = json.decodeFromString<List<InitialExercise>>(exercisesJson)

                    equipmentList.forEach { equip ->
                        dao.insertEquipment(
                            EquipmentEntity(
                                id = equip.id,
                                name = equip.name,
                                defaultWeight = equip.defaultWeight,
                                weightStep = equip.weightStep ?: 2.5
                            )
                        )
                    }
                    exercisesList.forEach { ex ->
                        dao.insertExercise(
                            ExerciseEntity(
                                name = ex.name,
                                muscleGroup = ex.muscleGroup,
                                description = ex.description,
                                equipmentId = ex.equipmentId,
                                sideType = ex.sideType
                            )
                        )
                    }

                    // Seed routines
                    val routinesJson = databaseContext.assets.open("routines.json")
                        .bufferedReader().use { it.readText() }
                    val routinesList = json.decodeFromString<List<InitialRoutine>>(routinesJson)

                    // Build name-to-id map for exercises
                    val exerciseIds = dao.getAllExercisesSync().associateBy { it.name }

                    routinesList.forEach { routine ->
                        val routineId = UUID.randomUUID().toString()
                        val routineEntity = RoutineEntity(
                            id = routineId,
                            name = routine.name,
                            description = routine.description
                        )

                        val daysWithExercises = routine.days.mapIndexed { dayIndex, day ->
                            val dayId = UUID.randomUUID().toString()
                            val dayEntity = WorkoutDayEntity(
                                id = dayId,
                                routineId = routineId,
                                name = day.name,
                                order = dayIndex
                            )

                            val exerciseEntities = day.exercises.mapIndexed { exIndex, ex ->
                                val matchedExercise = exerciseIds[ex.exerciseName]
                                val exerciseEntityId = matchedExercise?.id ?: ""
                                val sets = ex.sets.map { set ->
                                    RoutineSet(
                                        reps = set.reps,
                                        weight = set.weight,
                                        isAmrap = set.isAmrap,
                                        sideType = set.sideType,
                                        leftReps = set.leftReps,
                                        rightReps = set.rightReps
                                    )
                                }
                                WorkoutDayExerciseEntity(
                                    id = UUID.randomUUID().toString(),
                                    workoutDayId = dayId,
                                    exerciseId = exerciseEntityId,
                                    routineSets = sets,
                                    order = exIndex,
                                    sideType = ex.sideType
                                )
                            }

                            dayEntity to exerciseEntities
                        }

                        dao.upsertRoutine(routineEntity, daysWithExercises)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("WorkoutDatabase", "Failed to seed initial data", e)
                }
            }
        }
    }
}
