package de.melobeat.workoutplanner

import de.melobeat.workoutplanner.domain.model.Exercise
import de.melobeat.workoutplanner.domain.util.filterExercises
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseFilterTest {

    private val exercises = listOf(
        Exercise(id = "1", name = "Bench Press", muscleGroup = "Chest"),
        Exercise(id = "2", name = "Squat", muscleGroup = "Legs"),
        Exercise(id = "3", name = "Overhead Press", muscleGroup = "Shoulders"),
        Exercise(id = "4", name = "Leg Press", muscleGroup = "Legs")
    )

    @Test
    fun `blank query returns all exercises`() {
        assertEquals(exercises, filterExercises(exercises, ""))
    }

    @Test
    fun `whitespace only query returns all exercises`() {
        assertEquals(exercises, filterExercises(exercises, "   "))
    }

    @Test
    fun `query matches exercise name case insensitively`() {
        val result = filterExercises(exercises, "bench")
        assertEquals(listOf(exercises[0]), result)
    }

    @Test
    fun `query matches muscle group case insensitively`() {
        val result = filterExercises(exercises, "legs")
        assertEquals(listOf(exercises[1], exercises[3]), result)
    }

    @Test
    fun `query with no matches returns empty list`() {
        val result = filterExercises(exercises, "zzz")
        assertEquals(emptyList<Exercise>(), result)
    }

    @Test
    fun `query matches partial name`() {
        val result = filterExercises(exercises, "press")
        assertEquals(listOf(exercises[0], exercises[2], exercises[3]), result)
    }
}
