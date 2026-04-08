package de.melobeat.workoutplanner.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RestTimerPreferencesRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun makeRepo(): RestTimerPreferencesRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tmpFolder.newFile("test_prefs.preferences_pb") }
        )
        return RestTimerPreferencesRepository(dataStore)
    }

    @Test
    fun `returns defaults when nothing written`() = runTest {
        val repo = makeRepo()
        val settings = repo.settings.first()
        assertEquals(90, settings.betweenSetsEasySeconds)
        assertEquals(180, settings.betweenSetsHardSeconds)
        assertEquals(60, settings.betweenExercisesSeconds)
    }

    @Test
    fun `update roundtrip persists all three values`() = runTest {
        val repo = makeRepo()
        repo.update(RestTimerSettings(betweenSetsEasySeconds = 120, betweenSetsHardSeconds = 240, betweenExercisesSeconds = 90))
        val settings = repo.settings.first()
        assertEquals(120, settings.betweenSetsEasySeconds)
        assertEquals(240, settings.betweenSetsHardSeconds)
        assertEquals(90, settings.betweenExercisesSeconds)
    }

    @Test
    fun `update with default field values writes defaults for unchanged fields`() = runTest {
        val repo = makeRepo()
        repo.update(RestTimerSettings(betweenSetsEasySeconds = 45))
        val settings = repo.settings.first()
        assertEquals(45, settings.betweenSetsEasySeconds)
        assertEquals(180, settings.betweenSetsHardSeconds) // unchanged default
        assertEquals(60, settings.betweenExercisesSeconds)  // unchanged default
    }

    @Test
    fun `useDynamicColor defaults to false and can be toggled`() = runTest {
        val repo = makeRepo()
        assertEquals(false, repo.useDynamicColor.first())
        repo.setUseDynamicColor(true)
        assertEquals(true, repo.useDynamicColor.first())
        repo.setUseDynamicColor(false)
        assertEquals(false, repo.useDynamicColor.first())
    }
}