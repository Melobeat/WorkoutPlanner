package de.melobeat.workoutplanner.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import de.melobeat.workoutplanner.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserProfileRepository(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val AGE             = intPreferencesKey("user_profile_age")
        val HEIGHT_CM       = intPreferencesKey("user_profile_height_cm")
        val BODY_WEIGHT_KG  = doublePreferencesKey("user_profile_body_weight_kg")
    }

    val userProfile: Flow<UserProfile> = dataStore.data.map { prefs ->
        UserProfile(
            age = prefs[AGE],
            heightCm = prefs[HEIGHT_CM],
            bodyWeightKg = prefs[BODY_WEIGHT_KG]
        )
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        dataStore.edit { prefs ->
            if (profile.age != null) prefs[AGE] = profile.age else prefs.remove(AGE)
            if (profile.heightCm != null) prefs[HEIGHT_CM] = profile.heightCm else prefs.remove(HEIGHT_CM)
            if (profile.bodyWeightKg != null) prefs[BODY_WEIGHT_KG] = profile.bodyWeightKg else prefs.remove(BODY_WEIGHT_KG)
        }
    }
}
