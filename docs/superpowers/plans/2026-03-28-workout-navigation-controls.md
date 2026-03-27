# Workout Navigation Controls Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add "← Back" and "Skip Exercise →" navigation to the active workout screen so users can revisit previous sets/exercises and skip ahead.

**Architecture:** Two new ViewModel functions handle cursor movement (`goToPreviousSet`) and exercise skipping (`skipExercise`). The UI adds a `Row` of two `FilledTonalButton` pills below the existing gradient CTA. No data-layer or summary-screen changes are needed — skipped sets already render as "Skipped" via the existing `!set.isDone` logic.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, MockK, Turbine (coroutines testing)

---

## Files

- Modify: `app/src/main/java/com/example/workoutplanner/ui/ActiveWorkoutViewModel.kt` — add `goToPreviousSet()` and `skipExercise()`
- Modify: `app/src/main/java/com/example/workoutplanner/ui/WorkoutScreen.kt` — add `onGoBack`/`onSkipExercise` callbacks and navigation button row
- Modify: `app/src/test/java/com/example/workoutplanner/ui/ActiveWorkoutViewModelTest.kt` — add tests for both new functions

---

## Task 1: `goToPreviousSet()` — ViewModel (TDD)

**Files:**
- Modify: `app/src/test/java/com/example/workoutplanner/ui/ActiveWorkoutViewModelTest.kt`
- Modify: `app/src/main/java/com/example/workoutplanner/ui/ActiveWorkoutViewModel.kt`

- [ ] **Step 1: Write the failing tests**

Add a new `// region goToPreviousSet` block at the end of `ActiveWorkoutViewModelTest`, just before the `// region helpers` block:

```kotlin
// region goToPreviousSet

@Test
fun `goToPreviousSet decrements setIndex when not on first set`() = runTest {
    viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)
    // advance to set 1 first
    viewModel.completeCurrentSet()

    viewModel.goToPreviousSet()

    assertEquals(0, viewModel.uiState.value.currentSetIndex)
    assertEquals(0, viewModel.uiState.value.currentExerciseIndex)
}

@Test
fun `goToPreviousSet wraps to last set of previous exercise when on first set`() = runTest {
    // makeWorkoutDay gives 2 exercises, each with 2 sets
    viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)
    // advance past exercise 0 entirely: complete set 0, complete set 1 → moves to exercise 1 set 0
    viewModel.completeCurrentSet() // set 0 done → set 1
    viewModel.completeCurrentSet() // set 1 done → exercise 1 set 0

    viewModel.goToPreviousSet()

    assertEquals(0, viewModel.uiState.value.currentExerciseIndex)
    assertEquals(1, viewModel.uiState.value.currentSetIndex) // last set of exercise 0 (2 sets → index 1)
}

@Test
fun `goToPreviousSet does nothing when at first set of first exercise`() = runTest {
    viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)

    viewModel.goToPreviousSet()

    assertEquals(0, viewModel.uiState.value.currentExerciseIndex)
    assertEquals(0, viewModel.uiState.value.currentSetIndex)
}

@Test
fun `goToPreviousSet does not change isDone state of any set`() = runTest {
    viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)
    viewModel.completeCurrentSet() // completes set 0, moves to set 1

    viewModel.goToPreviousSet() // back to set 0

    assertTrue(viewModel.uiState.value.exercises[0].sets[0].isDone)
}

// endregion
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest \
  --tests "com.example.workoutplanner.ui.ActiveWorkoutViewModelTest" 2>&1 | tail -20
```

Expected: `FAILED` — `goToPreviousSet` does not exist yet.

- [ ] **Step 3: Implement `goToPreviousSet()` in `ActiveWorkoutViewModel`**

Add this function anywhere after `completeCurrentSet()`:

```kotlin
fun goToPreviousSet() {
    val state = _uiState.value
    val ei = state.currentExerciseIndex
    val si = state.currentSetIndex
    when {
        si > 0 -> _uiState.update { it.copy(currentSetIndex = si - 1) }
        ei > 0 -> {
            val prevExercise = state.exercises[ei - 1]
            _uiState.update {
                it.copy(
                    currentExerciseIndex = ei - 1,
                    currentSetIndex = prevExercise.sets.size - 1
                )
            }
        }
        // else: already at start — no-op
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest \
  --tests "com.example.workoutplanner.ui.ActiveWorkoutViewModelTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` with all tests passing.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/ActiveWorkoutViewModel.kt \
        app/src/test/java/com/example/workoutplanner/ui/ActiveWorkoutViewModelTest.kt
git commit -m "feat: add goToPreviousSet to ActiveWorkoutViewModel"
```

---

## Task 2: `skipExercise()` — ViewModel (TDD)

**Files:**
- Modify: `app/src/test/java/com/example/workoutplanner/ui/ActiveWorkoutViewModelTest.kt`
- Modify: `app/src/main/java/com/example/workoutplanner/ui/ActiveWorkoutViewModel.kt`

- [ ] **Step 1: Write the failing tests**

Add a `// region skipExercise` block after the `goToPreviousSet` region and before `// region helpers`:

```kotlin
// region skipExercise

@Test
fun `skipExercise advances to next exercise and resets setIndex`() = runTest {
    viewModel.startWorkout(makeWorkoutDay(exerciseCount = 3), 0, "R", null)

    viewModel.skipExercise()

    assertEquals(1, viewModel.uiState.value.currentExerciseIndex)
    assertEquals(0, viewModel.uiState.value.currentSetIndex)
}

@Test
fun `skipExercise leaves skipped exercise sets as not done`() = runTest {
    viewModel.startWorkout(makeWorkoutDay(exerciseCount = 2), 0, "R", null)

    viewModel.skipExercise()

    val skippedSets = viewModel.uiState.value.exercises[0].sets
    assertTrue(skippedSets.all { !it.isDone })
}

@Test
fun `skipExercise on last exercise triggers showSummary`() = runTest {
    viewModel.startWorkout(makeWorkoutDay(exerciseCount = 1), 0, "R", null)

    viewModel.skipExercise()

    assertTrue(viewModel.uiState.value.showSummary)
}

// endregion
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest \
  --tests "com.example.workoutplanner.ui.ActiveWorkoutViewModelTest" 2>&1 | tail -20
```

Expected: `FAILED` — `skipExercise` does not exist yet.

- [ ] **Step 3: Implement `skipExercise()` in `ActiveWorkoutViewModel`**

Add this function directly after `goToPreviousSet()`:

```kotlin
fun skipExercise() {
    val state = _uiState.value
    val ei = state.currentExerciseIndex
    if (ei < state.exercises.size - 1) {
        _uiState.update { it.copy(currentExerciseIndex = ei + 1, currentSetIndex = 0) }
    } else {
        requestFinish()
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest \
  --tests "com.example.workoutplanner.ui.ActiveWorkoutViewModelTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` with all tests passing.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/ActiveWorkoutViewModel.kt \
        app/src/test/java/com/example/workoutplanner/ui/ActiveWorkoutViewModelTest.kt
git commit -m "feat: add skipExercise to ActiveWorkoutViewModel"
```

---

## Task 3: Navigation button row — WorkoutScreen UI

**Files:**
- Modify: `app/src/main/java/com/example/workoutplanner/ui/WorkoutScreen.kt`

- [ ] **Step 1: Add callbacks to `WorkoutScreenContent` signature**

In `WorkoutScreenContent`, add two new parameters after `onDecrementWeight`:

```kotlin
onGoBack: () -> Unit,
onSkipExercise: () -> Unit,
```

The full updated signature (showing context around the new params):

```kotlin
fun WorkoutScreenContent(
    uiState: ActiveWorkoutUiState,
    availableExercises: List<Exercise>,
    onMinimize: () -> Unit,
    onCompleteSet: () -> Unit,
    onAddExercise: (Exercise) -> Unit,
    onFinishWorkout: () -> Unit,
    onCancelWorkout: () -> Unit,
    onIncrementReps: (exerciseIndex: Int, setIndex: Int) -> Unit,
    onDecrementReps: (exerciseIndex: Int, setIndex: Int) -> Unit,
    onSwapExercise: (exerciseIndex: Int, Exercise) -> Unit,
    onIncrementWeight: (exerciseIndex: Int, setIndex: Int) -> Unit,
    onDecrementWeight: (exerciseIndex: Int, setIndex: Int) -> Unit,
    onGoBack: () -> Unit,
    onSkipExercise: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
)
```

- [ ] **Step 2: Add the navigation button row below the gradient CTA**

In `WorkoutScreenContent`, find the closing `}` of the gradient CTA `Surface` block (the one containing `"✓  Done — Set X"`). Immediately after it, replace the existing `Spacer(Modifier.height(12.dp))` with the navigation row followed by a spacer:

```kotlin
// Navigation row — Back and Skip
val isAtStart = ei == 0 && si == 0
Row(
    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween
) {
    FilledTonalButton(
        onClick = onGoBack,
        enabled = !isAtStart,
        shape = CircleShape
    ) {
        Text("← Back", style = MaterialTheme.typography.labelMedium)
    }
    FilledTonalButton(
        onClick = onSkipExercise,
        shape = CircleShape
    ) {
        Text("Skip Exercise →", style = MaterialTheme.typography.labelMedium)
    }
}

Spacer(Modifier.height(12.dp))
```

- [ ] **Step 3: Wire the callbacks in `WorkoutScreen`**

In `WorkoutScreen`, update the `WorkoutScreenContent(...)` call to pass the new lambdas:

```kotlin
WorkoutScreenContent(
    uiState = uiState,
    availableExercises = exerciseLibState.exercises,
    onMinimize = { viewModel.setFullScreen(false); onNavigateBack() },
    onCompleteSet = { viewModel.completeCurrentSet() },
    onAddExercise = { viewModel.addExercise(it) },
    onSwapExercise = { exerciseIndex, exercise -> viewModel.swapExercise(exerciseIndex, exercise) },
    onFinishWorkout = { viewModel.requestFinish() },
    onCancelWorkout = { viewModel.cancelWorkout(); onNavigateBack() },
    onIncrementReps = { ei, si -> viewModel.incrementReps(ei, si) },
    onDecrementReps = { ei, si -> viewModel.decrementReps(ei, si) },
    onIncrementWeight = { ei, si -> viewModel.incrementWeight(ei, si) },
    onDecrementWeight = { ei, si -> viewModel.decrementWeight(ei, si) },
    onGoBack = { viewModel.goToPreviousSet() },
    onSkipExercise = { viewModel.skipExercise() },
    onNavigateBack = onNavigateBack,
    modifier = modifier
)
```

- [ ] **Step 4: Update the `WorkoutScreenContentPreview`**

Add the two new no-op lambdas to the preview call:

```kotlin
WorkoutScreenContent(
    // ... existing params ...
    onGoBack = {},
    onSkipExercise = {},
    // ...
)
```

- [ ] **Step 5: Verify the build compiles**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/WorkoutScreen.kt
git commit -m "feat: add Back and Skip Exercise navigation buttons to workout screen"
```