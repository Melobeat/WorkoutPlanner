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
    version = 8,
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
                    .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7, 8)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            val databaseContext = context.applicationContext
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val dao = getDatabase(databaseContext).workoutDao()
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
                                                defaultWeight = equip.defaultWeight
                                            )
                                        )
                                    }
                                    exercisesList.forEach { ex ->
                                        dao.insertExercise(
                                            ExerciseEntity(
                                                name = ex.name,
                                                muscleGroup = ex.muscleGroup,
                                                description = ex.description,
                                                equipmentId = ex.equipmentId
                                            )
                                        )
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("WorkoutDatabase", "Failed to seed initial data", e)
                                }
                            }
                        }
                    })
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
