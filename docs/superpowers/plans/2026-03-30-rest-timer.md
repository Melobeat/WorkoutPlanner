# Rest Timer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an automatic rest timer that counts up between sets/exercises, fires haptic + visual milestone notifications at configurable thresholds, and exposes those thresholds in a new Timer Settings screen.

**Architecture:** Rest timer state lives inside `ActiveWorkoutViewModel` as a nullable `RestTimerUiState` on `ActiveWorkoutUiState`. Milestone events flow out via a `SharedFlow<RestTimerEvent>` which the workout screen collects to trigger vibration. Thresholds persist via DataStore and are read by a new `RestTimerPreferencesRepository`.

**Tech Stack:** Kotlin coroutines (`delay`, `SharedFlow`), Jetpack DataStore Preferences, Hilt, Compose `AnimatedVisibility`, `Vibrator` API.

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `gradle/libs.versions.toml` | Modify | Add DataStore dependency declaration |
| `app/build.gradle.kts` | Modify | Add DataStore implementation dependency |
| `data/RestTimerPreferencesRepository.kt` | **Create** | `RestTimerSettings` data class + DataStore-backed repo |
| `di/DatabaseModule.kt` | Modify | Provide `DataStore<Preferences>` + `RestTimerPreferencesRepository` |
| `ui/ActiveWorkoutViewModel.kt` | Modify | Add `RestTimerContext`, `RestTimerUiState`, `RestTimerEvent`, `restTimerEvents` SharedFlow, rest timer job, inject `RestTimerPreferencesRepository` |
| `ui/WorkoutScreen.kt` | Modify | Add `RestTimerBanner` composable + vibration `LaunchedEffect` |
| `ui/TimerSettingsScreen.kt` | **Create** | `TimerSettingsViewModel` + `TimerSettingsScreen` composable |
| `ui/SettingsScreen.kt` | Modify | Add "Timer Settings" list item + `onNavigateToTimerSettings` callback |
| `ui/navigation/NavRoutes.kt` | Modify | Add `TimerSettingsRoute` |
| `ui/navigation/WorkoutNavGraph.kt` | Modify | Wire `TimerSettingsRoute` destination + update `SettingsScreen` call |
| `test/.../RestTimerPreferencesRepositoryTest.kt` | **Create** | DataStore roundtrip tests |
| `test/.../ActiveWorkoutViewModelTest.kt` | Modify | Update constructor call + add 3 rest timer tests |

---

## Task 1: Add DataStore dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add DataStore version + library to version catalog**

In `gradle/libs.versions.toml`, add under `[versions]`:
```toml
datastore = "1.1.3"
```

Add under `[libraries]`:
```toml
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
```

- [ ] **Step 2: Add DataStore to app dependencies**

In `app/build.gradle.kts`, inside `dependencies { ... }` after the Room block:
```kotlin
// DataStore
implementation(libs.androidx.datastore.preferences)
```

- [ ] **Step 3: Verify the build resolves the dependency**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew compileDebugKotlin
```
Expected: BUILD SUCCESSFUL (no unresolved reference errors)

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add DataStore Preferences dependency"
```

---

## Task 2: RestTimerPreferencesRepository (TDD)

**Files:**
- Create: `app/src/main/java/de/melobeat/workoutplanner/data/RestTimerPreferencesRepository.kt`
- Create: `app/src/test/java/de/melobeat/workoutplanner/data/RestTimerPreferencesRepositoryTest.kt`

- [ ] **Step 1: Write failing tests**

Create `app/src/test/java/de/melobeat/workoutplanner/data/RestTimerPreferencesRepositoryTest.kt`:

```kotlin
package de.melobeat.workoutplanner.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
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
    fun `partial update preserves unchanged values`() = runTest {
        val repo = makeRepo()
        repo.update(RestTimerSettings(betweenSetsEasySeconds = 45))
        val settings = repo.settings.first()
        assertEquals(45, settings.betweenSetsEasySeconds)
        assertEquals(180, settings.betweenSetsHardSeconds) // unchanged default
        assertEquals(60, settings.betweenExercisesSeconds)  // unchanged default
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests "de.melobeat.workoutplanner.data.RestTimerPreferencesRepositoryTest"
```
Expected: FAILED — `RestTimerPreferencesRepository` and `RestTimerSettings` do not exist yet.

- [ ] **Step 3: Implement `RestTimerPreferencesRepository`**

Create `app/src/main/java/de/melobeat/workoutplanner/data/RestTimerPreferencesRepository.kt`:

```kotlin
package de.melobeat.workoutplanner.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class RestTimerSettings(
    val betweenSetsEasySeconds: Int = 90,
    val betweenSetsHardSeconds: Int = 180,
    val betweenExercisesSeconds: Int = 60
)

@Singleton
class RestTimerPreferencesRepository @Inject constructor(
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
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests "de.melobeat.workoutplanner.data.RestTimerPreferencesRepositoryTest"
```
Expected: BUILD SUCCESSFUL, 3 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/data/RestTimerPreferencesRepository.kt \
        app/src/test/java/de/melobeat/workoutplanner/data/RestTimerPreferencesRepositoryTest.kt
git commit -m "feat: add RestTimerPreferencesRepository with DataStore persistence"
```

---

## Task 3: Wire DataStore and repository into DI

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/di/DatabaseModule.kt`

- [ ] **Step 1: Add DataStore and repository providers**

Add these imports to `DatabaseModule.kt`:
```kotlin
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import de.melobeat.workoutplanner.data.RestTimerPreferencesRepository
```

Add these two `@Provides` functions inside the `DatabaseModule` object, after the existing providers:

```kotlin
@Provides
@Singleton
fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("rest_timer_prefs") }
    )

@Provides
@Singleton
fun provideRestTimerPreferencesRepository(
    dataStore: DataStore<Preferences>
): RestTimerPreferencesRepository = RestTimerPreferencesRepository(dataStore)
```

- [ ] **Step 2: Verify KSP/Hilt compiles cleanly**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/di/DatabaseModule.kt
git commit -m "feat: provide DataStore and RestTimerPreferencesRepository via Hilt"
```

---

## Task 4: Rest timer logic in ActiveWorkoutViewModel (TDD)

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModel.kt`
- Modify: `app/src/test/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModelTest.kt`

- [ ] **Step 1: Add new types to `ActiveWorkoutViewModel.kt`**

Add these declarations just above the `ActiveWorkoutUiState` data class (around line 34):

```kotlin
enum class RestTimerContext { BetweenSets, BetweenExercises }

data class RestTimerUiState(
    val elapsedSeconds: Int = 0,
    val context: RestTimerContext,
    val easyThresholdSeconds: Int,
    val hardThresholdSeconds: Int,
    val singleThresholdSeconds: Int
)

sealed class RestTimerEvent {
    object EasyMilestone : RestTimerEvent()
    object HardMilestone : RestTimerEvent()
    object ExerciseMilestone : RestTimerEvent()
}
```

Add `val restTimer: RestTimerUiState? = null` to `ActiveWorkoutUiState`:

```kotlin
data class ActiveWorkoutUiState(
    val isActive: Boolean = false,
    val isFullScreen: Boolean = false,
    val workoutDayName: String = "",
    val exercises: List<ExerciseUiState> = emptyList(),
    val elapsedTime: Long = 0L,
    val isFinished: Boolean = false,
    val showSummary: Boolean = false,
    val summaryDurationMs: Long = 0L,
    val error: String? = null,
    val currentExerciseIndex: Int = 0,
    val currentSetIndex: Int = 0,
    val restTimer: RestTimerUiState? = null
)
```

Also add these imports at the top of the file:
```kotlin
import de.melobeat.workoutplanner.data.RestTimerPreferencesRepository
import de.melobeat.workoutplanner.data.RestTimerSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
```

- [ ] **Step 2: Write failing ViewModel tests**

Update `ActiveWorkoutViewModelTest.kt` — replace the top of the class (before `startWorkout` region) with:

```kotlin
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
        every { timerPrefs.settings } returns flowOf(RestTimerSettings())
        viewModel = ActiveWorkoutViewModel(repository, timerPrefs)
    }
```

Add these three tests after the `// region skipExercise` block (before `// region helpers`):

```kotlin
// region rest timer

@Test
fun `completeCurrentSet mid-exercise starts BetweenSets rest timer`() = runTest {
    // makeWorkoutDay returns 2 exercises each with 2 sets
    viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)

    viewModel.completeCurrentSet() // set 0 → set 1, should start BetweenSets timer

    assertEquals(RestTimerContext.BetweenSets, viewModel.uiState.value.restTimer?.context)
}

@Test
fun `completeCurrentSet on last set of non-final exercise starts BetweenExercises rest timer`() = runTest {
    viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)

    viewModel.completeCurrentSet() // set 0 → set 1
    viewModel.completeCurrentSet() // set 1 (last) → exercise 1, should start BetweenExercises timer

    assertEquals(RestTimerContext.BetweenExercises, viewModel.uiState.value.restTimer?.context)
}

@Test
fun `starting a new set resets rest timer elapsed seconds to zero`() = runTest {
    viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)

    viewModel.completeCurrentSet() // rest timer starts (BetweenSets)
    // rest timer starts fresh — elapsed should be 0 since no real time has passed
    assertEquals(0, viewModel.uiState.value.restTimer?.elapsedSeconds)

    viewModel.completeCurrentSet() // new rest timer starts (BetweenExercises), also elapsed=0
    assertEquals(0, viewModel.uiState.value.restTimer?.elapsedSeconds)
}

// endregion
```

Also add these imports to the test file:
```kotlin
import de.melobeat.workoutplanner.data.RestTimerPreferencesRepository
import de.melobeat.workoutplanner.data.RestTimerSettings
```

- [ ] **Step 3: Run tests to see current failures**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests "de.melobeat.workoutplanner.ui.ActiveWorkoutViewModelTest"
```
Expected: compile error — `ActiveWorkoutViewModel` constructor still only takes `repository`.

- [ ] **Step 4: Update `ActiveWorkoutViewModel` constructor and add rest timer logic**

Replace the `@HiltViewModel` class declaration and the fields block with:

```kotlin
@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val timerPrefs: RestTimerPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    private var timerJob: Job? = null
    private var restTimerJob: Job? = null
    private var currentWorkoutDay: WorkoutDay? = null
    private var currentDayIndex: Int = 0
    private var currentRoutineName: String = ""
    private var currentRoutineId: String? = null

    private val _restTimerEvents = MutableSharedFlow<RestTimerEvent>()
    val restTimerEvents: SharedFlow<RestTimerEvent> = _restTimerEvents.asSharedFlow()
```

Add a private `startRestTimer` function before `startTimer()`:

```kotlin
private fun startRestTimer(context: RestTimerContext) {
    restTimerJob?.cancel()
    viewModelScope.launch {
        val settings = timerPrefs.settings.first()
        val restState = RestTimerUiState(
            context = context,
            easyThresholdSeconds = settings.betweenSetsEasySeconds,
            hardThresholdSeconds = settings.betweenSetsHardSeconds,
            singleThresholdSeconds = settings.betweenExercisesSeconds
        )
        _uiState.update { it.copy(restTimer = restState) }
        var seconds = 0
        var easyFired = false
        var hardFired = false
        var singleFired = false
        while (true) {
            delay(1000)
            seconds++
            _uiState.update { s -> s.copy(restTimer = s.restTimer?.copy(elapsedSeconds = seconds)) }
            when (context) {
                RestTimerContext.BetweenSets -> {
                    if (!easyFired && seconds >= restState.easyThresholdSeconds) {
                        easyFired = true
                        _restTimerEvents.emit(RestTimerEvent.EasyMilestone)
                    }
                    if (!hardFired && seconds >= restState.hardThresholdSeconds) {
                        hardFired = true
                        _restTimerEvents.emit(RestTimerEvent.HardMilestone)
                    }
                }
                RestTimerContext.BetweenExercises -> {
                    if (!singleFired && seconds >= restState.singleThresholdSeconds) {
                        singleFired = true
                        _restTimerEvents.emit(RestTimerEvent.ExerciseMilestone)
                    }
                }
            }
        }
    }.also { restTimerJob = it }
}

private fun cancelRestTimer() {
    restTimerJob?.cancel()
    restTimerJob = null
    _uiState.update { it.copy(restTimer = null) }
}
```

Replace `cancelWorkout()` with:

```kotlin
fun cancelWorkout() {
    timerJob?.cancel()
    timerJob = null
    cancelRestTimer()
    _elapsedTime.value = 0L
    currentWorkoutDay = null
    _uiState.value = ActiveWorkoutUiState()
}
```

Replace `requestFinish()` with:

```kotlin
fun requestFinish() {
    val capturedDuration = _elapsedTime.value
    timerJob?.cancel()
    timerJob = null
    cancelRestTimer()
    _uiState.update { it.copy(showSummary = true, summaryDurationMs = capturedDuration) }
}
```

Replace `completeCurrentSet()` with:

```kotlin
fun completeCurrentSet() {
    val state = _uiState.value
    val ei = state.currentExerciseIndex
    val si = state.currentSetIndex
    cancelRestTimer()
    _uiState.update { s ->
        s.copy(exercises = s.exercises.mapIndexed { eIdx, ex ->
            if (eIdx != ei) ex
            else ex.copy(sets = ex.sets.mapIndexed { sIdx, set ->
                if (sIdx == si) set.copy(isDone = true) else set
            })
        })
    }
    val exercise = _uiState.value.exercises[ei]
    when {
        si < exercise.sets.size - 1 -> {
            _uiState.update { it.copy(currentSetIndex = si + 1) }
            startRestTimer(RestTimerContext.BetweenSets)
        }
        ei < _uiState.value.exercises.size - 1 -> {
            _uiState.update { it.copy(currentExerciseIndex = ei + 1, currentSetIndex = 0) }
            startRestTimer(RestTimerContext.BetweenExercises)
        }
        else -> requestFinish()
    }
}
```

Replace `skipExercise()` with:

```kotlin
fun skipExercise() {
    val state = _uiState.value
    val ei = state.currentExerciseIndex
    cancelRestTimer()
    if (ei < state.exercises.size - 1) {
        _uiState.update { it.copy(currentExerciseIndex = ei + 1, currentSetIndex = 0) }
        startRestTimer(RestTimerContext.BetweenExercises)
    } else {
        requestFinish()
    }
}
```

- [ ] **Step 5: Run all ViewModel tests**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests "de.melobeat.workoutplanner.ui.ActiveWorkoutViewModelTest"
```
Expected: BUILD SUCCESSFUL, all tests pass (existing + 3 new).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModel.kt \
        app/src/test/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModelTest.kt
git commit -m "feat: add rest timer logic to ActiveWorkoutViewModel"
```

---

## Task 5: Rest timer banner in WorkoutScreen

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutScreen.kt`

- [ ] **Step 1: Add `RestTimerBanner` composable**

Add this composable function at the bottom of `WorkoutScreen.kt`, just before `formatElapsedTime`:

```kotlin
@Composable
fun RestTimerBanner(restTimer: RestTimerUiState, modifier: Modifier = Modifier) {
    val elapsed = restTimer.elapsedSeconds
    val milestoneLabel: String? = when (restTimer.context) {
        RestTimerContext.BetweenSets -> when {
            elapsed >= restTimer.hardThresholdSeconds -> "HARD? TIME TO GO"
            elapsed >= restTimer.easyThresholdSeconds -> "EASY? TIME TO GO"
            else -> null
        }
        RestTimerContext.BetweenExercises -> when {
            elapsed >= restTimer.singleThresholdSeconds -> "READY FOR NEXT EXERCISE?"
            else -> null
        }
    }
    val progress: Float = when (restTimer.context) {
        RestTimerContext.BetweenSets -> when {
            elapsed >= restTimer.hardThresholdSeconds -> 1f
            elapsed >= restTimer.easyThresholdSeconds -> {
                val segLen = restTimer.hardThresholdSeconds - restTimer.easyThresholdSeconds
                if (segLen == 0) 1f
                else (elapsed - restTimer.easyThresholdSeconds).toFloat() / segLen.toFloat()
            }
            else -> elapsed.toFloat() / restTimer.easyThresholdSeconds.toFloat()
        }
        RestTimerContext.BetweenExercises -> when {
            elapsed >= restTimer.singleThresholdSeconds -> 1f
            else -> elapsed.toFloat() / restTimer.singleThresholdSeconds.toFloat()
        }
    }.coerceIn(0f, 1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REST",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatElapsedTime(elapsed * 1000L),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            if (milestoneLabel != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = milestoneLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

- [ ] **Step 2: Add vibration LaunchedEffect and banner to WorkoutScreen**

In `WorkoutScreen` (the stateful composable, not `WorkoutScreenContent`), add the vibration collector after the existing `LaunchedEffect(uiState.showSummary)`:

```kotlin
val context = androidx.compose.ui.platform.LocalContext.current
val vibrator = remember { context.getSystemService(android.os.Vibrator::class.java) }
LaunchedEffect(Unit) {
    viewModel.restTimerEvents.collect {
        vibrator?.vibrate(
            android.os.VibrationEffect.createWaveform(longArrayOf(0, 50, 100, 50), -1)
        )
    }
}
```

- [ ] **Step 3: Wire banner into `WorkoutScreenContent`**

`WorkoutScreenContent` receives `restTimer: RestTimerUiState?` as a new parameter. Add it after `uiState`:

```kotlin
@Composable
fun WorkoutScreenContent(
    uiState: ActiveWorkoutUiState,
    restTimer: RestTimerUiState?,       // <-- add this
    availableExercises: List<Exercise>,
    ...
```

Pass it from `WorkoutScreen`:

```kotlin
WorkoutScreenContent(
    uiState = uiState,
    restTimer = uiState.restTimer,      // <-- add this
    ...
```

Also update the existing `WorkoutScreenContentPreview` at the bottom of the file — add `restTimer = null` as the second argument:

```kotlin
WorkoutScreenContent(
    uiState = ActiveWorkoutUiState(...),
    restTimer = null,
    availableExercises = ...,
    ...
```

Inside `WorkoutScreenContent`, locate the section after the "Done CTA" `Surface` (around the gradient pill button) and before the "Navigation row — Back and Skip" comment. Add the banner there:

```kotlin
// Rest timer banner — visible while resting after a set
Spacer(Modifier.height(8.dp))
AnimatedVisibility(
    visible = restTimer != null,
    enter = expandVertically(),
    exit = shrinkVertically()
) {
    if (restTimer != null) {
        RestTimerBanner(restTimer = restTimer)
    }
}
```

- [ ] **Step 4: Verify it builds**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutScreen.kt
git commit -m "feat: add rest timer banner and vibration to WorkoutScreen"
```

---

## Task 6: TimerSettingsScreen

**Files:**
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/TimerSettingsScreen.kt`

- [ ] **Step 1: Create `TimerSettingsScreen.kt`**

Create `app/src/main/java/de/melobeat/workoutplanner/ui/TimerSettingsScreen.kt`:

```kotlin
package de.melobeat.workoutplanner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.melobeat.workoutplanner.data.RestTimerPreferencesRepository
import de.melobeat.workoutplanner.data.RestTimerSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerSettingsViewModel @Inject constructor(
    private val timerPrefs: RestTimerPreferencesRepository
) : ViewModel() {

    val settings: StateFlow<RestTimerSettings> = timerPrefs.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RestTimerSettings())

    fun updateEasyThreshold(seconds: Int) {
        viewModelScope.launch { timerPrefs.update(settings.value.copy(betweenSetsEasySeconds = seconds)) }
    }

    fun updateHardThreshold(seconds: Int) {
        viewModelScope.launch { timerPrefs.update(settings.value.copy(betweenSetsHardSeconds = seconds)) }
    }

    fun updateExerciseThreshold(seconds: Int) {
        viewModelScope.launch { timerPrefs.update(settings.value.copy(betweenExercisesSeconds = seconds)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TimerSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Timer Settings", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TimerSettingRow(
                title = "Easy set rest",
                subtitle = "Notify when it's time for the next easy set",
                seconds = settings.betweenSetsEasySeconds,
                onValueChange = { viewModel.updateEasyThreshold(it) }
            )
            HorizontalDivider()
            TimerSettingRow(
                title = "Hard set rest",
                subtitle = "Notify when it's time for the next hard set",
                seconds = settings.betweenSetsHardSeconds,
                onValueChange = { viewModel.updateHardThreshold(it) }
            )
            HorizontalDivider()
            TimerSettingRow(
                title = "Between exercises",
                subtitle = "Notify when it's time for the next exercise",
                seconds = settings.betweenExercisesSeconds,
                onValueChange = { viewModel.updateExerciseThreshold(it) }
            )
        }
    }
}

@Composable
private fun TimerSettingRow(
    title: String,
    subtitle: String,
    seconds: Int,
    onValueChange: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Text(
                text = formatRestSeconds(seconds),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable { showDialog = true }
    )

    if (showDialog) {
        TimerEditDialog(
            title = title,
            currentSeconds = seconds,
            onDismiss = { showDialog = false },
            onConfirm = { newSeconds ->
                onValueChange(newSeconds)
                showDialog = false
            }
        )
    }
}

@Composable
private fun TimerEditDialog(
    title: String,
    currentSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var inputText by remember { mutableStateOf(currentSeconds.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("Enter duration in seconds:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it.filter { c -> c.isDigit() } },
                    singleLine = true,
                    suffix = { Text("s") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = inputText.toIntOrNull()
                if (v != null && v > 0) onConfirm(v)
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun formatRestSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "$m:${s.toString().padStart(2, '0')}" else "${s}s"
}
```

- [ ] **Step 2: Verify it compiles**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/TimerSettingsScreen.kt
git commit -m "feat: add TimerSettingsScreen with configurable rest thresholds"
```

---

## Task 7: Navigation wiring

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/navigation/NavRoutes.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/navigation/WorkoutNavGraph.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/SettingsScreen.kt`

- [ ] **Step 1: Add `TimerSettingsRoute` to `NavRoutes.kt`**

Add this line at the end of `NavRoutes.kt`:

```kotlin
@Serializable object TimerSettingsRoute
```

- [ ] **Step 2: Update `SettingsScreen` signature and add list item**

In `SettingsScreen.kt`, add `onNavigateToTimerSettings: () -> Unit` to the function signature:

```kotlin
@Composable
fun SettingsScreen(
    onNavigateToExercises: () -> Unit,
    onNavigateToRoutines: () -> Unit,
    onNavigateToEquipment: () -> Unit,
    onNavigateToTimerSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
)
```

Add the timer settings list item inside the `LazyColumn`, before the "Manage Exercises" item:

```kotlin
item {
    SettingsListItem(
        title = "Timer Settings",
        subtitle = "Rest timer durations between sets and exercises",
        icon = Icons.Outlined.Timer,
        onClick = onNavigateToTimerSettings
    )
    HorizontalDivider()
}
```

Add the import:
```kotlin
import androidx.compose.material.icons.outlined.Timer
```

Also update the `SettingsScreenPreview` to pass a no-op lambda:
```kotlin
SettingsScreen(
    onNavigateToExercises = {},
    onNavigateToRoutines = {},
    onNavigateToEquipment = {},
    onNavigateToTimerSettings = {},
    onBack = {}
)
```

- [ ] **Step 3: Wire `TimerSettingsRoute` in `WorkoutNavGraph`**

Add the import at the top of `WorkoutNavGraph.kt`:
```kotlin
import de.melobeat.workoutplanner.ui.TimerSettingsScreen
import de.melobeat.workoutplanner.ui.navigation.TimerSettingsRoute
```

Inside `navigation<SettingsGraphRoute>`, add a new destination after `composable<EquipmentRoute>`:

```kotlin
composable<TimerSettingsRoute> {
    TimerSettingsScreen(onBack = { navController.popBackStack() })
}
```

Update the `SettingsScreen` call inside `composable<SettingsRoute>`:

```kotlin
composable<SettingsRoute> {
    SettingsScreen(
        onNavigateToExercises = { navController.navigate(ExercisesRoute) },
        onNavigateToRoutines = { navController.navigate(RoutinesRoute) },
        onNavigateToEquipment = { navController.navigate(EquipmentRoute) },
        onNavigateToTimerSettings = { navController.navigate(TimerSettingsRoute) },
        onBack = { navController.popBackStack() }
    )
}
```

- [ ] **Step 4: Full build + all unit tests**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew compileDebugKotlin && \
JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest
```
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/navigation/NavRoutes.kt \
        app/src/main/java/de/melobeat/workoutplanner/ui/navigation/WorkoutNavGraph.kt \
        app/src/main/java/de/melobeat/workoutplanner/ui/SettingsScreen.kt
git commit -m "feat: wire Timer Settings screen into navigation"
```

---

## Task 8: Install and smoke test

- [ ] **Step 1: Build and install debug APK**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew installDebug
```
Expected: BUILD SUCCESSFUL, APK installed on connected device/emulator.

- [ ] **Step 2: Verify rest timer smoke test**
  - Start a workout
  - Complete a set — confirm the rest timer banner appears below the Done button with a count-up timer
  - Wait ~90s (or temporarily lower the threshold in Settings) — confirm a vibration fires and the label "EASY? TIME TO GO" appears
  - Tap the Done button to start the next set — confirm the banner disappears

- [ ] **Step 3: Verify Timer Settings smoke test**
  - Navigate to Settings → Timer Settings
  - Confirm three rows display with default values (1:30, 3:00, 1:00)
  - Tap a row, enter a new value, confirm OK saves it and the row displays the updated value
  - Start a workout and verify the new threshold is used