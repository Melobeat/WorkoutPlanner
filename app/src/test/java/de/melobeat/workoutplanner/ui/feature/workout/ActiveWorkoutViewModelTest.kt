package de.melobeat.workoutplanner.ui.feature.workout

import app.cash.turbine.test
import de.melobeat.workoutplanner.data.ExerciseHistoryEntity
import de.melobeat.workoutplanner.data.RestTimerPreferencesRepository
import de.melobeat.workoutplanner.data.RestTimerSettings
import de.melobeat.workoutplanner.data.WorkoutRepository
import de.melobeat.workoutplanner.model.Equipment
import de.melobeat.workoutplanner.model.Exercise
import de.melobeat.workoutplanner.model.RoutineSet
import de.melobeat.workoutplanner.model.SideType
import de.melobeat.workoutplanner.model.WorkoutDay
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
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<WorkoutRepository>(relaxed = true)
    private val timerPrefs = mockk<RestTimerPreferencesRepository>(relaxed = true)
    private lateinit var viewModel: ActiveWorkoutViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getHistoryForExercise(any()) } returns flowOf(emptyList())
        every { repository.getEquipmentStream() } returns flowOf(emptyList())
        every { timerPrefs.settings } returns flowOf(RestTimerSettings())
        viewModel = ActiveWorkoutViewModel(repository, timerPrefs)
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
        Assert.assertTrue(state.isActive)
        Assert.assertTrue(state.isFullScreen)
    }

    @Test
    fun `startWorkout populates workoutDayName`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "My Routine", "routine1")

        Assert.assertEquals("Push Day", viewModel.uiState.value.workoutDayName)
    }

    @Test
    fun `startWorkout creates one ExerciseUiState per exercise`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(exerciseCount = 3), 0, "My Routine", "routine1")

        Assert.assertEquals(3, viewModel.uiState.value.exercises.size)
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
        Assert.assertEquals("8", set.reps)
        Assert.assertEquals("80", set.weight)
        Assert.assertFalse(set.isDone)
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
        Assert.assertEquals("100", viewModel.uiState.value.exercises[0].sets[0].weight)
    }

    @Test
    fun `startWorkout only expands the first exercise`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(exerciseCount = 3), 0, "My Routine", "routine1")

        val exercises = viewModel.uiState.value.exercises
        Assert.assertTrue(exercises[0].isExpanded)
        Assert.assertFalse(exercises[1].isExpanded)
        Assert.assertFalse(exercises[2].isExpanded)
    }

    // endregion

    // region cancelWorkout

    @Test
    fun `cancelWorkout resets state to defaults`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "My Routine", "routine1")
        viewModel.cancelWorkout()

        val state = viewModel.uiState.value
        Assert.assertFalse(state.isActive)
        Assert.assertFalse(state.isFinished)
        Assert.assertTrue(state.exercises.isEmpty())
        Assert.assertEquals("", state.workoutDayName)
    }

    @Test
    fun `cancelWorkout clears elapsed time`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "My Routine", "routine1")
        viewModel.cancelWorkout()

        Assert.assertEquals(0L, viewModel.uiState.value.elapsedTime)
    }

    // endregion

    // region finishWorkout

    @Test
    fun `finishWorkout sets isFinished true`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "My Routine", "routine1")
        viewModel.finishWorkout()

        Assert.assertTrue(viewModel.uiState.value.isFinished)
    }

    @Test
    fun `finishWorkout resets isActive`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "My Routine", "routine1")
        viewModel.finishWorkout()

        Assert.assertFalse(viewModel.uiState.value.isActive)
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

        Assert.assertFalse(viewModel.uiState.value.isFinished)
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

        Assert.assertTrue(viewModel.uiState.value.exercises[0].sets[0].isDone)
    }

    @Test
    fun `toggleSetDone decrements reps when set is already done`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "R", null) // sets have reps = "10"
        viewModel.toggleSetDone(0, 0) // isDone = true, reps = "10"

        viewModel.toggleSetDone(0, 0) // should decrement → reps = "9", still done

        val set = viewModel.uiState.value.exercises[0].sets[0]
        Assert.assertEquals("9", set.reps)
        Assert.assertTrue(set.isDone)
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
        Assert.assertFalse(set.isDone)
        Assert.assertEquals("1", set.reps)
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

        Assert.assertEquals(before, viewModel.uiState.value.exercises[0].sets[0])
    }

    // endregion

    // region set management

    @Test
    fun `addSet appends a new set`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)
        val before = viewModel.uiState.value.exercises[0].sets.size

        viewModel.addSet(0)

        Assert.assertEquals(before + 1, viewModel.uiState.value.exercises[0].sets.size)
    }

    @Test
    fun `removeSet removes the set at the given index`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "R", null) // 2 sets per exercise
        viewModel.removeSet(0, 0)

        Assert.assertEquals(1, viewModel.uiState.value.exercises[0].sets.size)
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

        Assert.assertEquals(1, viewModel.uiState.value.exercises[0].sets.size)
    }

    @Test
    fun `removeSet re-indexes remaining sets`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "R", null) // sets: index 0, 1
        viewModel.removeSet(0, 0) // remove first

        Assert.assertEquals(0, viewModel.uiState.value.exercises[0].sets[0].index)
    }

    // endregion

    // region exercise management

    @Test
    fun `removeExercise removes the correct exercise`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(exerciseCount = 3), 0, "R", null)
        val secondName = viewModel.uiState.value.exercises[1].name

        viewModel.removeExercise(0)

        Assert.assertEquals(2, viewModel.uiState.value.exercises.size)
        Assert.assertEquals(secondName, viewModel.uiState.value.exercises[0].name)
    }

    @Test
    fun `reorderExercise moves exercise from source to target position`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(exerciseCount = 3), 0, "R", null)
        val originalFirst = viewModel.uiState.value.exercises[0].name

        viewModel.reorderExercise(from = 0, to = 2)

        Assert.assertEquals(originalFirst, viewModel.uiState.value.exercises[2].name)
    }

    @Test
    fun `toggleExerciseExpanded flips expanded state`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)
        val initial = viewModel.uiState.value.exercises[0].isExpanded

        viewModel.toggleExerciseExpanded(0)

        Assert.assertEquals(!initial, viewModel.uiState.value.exercises[0].isExpanded)
    }

    // endregion

    // region setFullScreen

    @Test
    fun `setFullScreen updates isFullScreen in state`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.setFullScreen(true)
            Assert.assertTrue(awaitItem().isFullScreen)

            viewModel.setFullScreen(false)
            Assert.assertFalse(awaitItem().isFullScreen)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region goToPreviousSet

    @Test
    fun `goToPreviousSet decrements setIndex when not on first set`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)
        // advance to set 1 first
        viewModel.completeCurrentSet()

        viewModel.goToPreviousSet()

        Assert.assertEquals(0, viewModel.uiState.value.currentSetIndex)
        Assert.assertEquals(0, viewModel.uiState.value.currentExerciseIndex)
    }

    @Test
    fun `goToPreviousSet wraps to last set of previous exercise when on first set`() = runTest {
        // makeWorkoutDay gives 2 exercises, each with 2 sets
        viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)
        // advance past exercise 0 entirely: complete set 0, complete set 1 → moves to exercise 1 set 0
        viewModel.completeCurrentSet() // set 0 done → set 1
        viewModel.completeCurrentSet() // set 1 done → exercise 1 set 0

        viewModel.goToPreviousSet()

        Assert.assertEquals(0, viewModel.uiState.value.currentExerciseIndex)
        Assert.assertEquals(
            1,
            viewModel.uiState.value.currentSetIndex
        ) // last set of exercise 0 (2 sets → index 1)
    }

    @Test
    fun `goToPreviousSet does nothing when at first set of first exercise`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)

        viewModel.goToPreviousSet()

        Assert.assertEquals(0, viewModel.uiState.value.currentExerciseIndex)
        Assert.assertEquals(0, viewModel.uiState.value.currentSetIndex)
    }

    @Test
    fun `goToPreviousSet does not change isDone state of any set`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)
        viewModel.completeCurrentSet() // completes set 0, moves to set 1

        viewModel.goToPreviousSet() // back to set 0

        Assert.assertTrue(viewModel.uiState.value.exercises[0].sets[0].isDone)
    }

    // endregion

    // region skipExercise

    @Test
    fun `skipExercise advances to next exercise and resets setIndex`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(exerciseCount = 3), 0, "R", null)

        viewModel.skipExercise()

        Assert.assertEquals(1, viewModel.uiState.value.currentExerciseIndex)
        Assert.assertEquals(0, viewModel.uiState.value.currentSetIndex)
    }

    @Test
    fun `skipExercise leaves skipped exercise sets as not done`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(exerciseCount = 2), 0, "R", null)

        viewModel.skipExercise()

        val skippedSets = viewModel.uiState.value.exercises[0].sets
        Assert.assertTrue(skippedSets.all { !it.isDone })
    }

    @Test
    fun `skipExercise on last exercise triggers showSummary`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(exerciseCount = 1), 0, "R", null)

        viewModel.skipExercise()

        Assert.assertTrue(viewModel.uiState.value.showSummary)
    }

    // endregion

    // region rest timer

    @Test
    fun `completeCurrentSet mid-exercise starts BetweenSets rest timer`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)

        viewModel.completeCurrentSet() // set 0 → set 1, should start BetweenSets timer

        Assert.assertEquals(
            RestTimerContext.BetweenSets,
            viewModel.uiState.value.restTimer?.context
        )
    }

    @Test
    fun `completeCurrentSet on last set of non-final exercise starts BetweenExercises rest timer`() =
        runTest {
            viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)

            viewModel.completeCurrentSet() // set 0 → set 1
            viewModel.completeCurrentSet() // set 1 (last) → exercise 1, should start BetweenExercises timer

            Assert.assertEquals(
                RestTimerContext.BetweenExercises,
                viewModel.uiState.value.restTimer?.context
            )
        }

    @Test
    fun `starting a new set resets rest timer elapsed seconds to zero`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)

        viewModel.completeCurrentSet() // rest timer starts (BetweenSets)
        Assert.assertEquals(0, viewModel.uiState.value.restTimer?.elapsedSeconds)

        viewModel.completeCurrentSet() // new rest timer starts (BetweenExercises), also elapsed=0
        Assert.assertEquals(0, viewModel.uiState.value.restTimer?.elapsedSeconds)
    }

    // endregion

    // region jumpToSet

    @Test
    fun `jumpToSet moves cursor to given exercise and set`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(exerciseCount = 3), 0, "R", "r1")

        viewModel.jumpToSet(exerciseIndex = 2, setIndex = 1)

        val state = viewModel.uiState.value
        Assert.assertEquals(2, state.currentExerciseIndex)
        Assert.assertEquals(1, state.currentSetIndex)
    }

    @Test
    fun `jumpToSet expands the target exercise`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(exerciseCount = 3), 0, "R", "r1")
        // exercise 2 starts collapsed (only exercise 0 is expanded on start)
        Assert.assertFalse(viewModel.uiState.value.exercises[2].isExpanded)

        viewModel.jumpToSet(exerciseIndex = 2, setIndex = 0)

        Assert.assertTrue(viewModel.uiState.value.exercises[2].isExpanded)
    }

    @Test
    fun `jumpToSet does not modify isDone on any set`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(exerciseCount = 2), 0, "R", "r1")
        // complete set 0 of exercise 0
        viewModel.completeCurrentSet()
        Assert.assertTrue(viewModel.uiState.value.exercises[0].sets[0].isDone)

        viewModel.jumpToSet(exerciseIndex = 1, setIndex = 0)

        // set 0 of exercise 0 still done
        Assert.assertTrue(viewModel.uiState.value.exercises[0].sets[0].isDone)
    }

    @Test
    fun `completeCurrentSet auto-collapses exercise when all its sets are done`() = runTest {
        val day = WorkoutDay(
            id = "d1", name = "Day",
            exercises = listOf(
                Exercise(
                    id = "e1", name = "Ex1", muscleGroup = "",
                    routineSets = listOf(RoutineSet(reps = 5, weight = 50.0))
                ),
                Exercise(
                    id = "e2", name = "Ex2", muscleGroup = "",
                    routineSets = listOf(RoutineSet(reps = 5, weight = 50.0))
                )
            )
        )
        viewModel.startWorkout(day, 0, "R", "r1")
        Assert.assertTrue(viewModel.uiState.value.exercises[0].isExpanded)

        // Complete the only set of exercise 0 — should auto-collapse it
        viewModel.completeCurrentSet()

        Assert.assertFalse(viewModel.uiState.value.exercises[0].isExpanded)
        Assert.assertTrue(viewModel.uiState.value.exercises[1].isExpanded)
    }

    @Test
    fun `completeCurrentSet expands next exercise even if it was manually collapsed`() = runTest {
        val day = WorkoutDay(
            id = "d1", name = "Day",
            exercises = listOf(
                Exercise(
                    id = "e1", name = "Ex1", muscleGroup = "",
                    routineSets = listOf(RoutineSet(reps = 5, weight = 50.0))
                ),
                Exercise(
                    id = "e2", name = "Ex2", muscleGroup = "",
                    routineSets = listOf(RoutineSet(reps = 5, weight = 50.0))
                )
            )
        )
        viewModel.startWorkout(day, 0, "R", "r1")
        // exercise 1 already starts collapsed; toggling it expands it, then collapse it again
        // to explicitly verify completeCurrentSet still forces it open
        viewModel.toggleExerciseExpanded(1) // now expanded
        viewModel.toggleExerciseExpanded(1) // back to collapsed
        Assert.assertFalse(viewModel.uiState.value.exercises[1].isExpanded)

        // Complete the only set of exercise 0 — exercise 1 must be expanded
        viewModel.completeCurrentSet()

        Assert.assertTrue(viewModel.uiState.value.exercises[1].isExpanded)
    }

    @Test
    fun `goToPreviousSet expands the target exercise when going back`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(exerciseCount = 2), 0, "R", "r1")
        // advance to exercise 1
        viewModel.jumpToSet(exerciseIndex = 1, setIndex = 0)
        // collapse exercise 0
        viewModel.toggleExerciseExpanded(0)
        Assert.assertFalse(viewModel.uiState.value.exercises[0].isExpanded)

        // go back to exercise 0
        viewModel.goToPreviousSet()

        Assert.assertTrue(viewModel.uiState.value.exercises[0].isExpanded)
    }

    // endregion

    // region weightStep

    @Test
    fun `incrementWeight uses exercise weightStep`() = runTest {
        val equipment = listOf(Equipment(id = "equip1", name = "Test", weightStep = 1.0))
        every { repository.getEquipmentStream() } returns flowOf(equipment)

        val day = WorkoutDay(
            id = "d1", name = "Day",
            exercises = listOf(
                Exercise(
                    id = "ex1", name = "Ex", equipmentId = "equip1",
                    routineSets = listOf(RoutineSet(reps = 10, weight = 50.0))
                )
            )
        )
        val freshVm = ActiveWorkoutViewModel(repository, timerPrefs)
        freshVm.startWorkout(day, 0, "R", null)

        freshVm.incrementWeight(0, 0)

        val set = freshVm.uiState.value.exercises[0].sets[0]
        Assert.assertEquals("51", set.weight)
    }

    @Test
    fun `decrementWeight uses exercise weightStep`() = runTest {
        val equipment = listOf(Equipment(id = "equip1", name = "Test", weightStep = 1.25))
        every { repository.getEquipmentStream() } returns flowOf(equipment)

        val day = WorkoutDay(
            id = "d1", name = "Day",
            exercises = listOf(
                Exercise(
                    id = "ex1", name = "Ex", equipmentId = "equip1",
                    routineSets = listOf(RoutineSet(reps = 10, weight = 50.0))
                )
            )
        )
        val freshVm = ActiveWorkoutViewModel(repository, timerPrefs)
        freshVm.startWorkout(day, 0, "R", null)

        freshVm.incrementWeight(0, 0)
        freshVm.incrementWeight(0, 0)
        freshVm.incrementWeight(0, 0) // 50 + 3 * 1.25 = 53.75

        freshVm.decrementWeight(0, 0) // 53.75 - 1.25 = 52.5

        val set = freshVm.uiState.value.exercises[0].sets[0]
        Assert.assertEquals("52.5", set.weight)
    }

    @Test
    fun `decrementWeight does nothing when weight is less than weightStep`() = runTest {
        val equipment = listOf(Equipment(id = "equip1", name = "Test", weightStep = 2.5))
        every { repository.getEquipmentStream() } returns flowOf(equipment)

        val day = WorkoutDay(
            id = "d1", name = "Day",
            exercises = listOf(
                Exercise(
                    id = "ex1", name = "Ex", equipmentId = "equip1",
                    routineSets = listOf(RoutineSet(reps = 10, weight = 0.0))
                )
            )
        )
        val freshVm = ActiveWorkoutViewModel(repository, timerPrefs)
        freshVm.startWorkout(day, 0, "R", null)

        freshVm.decrementWeight(0, 0) // weight is 0, should stay 0

        val set = freshVm.uiState.value.exercises[0].sets[0]
        Assert.assertEquals("0", set.weight)
    }

    @Test
    fun `exercises use fallback weightStep 1_0 when equipment stream is empty`() = runTest {
        every { repository.getEquipmentStream() } returns flowOf(emptyList())

        val day = WorkoutDay(
            id = "d1", name = "Day",
            exercises = listOf(
                Exercise(
                    id = "ex1", name = "Ex", equipmentId = "equip_unknown",
                    routineSets = listOf(RoutineSet(reps = 10, weight = 50.0))
                )
            )
        )
        val freshVm = ActiveWorkoutViewModel(repository, timerPrefs)
        freshVm.startWorkout(day, 0, "R", null)

        freshVm.incrementWeight(0, 0) // 50 + 1.0 = 51

        val set = freshVm.uiState.value.exercises[0].sets[0]
        Assert.assertEquals("51", set.weight)
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

    private fun makeUnilateralExercise() = Exercise(
        id = "e_uni",
        name = "Bicep Curl",
        muscleGroup = "Arms",
        sideType = SideType.Unilateral,
        routineSets = listOf(
            RoutineSet(
                reps = 10,
                weight = 15.0,
                sideType = "Unilateral",
                leftReps = 10,
                rightReps = 10
            ),
            RoutineSet(
                reps = 8,
                weight = 15.0,
                sideType = "Unilateral",
                leftReps = 8,
                rightReps = 8
            )
        )
    )

    // endregion

    // region unilateral / bilateral

    @Test
    fun `toggleSetDone on unilateral set does nothing when only one side has reps`() = runTest {
        val day = WorkoutDay(
            id = "d1", name = "Day",
            exercises = listOf(
                Exercise(
                    id = "e_uni", name = "Bicep Curl", muscleGroup = "Arms",
                    sideType = SideType.Unilateral,
                    routineSets = listOf(
                        RoutineSet(
                            reps = 10,
                            weight = 15.0,
                            sideType = "Unilateral",
                            leftReps = 10,
                            rightReps = 0
                        )
                    )
                )
            )
        )
        viewModel.startWorkout(day, 0, "R", null)

        viewModel.toggleSetDone(0, 0)

        Assert.assertFalse(viewModel.uiState.value.exercises[0].sets[0].isDone)
    }

    @Test
    fun `toggleSetDone on done unilateral set decrements both sides`() = runTest {
        val day = WorkoutDay(
            id = "d1", name = "Day",
            exercises = listOf(makeUnilateralExercise())
        )
        viewModel.startWorkout(day, 0, "R", null)

        viewModel.setLeftReps(0, 0, 10)
        viewModel.setRightReps(0, 0, 10)
        viewModel.toggleSetDone(0, 0)

        viewModel.toggleSetDone(0, 0)

        val set = viewModel.uiState.value.exercises[0].sets[0]
        Assert.assertEquals(9, set.leftReps)
        Assert.assertEquals(9, set.rightReps)
        Assert.assertTrue(set.isDone)
    }

    @Test
    fun `toggleSetDone on unilateral set resets when both sides reach zero`() = runTest {
        val day = WorkoutDay(
            id = "d1", name = "Day",
            exercises = listOf(
                Exercise(
                    id = "e1", name = "Ex",
                    sideType = SideType.Unilateral,
                    routineSets = listOf(
                        RoutineSet(
                            reps = 1,
                            weight = 0.0,
                            sideType = "Unilateral",
                            leftReps = 1,
                            rightReps = 1
                        )
                    )
                )
            )
        )
        viewModel.startWorkout(day, 0, "R", null)

        viewModel.toggleSetDone(0, 0) // done
        viewModel.toggleSetDone(0, 0) // L:0, R:0 → reset

        val set = viewModel.uiState.value.exercises[0].sets[0]
        Assert.assertFalse(set.isDone)
        Assert.assertEquals(1, set.leftReps)
        Assert.assertEquals(1, set.rightReps)
    }

    @Test
    fun `addSet on unilateral exercise initializes left and right reps to 0`() = runTest {
        val day = WorkoutDay(
            id = "d1", name = "Day",
            exercises = listOf(makeUnilateralExercise())
        )
        viewModel.startWorkout(day, 0, "R", null)
        val before = viewModel.uiState.value.exercises[0].sets.size

        viewModel.addSet(0)

        val sets = viewModel.uiState.value.exercises[0].sets
        Assert.assertEquals(before + 1, sets.size)
        val newSet = sets.last()
        Assert.assertEquals("Unilateral", newSet.sideType)
        Assert.assertEquals(0, newSet.leftReps)
        Assert.assertEquals(0, newSet.rightReps)
    }

    @Test
    fun `setLeftReps and setRightReps update independently`() = runTest {
        val day = WorkoutDay(
            id = "d1", name = "Day",
            exercises = listOf(makeUnilateralExercise())
        )
        viewModel.startWorkout(day, 0, "R", null)

        viewModel.setLeftReps(0, 0, 12)
        Assert.assertEquals(12, viewModel.uiState.value.exercises[0].sets[0].leftReps)
        Assert.assertEquals(10, viewModel.uiState.value.exercises[0].sets[0].rightReps)

        viewModel.setRightReps(0, 0, 14)
        Assert.assertEquals(12, viewModel.uiState.value.exercises[0].sets[0].leftReps)
        Assert.assertEquals(14, viewModel.uiState.value.exercises[0].sets[0].rightReps)
    }

    @Test
    fun `bilateral exercises behave unchanged with new sideType field`() = runTest {
        viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)

        val state = viewModel.uiState.value
        val set = state.exercises[0].sets[0]
        Assert.assertEquals("Bilateral", set.sideType)
        Assert.assertNull(set.leftReps)
        Assert.assertNull(set.rightReps)

        viewModel.toggleSetDone(0, 0)
        Assert.assertTrue(viewModel.uiState.value.exercises[0].sets[0].isDone)
    }

    @Test
    fun `startWorkout falls back to exercise sideType when RoutineSet has default Bilateral`() =
        runTest {
            val day = WorkoutDay(
                id = "d1", name = "Day",
                exercises = listOf(
                    Exercise(
                        id = "e1", name = "Bicep Curl", muscleGroup = "Arms",
                        sideType = SideType.Unilateral,
                        routineSets = listOf(
                            RoutineSet(reps = 10, weight = 15.0),
                            RoutineSet(reps = 10, weight = 15.0)
                        )
                    )
                )
            )
            viewModel.startWorkout(day, 0, "R", null)

            val set = viewModel.uiState.value.exercises[0].sets[0]
            Assert.assertEquals("Unilateral", set.sideType)
            Assert.assertEquals(0, set.leftReps)
            Assert.assertEquals(0, set.rightReps)
        }

    @Test
    fun `startWorkout uses RoutineSet sideType when explicitly set to Unilateral`() = runTest {
        val day = WorkoutDay(
            id = "d1", name = "Day",
            exercises = listOf(
                Exercise(
                    id = "e1", name = "Bicep Curl", muscleGroup = "Arms",
                    sideType = SideType.Unilateral,
                    routineSets = listOf(
                        RoutineSet(
                            reps = 10,
                            weight = 15.0,
                            sideType = "Unilateral",
                            leftReps = 10,
                            rightReps = 10
                        )
                    )
                )
            )
        )
        viewModel.startWorkout(day, 0, "R", null)

        val set = viewModel.uiState.value.exercises[0].sets[0]
        Assert.assertEquals("Unilateral", set.sideType)
        Assert.assertEquals(10, set.leftReps)
        Assert.assertEquals(10, set.rightReps)
    }

    @Test
    fun `startWorkout bilateral exercise keeps Bilateral sideType`() = runTest {
        val day = WorkoutDay(
            id = "d1", name = "Day",
            exercises = listOf(
                Exercise(
                    id = "e1", name = "Bench Press", muscleGroup = "Chest",
                    sideType = SideType.Bilateral,
                    routineSets = listOf(
                        RoutineSet(reps = 10, weight = 80.0)
                    )
                )
            )
        )
        viewModel.startWorkout(day, 0, "R", null)

        val set = viewModel.uiState.value.exercises[0].sets[0]
        Assert.assertEquals("Bilateral", set.sideType)
        Assert.assertNull(set.leftReps)
        Assert.assertNull(set.rightReps)
    }

    // endregion
}