package de.melobeat.workoutplanner.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val DEFAULT_BETWEEN_SETS_EASY_SECONDS = 90
const val DEFAULT_BETWEEN_SETS_HARD_SECONDS = 180
const val DEFAULT_BETWEEN_EXERCISES_SECONDS = 60

data class RestTimerSettings(
    val betweenSetsEasySeconds: Int = DEFAULT_BETWEEN_SETS_EASY_SECONDS,
    val betweenSetsHardSeconds: Int = DEFAULT_BETWEEN_SETS_HARD_SECONDS,
    val betweenExercisesSeconds: Int = DEFAULT_BETWEEN_EXERCISES_SECONDS
)

class RestTimerPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val BETWEEN_SETS_EASY = intPreferencesKey("between_sets_easy_seconds")
        val BETWEEN_SETS_HARD = intPreferencesKey("between_sets_hard_seconds")
        val BETWEEN_EXERCISES = intPreferencesKey("between_exercises_seconds")
    }

    val settings: Flow<RestTimerSettings> = dataStore.data.map { prefs ->
        RestTimerSettings(
            betweenSetsEasySeconds = prefs[BETWEEN_SETS_EASY] ?: DEFAULT_BETWEEN_SETS_EASY_SECONDS,
            betweenSetsHardSeconds = prefs[BETWEEN_SETS_HARD] ?: DEFAULT_BETWEEN_SETS_HARD_SECONDS,
            betweenExercisesSeconds = prefs[BETWEEN_EXERCISES] ?: DEFAULT_BETWEEN_EXERCISES_SECONDS
        )
    }

    suspend fun update(settings: RestTimerSettings) {
        dataStore.edit { prefs ->
            prefs[BETWEEN_SETS_EASY] = settings.betweenSetsEasySeconds
            prefs[BETWEEN_SETS_HARD] = settings.betweenSetsHardSeconds
            prefs[BETWEEN_EXERCISES] = settings.betweenExercisesSeconds
        }
    }
}
