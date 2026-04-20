package de.melobeat.workoutplanner.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import de.melobeat.workoutplanner.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UserProfileRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun makeRepo(): UserProfileRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tmpFolder.newFile("user_profile_prefs.preferences_pb") }
        )
        return UserProfileRepository(dataStore)
    }

    @Test
    fun `returns null defaults when nothing written`() = runTest {
        val repo = makeRepo()
        val profile = repo.userProfile.first()
        assertNull(profile.age)
        assertNull(profile.heightCm)
        assertNull(profile.bodyWeightKg)
    }

    @Test
    fun `saveUserProfile roundtrip persists all values`() = runTest {
        val repo = makeRepo()
        repo.saveUserProfile(UserProfile(age = 30, heightCm = 180, bodyWeightKg = 80.5))
        val profile = repo.userProfile.first()
        assertEquals(30, profile.age)
        assertEquals(180, profile.heightCm)
        assertEquals(80.5, profile.bodyWeightKg)
    }

    @Test
    fun `saveUserProfile with null values clears stored values`() = runTest {
        val repo = makeRepo()
        repo.saveUserProfile(UserProfile(age = 25, heightCm = 175, bodyWeightKg = 70.0))
        repo.saveUserProfile(UserProfile(age = null, heightCm = null, bodyWeightKg = null))
        val profile = repo.userProfile.first()
        assertNull(profile.age)
        assertNull(profile.heightCm)
        assertNull(profile.bodyWeightKg)
    }

    @Test
    fun `saveUserProfile partial update preserves other null values`() = runTest {
        val repo = makeRepo()
        repo.saveUserProfile(UserProfile(age = 28))
        val profile = repo.userProfile.first()
        assertEquals(28, profile.age)
        assertNull(profile.heightCm)
        assertNull(profile.bodyWeightKg)
    }
}
