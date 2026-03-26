package com.example.workoutplanner.ui

import app.cash.turbine.test
import com.example.workoutplanner.data.ExerciseHistoryEntity
import com.example.workoutplanner.data.WorkoutRepository
import com.example.workoutplanner.model.Exercise
import com.example.workoutplanner.model.RoutineSet
import com.example.workoutplanner.model.WorkoutDay
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<WorkoutRepository>(relaxed = true)
    private lateinit var viewModel: ActiveWorkoutViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getHistoryForExercise(any()) } returns flowOf(emptyList())
        viewModel = ActiveWorkoutViewModel(repository)
    }

    @After
    fun tearDown() {
        viewModel.cancelWorkout()
        Dispatchers.resetMain()
    }

    // region startWorkout

    @Test
    fun `startWorkout sets isActive and isFullScreen`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "My Routine", "routine1")

        val state = viewModel.uiState.value
        assertTrue(state.isActive)
        assertTrue(state.isFullScreen)
    }

    @Test
    fun `startWorkout populates workoutDayName`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "My Routine", "routine1")

        assertEquals("Push Day", viewModel.uiState.value.workoutDayName)
    }

    @Test
    fun `startWorkout creates one ExerciseUiState per exercise`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(exerciseCount = 3), 0, "My Routine", "routine1")

        assertEquals(3, viewModel.uiState.value.exercises.size)
    }

    @Test
    fun `startWorkout maps RoutineSets to SetUiState with correct reps and weight`() = runTest {
        val day = WorkoutDay(
            id = "d1", name = "Day",
            exercises = listOf(
                Exercise(
                    id = "e1", name = "Bench Press",
                    routineSets = listOf(RoutineSet(reps = 8, weight = 80.0))
                )
            )
        )
        viewModel.startWorkout(day, 0, "PPL", null)

        val set = viewModel.uiState.value.exercises[0].sets[0]
        assertEquals("8", set.reps)
        assertEquals("80", set.weight)
        assertFalse(set.isDone)
    }

    @Test
    fun `startWorkout uses last session weight when history exists`() = runTest {
        val historyEntry = mockk<ExerciseHistoryEntity>(relaxed = true) {
            every { workoutHistoryId } returns "w1"
            every { sets } returns 1
            every { weight } returns 100.0
            every { reps } returns 5
        }
        every { repository.getHistoryForExercise("e1") } returns flowOf(listOf(historyEntry))

        val day = WorkoutDay(
            id = "d1", name = "Day",
            exercises = listOf(
                Exercise(
                    id = "e1", name = "Squat",
                    routineSets = listOf(RoutineSet(reps = 5, weight = 80.0))
                )
            )
        )
        viewModel.startWorkout(day, 0, "PPL", null)

        // Weight should come from last session (100kg) not from routine (80kg)
        assertEquals("100", viewModel.uiState.value.exercises[0].sets[0].weight)
    }

    // endregion

    // region cancelWorkout

    @Test
    fun `cancelWorkout resets state to defaults`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "My Routine", "routine1")
        viewModel.cancelWorkout()

        val state = viewModel.uiState.value
        assertFalse(state.isActive)
        assertFalse(state.isFinished)
        assertTrue(state.exercises.isEmpty())
        assertEquals("", state.workoutDayName)
    }

    @Test
    fun `cancelWorkout clears elapsed time`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "My Routine", "routine1")
        viewModel.cancelWorkout()

        assertEquals(0L, viewModel.uiState.value.elapsedTime)
    }

    // endregion

    // region finishWorkout

    @Test
    fun `finishWorkout sets isFinished true`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "My Routine", "routine1")
        viewModel.finishWorkout()

        assertTrue(viewModel.uiState.value.isFinished)
    }

    @Test
    fun `finishWorkout resets isActive`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "My Routine", "routine1")
        viewModel.finishWorkout()

        assertFalse(viewModel.uiState.value.isActive)
    }

    @Test
    fun `finishWorkout saves to repository exactly once even when called twice`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "My Routine", "routine1")
        viewModel.finishWorkout()
        viewModel.finishWorkout() // second call — currentWorkoutDay is now null, should no-op

        coVerify(exactly = 1) {
            repository.finishWorkout(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `finishWorkout before startWorkout does nothing`() = runTest {
        viewModel.finishWorkout()

        assertFalse(viewModel.uiState.value.isFinished)
        coVerify(exactly = 0) {
            repository.finishWorkout(any(), any(), any(), any(), any(), any())
        }
    }

    // endregion

    // region toggleSetDone

    @Test
    fun `toggleSetDone marks undone set as done`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)

        viewModel.toggleSetDone(0, 0)

        assertTrue(viewModel.uiState.value.exercises[0].sets[0].isDone)
    }

    @Test
    fun `toggleSetDone decrements reps when set is already done`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "R", null) // sets have reps = "10"
        viewModel.toggleSetDone(0, 0) // isDone = true, reps = "10"

        viewModel.toggleSetDone(0, 0) // should decrement → reps = "9", still done

        val set = viewModel.uiState.value.exercises[0].sets[0]
        assertEquals("9", set.reps)
        assertTrue(set.isDone)
    }

    @Test
    fun `toggleSetDone resets to originalReps and clears done when reps reach zero`() = runTest {
        val day = WorkoutDay(
            id = "d1", name = "Day",
            exercises = listOf(
                Exercise(
                    id = "e1", name = "Ex",
                    routineSets = listOf(RoutineSet(reps = 1, weight = 0.0))
                )
            )
        )
        viewModel.startWorkout(day, 0, "R", null)

        viewModel.toggleSetDone(0, 0) // isDone=true, reps="1"
        viewModel.toggleSetDone(0, 0) // isDone=true, reps="0"
        viewModel.toggleSetDone(0, 0) // reps==0 → reset to originalReps, isDone=false

        val set = viewModel.uiState.value.exercises[0].sets[0]
        assertFalse(set.isDone)
        assertEquals("1", set.reps)
    }

    @Test
    fun `toggleSetDone on AMRAP set does nothing`() = runTest {
        val day = WorkoutDay(
            id = "d1", name = "Day",
            exercises = listOf(
                Exercise(
                    id = "e1", name = "Ex",
                    routineSets = listOf(RoutineSet(reps = 5, weight = 0.0, isAmrap = true))
                )
            )
        )
        viewModel.startWorkout(day, 0, "R", null)
        val before = viewModel.uiState.value.exercises[0].sets[0]

        viewModel.toggleSetDone(0, 0)

        assertEquals(before, viewModel.uiState.value.exercises[0].sets[0])
    }

    // endregion

    // region set management

    @Test
    fun `addSet appends a new set`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)
        val before = viewModel.uiState.value.exercises[0].sets.size

        viewModel.addSet(0)

        assertEquals(before + 1, viewModel.uiState.value.exercises[0].sets.size)
    }

    @Test
    fun `removeSet removes the set at the given index`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "R", null) // 2 sets per exercise
        viewModel.removeSet(0, 0)

        assertEquals(1, viewModel.uiState.value.exercises[0].sets.size)
    }

    @Test
    fun `removeSet does not remove the last remaining set`() = runTest {
        val day = WorkoutDay(
            id = "d1", name = "Day",
            exercises = listOf(
                Exercise(id = "e1", name = "Ex", routineSets = listOf(RoutineSet(10, 0.0)))
            )
        )
        viewModel.startWorkout(day, 0, "R", null)

        viewModel.removeSet(0, 0)

        assertEquals(1, viewModel.uiState.value.exercises[0].sets.size)
    }

    @Test
    fun `removeSet re-indexes remaining sets`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "R", null) // sets: index 0, 1
        viewModel.removeSet(0, 0) // remove first

        assertEquals(0, viewModel.uiState.value.exercises[0].sets[0].index)
    }

    // endregion

    // region exercise management

    @Test
    fun `removeExercise removes the correct exercise`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(exerciseCount = 3), 0, "R", null)
        val secondName = viewModel.uiState.value.exercises[1].name

        viewModel.removeExercise(0)

        assertEquals(2, viewModel.uiState.value.exercises.size)
        assertEquals(secondName, viewModel.uiState.value.exercises[0].name)
    }

    @Test
    fun `reorderExercise moves exercise from source to target position`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(exerciseCount = 3), 0, "R", null)
        val originalFirst = viewModel.uiState.value.exercises[0].name

        viewModel.reorderExercise(from = 0, to = 2)

        assertEquals(originalFirst, viewModel.uiState.value.exercises[2].name)
    }

    @Test
    fun `toggleExerciseExpanded flips expanded state`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)
        val initial = viewModel.uiState.value.exercises[0].isExpanded

        viewModel.toggleExerciseExpanded(0)

        assertEquals(!initial, viewModel.uiState.value.exercises[0].isExpanded)
    }

    // endregion

    // region setFullScreen

    @Test
    fun `setFullScreen updates isFullScreen in state`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.setFullScreen(true)
            assertTrue(awaitItem().isFullScreen)

            viewModel.setFullScreen(false)
            assertFalse(awaitItem().isFullScreen)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region helpers

    private fun makeWorkoutDay(exerciseCount: Int = 2) = WorkoutDay(
        id = "day1",
        name = "Push Day",
        exercises = (1..exerciseCount).map { i ->
            Exercise(
                id = "ex$i",
                name = "Exercise $i",
                muscleGroup = "Chest",
                routineSets = listOf(
                    RoutineSet(reps = 10, weight = 50.0),
                    RoutineSet(reps = 8, weight = 50.0)
                )
            )
        }
    )

    // endregion
}
