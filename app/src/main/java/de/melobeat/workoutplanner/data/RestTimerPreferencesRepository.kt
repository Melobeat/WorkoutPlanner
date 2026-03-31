package de.melobeat.workoutplanner.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
data class RestTimerSettings(
    val betweenSetsEasySeconds: Int = 90,
    val betweenSetsHardSeconds: Int = 180,
    val betweenExercisesSeconds: Int = 60
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
            betweenSetsEasySeconds = prefs[BETWEEN_SETS_EASY] ?: 90,
            betweenSetsHardSeconds = prefs[BETWEEN_SETS_HARD] ?: 180,
            betweenExercisesSeconds = prefs[BETWEEN_EXERCISES] ?: 60
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