package de.melobeat.workoutplanner.ui.feature.routines

import app.cash.turbine.test
import de.melobeat.workoutplanner.data.WorkoutRepository
import de.melobeat.workoutplanner.model.Routine
import de.melobeat.workoutplanner.model.WorkoutDay
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class RoutinesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<WorkoutRepository>(relaxed = true)
    private lateinit var viewModel: RoutinesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getRoutinesStream() } returns flowOf(emptyList())
        every { repository.getRoutineStream(any()) } returns flowOf(null)
        viewModel = RoutinesViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region uiState loading

    @Test
    fun `initial state has isLoading false after stream emits`() = runTest {
        Assert.assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `routines are populated from repository stream`() = runTest {
        val routinesFlow = MutableStateFlow<List<Routine>>(emptyList())
        every { repository.getRoutinesStream() } returns routinesFlow
        viewModel = RoutinesViewModel(repository)

        routinesFlow.value = listOf(Routine(id = "r1", name = "PPL"))

        viewModel.uiState.test {
            val state = awaitItem()
            Assert.assertEquals(1, state.routines.size)
            Assert.assertEquals("PPL", state.routines[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState updates when stream emits new routines`() = runTest {
        val routinesFlow = MutableStateFlow<List<Routine>>(emptyList())
        every { repository.getRoutinesStream() } returns routinesFlow
        viewModel = RoutinesViewModel(repository)

        viewModel.uiState.test {
            awaitItem() // empty

            routinesFlow.value = listOf(Routine(id = "r1", name = "PPL"))
            Assert.assertEquals(1, awaitItem().routines.size)

            routinesFlow.value = listOf(
                Routine(id = "r1", name = "PPL"),
                Routine(id = "r2", name = "Upper/Lower")
            )
            Assert.assertEquals(2, awaitItem().routines.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region saveRoutine / saveComplete

    @Test
    fun `saveRoutine sets saveComplete to true on success`() = runTest {
        viewModel.saveRoutine("Test Routine", "desc", emptyList(), null)

        Assert.assertTrue(viewModel.saveComplete.value)
    }

    @Test
    fun `saveComplete starts as false`() {
        Assert.assertFalse(viewModel.saveComplete.value)
    }

    @Test
    fun `onSaveHandled resets saveComplete to false`() = runTest {
        viewModel.saveRoutine("Test Routine", "desc", emptyList(), null)
        Assert.assertTrue(viewModel.saveComplete.value)

        viewModel.onSaveHandled()

        Assert.assertFalse(viewModel.saveComplete.value)
    }

    @Test
    fun `saveRoutine sets error message on failure`() = runTest {
        coEvery {
            repository.saveRoutine(any(), any(), any(), any())
        } throws RuntimeException("DB write failed")

        viewModel.saveRoutine("Test Routine", "desc", emptyList(), null)

        Assert.assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun `saveRoutine does not set saveComplete on failure`() = runTest {
        coEvery {
            repository.saveRoutine(any(), any(), any(), any())
        } throws RuntimeException("DB write failed")

        viewModel.saveRoutine("Test Routine", "desc", emptyList(), null)

        Assert.assertFalse(viewModel.saveComplete.value)
    }

    @Test
    fun `clearError clears the error message`() = runTest {
        coEvery {
            repository.saveRoutine(any(), any(), any(), any())
        } throws RuntimeException("error")
        viewModel.saveRoutine("Test", "", emptyList(), null)
        Assert.assertNotNull(viewModel.uiState.value.error)

        viewModel.clearError()

        Assert.assertNull(viewModel.uiState.value.error)
    }

    // endregion

    // region detailRoutine

    @Test
    fun `loadRoutineDetail exposes routine via detailRoutine`() = runTest {
        val routine = Routine(
            id = "r1", name = "Push Pull Legs", workoutDays = listOf(
                WorkoutDay(id = "d1", name = "Push")
            )
        )
        every { repository.getRoutineStream("r1") } returns flowOf(routine)

        viewModel.loadRoutineDetail("r1")

        viewModel.detailRoutine.test {
            Assert.assertEquals(routine, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `detailRoutine emits null before loadRoutineDetail is called`() = runTest {
        viewModel.detailRoutine.test {
            Assert.assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region deleteRoutine

    @Test
    fun `deleteRoutine sets error on failure`() = runTest {
        coEvery { repository.deleteRoutine(any()) } throws RuntimeException("Cannot delete")

        viewModel.deleteRoutine("r1")

        Assert.assertNotNull(viewModel.uiState.value.error)
    }

    // endregion
}