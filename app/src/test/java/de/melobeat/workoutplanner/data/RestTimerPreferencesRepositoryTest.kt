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
    fun `setThemeMode persists and themeMode flow emits correct value`() = runTest {
        val repo = makeRepo()
        repo.setThemeMode("light")
        assertEquals("light", repo.themeMode.first())

        repo.setThemeMode("system")
        assertEquals("system", repo.themeMode.first())

        repo.setThemeMode("dark")
        assertEquals("dark", repo.themeMode.first())
    }

    @Test
    fun `themeMode defaults to dark when unset`() = runTest {
        val repo = makeRepo()
        assertEquals("dark", repo.themeMode.first())
    }

    @Test
    fun `setThemeMode rejects invalid mode`() = runTest {
        val repo = makeRepo()
        try {
            repo.setThemeMode("invalid")
            org.junit.Assert.fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
        // themeMode should still be the default
        assertEquals("dark", repo.themeMode.first())
    }
}