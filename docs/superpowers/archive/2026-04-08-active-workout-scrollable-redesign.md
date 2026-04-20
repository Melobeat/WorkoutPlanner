# Active Workout Screen — Scrollable Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the one-exercise-at-a-time workout screen with a scrollable `LazyColumn` showing all exercises and sets at once, where any set can be tapped to activate it.

**Architecture:** Add `jumpToSet()` and `toggleExerciseExpanded()` to `ActiveWorkoutViewModel`, auto-collapse completed exercises inside `completeCurrentSet()`. Replace `WorkoutExerciseContent` with a new `ExerciseCard` composable. Rewrite `WorkoutScreenContent` to use a `LazyColumn` with a sticky `RestTimerBanner` slot above it. Add AMRAP styling to `StepperCard`.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt, `LazyColumn` / `LazyListState`, `AnimatedVisibility`, `UnconfinedTestDispatcher` + MockK for tests.

**Spec:** `docs/superpowers/specs/2026-04-08-active-workout-scrollable-redesign.md`

---

## File Map

| Action | File | Purpose |
|---|---|---|
| Modify | `ui/ActiveWorkoutViewModel.kt` | Add `jumpToSet()`, `toggleExerciseExpanded()` (already exists — verify), auto-collapse in `completeCurrentSet()` |
| Modify | `ui/WorkoutStepperCard.kt` | Add `isAmrap: Boolean` parameter with `tertiaryContainer` styling |
| Delete | `ui/WorkoutExerciseContent.kt` | Replaced entirely by `ExerciseCard` |
| Create | `ui/ExerciseCard.kt` | New composable: all card states + set rows + active set with steppers |
| Modify | `ui/WorkoutScreen.kt` | Replace `Column`+`WorkoutExerciseContent` with `LazyColumn`+`ExerciseCard`; move `RestTimerBanner` above the list |
| Modify | `app/src/test/java/.../ActiveWorkoutViewModelTest.kt` | Add tests for `jumpToSet()` and auto-collapse |

---

## Task 1: Add `jumpToSet()` to ViewModel + auto-collapse in `completeCurrentSet()`

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModel.kt`
- Test: `app/src/test/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModelTest.kt`

- [ ] **Step 1: Write failing tests**

Add these tests to `ActiveWorkoutViewModelTest.kt` inside a new `// region jumpToSet` block, after the last existing region:

```kotlin
// region jumpToSet

@Test
fun `jumpToSet moves cursor to given exercise and set`() = runTest {
    viewModel.startWorkout(makeWorkoutDay(exerciseCount = 3), 0, "R", "r1")

    viewModel.jumpToSet(exerciseIndex = 2, setIndex = 1)

    val state = viewModel.uiState.value
    assertEquals(2, state.currentExerciseIndex)
    assertEquals(1, state.currentSetIndex)
}

@Test
fun `jumpToSet expands the target exercise`() = runTest {
    viewModel.startWorkout(makeWorkoutDay(exerciseCount = 3), 0, "R", "r1")
    // collapse exercise 2 first
    viewModel.toggleExerciseExpanded(2)
    assertFalse(viewModel.uiState.value.exercises[2].isExpanded)

    viewModel.jumpToSet(exerciseIndex = 2, setIndex = 0)

    assertTrue(viewModel.uiState.value.exercises[2].isExpanded)
}

@Test
fun `jumpToSet does not modify isDone on any set`() = runTest {
    viewModel.startWorkout(makeWorkoutDay(exerciseCount = 2), 0, "R", "r1")
    // complete set 0 of exercise 0
    viewModel.completeCurrentSet()
    assertTrue(viewModel.uiState.value.exercises[0].sets[0].isDone)

    viewModel.jumpToSet(exerciseIndex = 1, setIndex = 0)

    // set 0 of exercise 0 still done
    assertTrue(viewModel.uiState.value.exercises[0].sets[0].isDone)
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
    assertTrue(viewModel.uiState.value.exercises[0].isExpanded)

    // Complete the only set of exercise 0 — should auto-collapse it
    viewModel.completeCurrentSet()

    assertFalse(viewModel.uiState.value.exercises[0].isExpanded)
}

// endregion
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "de.melobeat.workoutplanner.ui.ActiveWorkoutViewModelTest.jumpToSet*" --tests "de.melobeat.workoutplanner.ui.ActiveWorkoutViewModelTest.completeCurrentSet auto-collapses*" 2>&1 | tail -20
```

Expected: failures — `jumpToSet` does not exist yet.

- [ ] **Step 3: Add `jumpToSet()` to `ActiveWorkoutViewModel`**

Add after the existing `toggleExerciseExpanded()` method (line 272):

```kotlin
fun jumpToSet(exerciseIndex: Int, setIndex: Int) {
    _uiState.update { state ->
        state.copy(
            currentExerciseIndex = exerciseIndex,
            currentSetIndex = setIndex,
            exercises = state.exercises.mapIndexed { i, ex ->
                if (i == exerciseIndex) ex.copy(isExpanded = true) else ex
            }
        )
    }
}
```

- [ ] **Step 4: Add auto-collapse to `completeCurrentSet()`**

In `ActiveWorkoutViewModel.kt`, find the block inside `completeCurrentSet()` that handles advancing to the next exercise (around line 435):

```kotlin
ei < _uiState.value.exercises.size - 1 -> {
    _uiState.update { it.copy(currentExerciseIndex = ei + 1, currentSetIndex = 0) }
    startRestTimer(RestTimerContext.BetweenExercises)
}
```

Replace it with:

```kotlin
ei < _uiState.value.exercises.size - 1 -> {
    _uiState.update { state ->
        state.copy(
            currentExerciseIndex = ei + 1,
            currentSetIndex = 0,
            exercises = state.exercises.mapIndexed { i, ex ->
                if (i == ei) ex.copy(isExpanded = false) else ex
            }
        )
    }
    startRestTimer(RestTimerContext.BetweenExercises)
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "de.melobeat.workoutplanner.ui.ActiveWorkoutViewModelTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` with all tests in the class passing.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModel.kt \
        app/src/test/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModelTest.kt
git commit -m "feat: add jumpToSet and auto-collapse completed exercises in workout VM"
```

---

## Task 2: Add AMRAP styling to `StepperCard`

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutStepperCard.kt`

No new tests needed — this is a pure visual parameter change with no logic.

- [ ] **Step 1: Add `isAmrap` parameter and conditional colours**

Replace the entire contents of `WorkoutStepperCard.kt` with:

```kotlin
package de.melobeat.workoutplanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StepperCard(
    label: String,
    value: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    isAmrap: Boolean = false
) {
    val containerColor = if (isAmrap)
        MaterialTheme.colorScheme.tertiaryContainer
    else
        MaterialTheme.colorScheme.surface

    val contentColor = if (isAmrap)
        MaterialTheme.colorScheme.onTertiaryContainer
    else
        MaterialTheme.colorScheme.onSurface

    val displayLabel = if (isAmrap) "AMRAP" else label.uppercase()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                displayLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isAmrap)
                    MaterialTheme.colorScheme.onTertiaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = contentColor
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onDecrement,
                    shape = CircleShape,
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("−", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onIncrement,
                    shape = CircleShape,
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
```

- [ ] **Step 2: Build to verify no compile errors**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutStepperCard.kt
git commit -m "feat: add isAmrap styling to StepperCard (tertiaryContainer bg, AMRAP label)"
```

---

## Task 3: Create `ExerciseCard` composable

**Files:**
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseCard.kt`

This is the core new composable. It renders a single exercise as a card with all its set rows.

- [ ] **Step 1: Create `ExerciseCard.kt`**

```kotlin
package de.melobeat.workoutplanner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.melobeat.workoutplanner.ui.theme.Pink40
import de.melobeat.workoutplanner.ui.theme.Purple40

@Composable
fun ExerciseCard(
    exercise: ExerciseUiState,
    exerciseIndex: Int,
    totalExercises: Int,
    currentExerciseIndex: Int,
    currentSetIndex: Int,
    onActivateSet: (setIndex: Int) -> Unit,
    onToggleExpanded: () -> Unit,
    onSwapExercise: () -> Unit,
    onIncrementReps: (setIndex: Int) -> Unit,
    onDecrementReps: (setIndex: Int) -> Unit,
    onIncrementWeight: (setIndex: Int) -> Unit,
    onDecrementWeight: (setIndex: Int) -> Unit,
    onCompleteSet: () -> Unit,
    onGoBack: () -> Unit,
    onSkipExercise: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = exerciseIndex == currentExerciseIndex
    val allDone = exercise.sets.isNotEmpty() && exercise.sets.all { it.isDone }
    val hasAnyDone = exercise.sets.any { it.isDone }
    val isIncompleteAway = !isActive && hasAnyDone && !allDone
    val isUpNext = !isActive && !allDone && !isIncompleteAway &&
            exerciseIndex == currentExerciseIndex + 1

    val borderColor = when {
        isActive -> MaterialTheme.colorScheme.primary
        isIncompleteAway -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val cardAlpha = if (allDone) 0.55f else 1f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (allDone || isIncompleteAway) Modifier.clickable { onToggleExpanded() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = cardAlpha))
    ) {
        Column(modifier = Modifier.then(
            if (allDone) Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = cardAlpha))
            else Modifier
        )) {

            // ── Header row ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (allDone) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Column {
                        if (isActive) {
                            Text(
                                "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (isUpNext) {
                            Text(
                                "UP NEXT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            exercise.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        // Subtitle for collapsed states
                        if (!exercise.isExpanded) {
                            val subtitle = when {
                                allDone -> "${exercise.sets.size} sets done"
                                isIncompleteAway -> {
                                    val doneCount = exercise.sets.count { it.isDone }
                                    "Set $doneCount done · Set ${doneCount + 1} incomplete"
                                }
                                else -> {
                                    val first = exercise.sets.firstOrNull()
                                    "${exercise.sets.size} sets · ${first?.reps ?: "0"} reps · ${first?.weight ?: "0"} kg"
                                }
                            }
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isActive) {
                        FilledTonalButton(onClick = onSwapExercise, shape = CircleShape) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Swap", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(onClick = onToggleExpanded) {
                        Icon(
                            if (exercise.isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (exercise.isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Expanded body ───────────────────────────────────────────────
            AnimatedVisibility(
                visible = exercise.isExpanded,
                enter = expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = shrinkVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            ) {
                Column {
                    exercise.sets.forEachIndexed { si, set ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        val isActiveSet = isActive && si == currentSetIndex

                        when {
                            set.isDone -> {
                                // Completed set row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onActivateSet(si) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Set ${si + 1}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(0.6f).then(Modifier.then(
                                            if (allDone) Modifier else Modifier
                                        ))
                                    )
                                    Text(
                                        "${set.reps} × ${set.weight} kg",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = "Done",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            isActiveSet -> {
                                // Active set — expanded with steppers + CTA
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .padding(start = 3.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    // Left accent border via Row
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(1.dp) // stretches with content via parent
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                "SET ${si + 1} — ACTIVE",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(Modifier.height(10.dp))
                                            // Stepper cards
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                StepperCard(
                                                    label = "Reps",
                                                    value = set.reps,
                                                    onIncrement = { onIncrementReps(si) },
                                                    onDecrement = { onDecrementReps(si) },
                                                    isAmrap = set.isAmrap,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                StepperCard(
                                                    label = "kg",
                                                    value = set.weight,
                                                    onIncrement = { onIncrementWeight(si) },
                                                    onDecrement = { onDecrementWeight(si) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            Spacer(Modifier.height(12.dp))
                                            // Done CTA
                                            val isLastSet = si == exercise.sets.size - 1
                                            val isLastExercise = exerciseIndex == totalExercises - 1
                                            val ctaLabel = when {
                                                isLastSet && isLastExercise -> "✓  Finish Workout"
                                                isLastSet -> "✓  Next Exercise"
                                                else -> "✓  Done — Set ${si + 2}"
                                            }
                                            Surface(
                                                onClick = onCompleteSet,
                                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                                shape = RoundedCornerShape(50),
                                                color = Color.Transparent
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(RoundedCornerShape(50))
                                                        .background(Brush.linearGradient(listOf(Purple40, Pink40))),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        ctaLabel,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            // Back / Skip row
                                            val isAtStart = exerciseIndex == 0 && si == 0
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
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
                                        }
                                    }
                                }
                            }

                            else -> {
                                // Pending set row — tappable to jump
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onActivateSet(si) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Set ${si + 1}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.weight(0.6f)
                                    )
                                    Text(
                                        "${set.reps} × ${set.weight} kg",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Hint text for non-active expanded upcoming cards
                    if (!isActive && !allDone) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Text(
                            "Tap any set to jump to it",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Build to verify no compile errors**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseCard.kt
git commit -m "feat: add ExerciseCard composable for scrollable workout list"
```

---

## Task 4: Rewrite `WorkoutScreenContent` to use `LazyColumn`

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutScreen.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutExerciseContent.kt`

- [ ] **Step 1: Replace `WorkoutScreenContent` in `WorkoutScreen.kt`**

Replace the entire `WorkoutScreenContent` composable and its `@Preview` (lines 102–339 of the original file) with the following. Leave the `WorkoutScreen` stateful composable (lines 53–100) untouched:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreenContent(
    uiState: ActiveWorkoutUiState,
    restTimer: RestTimerUiState?,
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
    onJumpToSet: (exerciseIndex: Int, setIndex: Int) -> Unit,
    onToggleExerciseExpanded: (exerciseIndex: Int) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showSwapExerciseIndex by remember { mutableStateOf<Int?>(null) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val exercises = uiState.exercises
    val ei = uiState.currentExerciseIndex
    val si = uiState.currentSetIndex

    val progress = if (exercises.isEmpty()) 0f
    else {
        val currentExercise = exercises.getOrNull(ei)
        (ei.toFloat() + (si.toFloat() / (currentExercise?.sets?.size?.toFloat() ?: 1f))) / exercises.size
    }

    val listState = rememberLazyListState()
    LaunchedEffect(ei) {
        if (exercises.isNotEmpty()) {
            listState.animateScrollToItem(ei.coerceIn(0, exercises.size - 1))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.workoutDayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = formatElapsedTime(uiState.elapsedTime),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMinimize) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Minimize")
                    }
                },
                actions = {
                    FilledTonalButton(onClick = { showAddExerciseDialog = true }, shape = CircleShape) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Exercise", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.width(4.dp))
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Finish Workout") },
                                onClick = { showMenu = false; onFinishWorkout() }
                            )
                            DropdownMenuItem(
                                text = { Text("Cancel Workout", color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; showCancelDialog = true }
                            )
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Rest timer banner — sticky below TopAppBar
            AnimatedVisibility(
                visible = restTimer != null,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                if (restTimer != null) {
                    RestTimerBanner(restTimer = restTimer)
                }
            }

            // Progress row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXERCISE ${(ei + 1).coerceAtLeast(1)} OF ${exercises.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(CircleShape)
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            if (exercises.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No exercises in this workout.")
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(exercises) { exerciseIndex, exercise ->
                        ExerciseCard(
                            exercise = exercise,
                            exerciseIndex = exerciseIndex,
                            totalExercises = exercises.size,
                            currentExerciseIndex = ei,
                            currentSetIndex = si,
                            onActivateSet = { setIndex -> onJumpToSet(exerciseIndex, setIndex) },
                            onToggleExpanded = { onToggleExerciseExpanded(exerciseIndex) },
                            onSwapExercise = { showSwapExerciseIndex = exerciseIndex },
                            onIncrementReps = { setIndex -> onIncrementReps(exerciseIndex, setIndex) },
                            onDecrementReps = { setIndex -> onDecrementReps(exerciseIndex, setIndex) },
                            onIncrementWeight = { setIndex -> onIncrementWeight(exerciseIndex, setIndex) },
                            onDecrementWeight = { setIndex -> onDecrementWeight(exerciseIndex, setIndex) },
                            onCompleteSet = onCompleteSet,
                            onGoBack = onGoBack,
                            onSkipExercise = onSkipExercise
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddExerciseDialog) {
        ExerciseSelectionDialog(
            title = "Add Exercise",
            exercises = availableExercises,
            onDismiss = { showAddExerciseDialog = false },
            onExerciseSelected = { exercise ->
                onAddExercise(exercise)
                showAddExerciseDialog = false
            }
        )
    }

    showSwapExerciseIndex?.let { swapIndex ->
        ExerciseSelectionDialog(
            title = "Swap Exercise",
            exercises = availableExercises,
            onDismiss = { showSwapExerciseIndex = null },
            onExerciseSelected = { exercise ->
                onSwapExercise(swapIndex, exercise)
                showSwapExerciseIndex = null
            }
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Workout?") },
            text = { Text("All progress in this session will be lost.") },
            confirmButton = {
                TextButton(onClick = { onCancelWorkout() }) {
                    Text("Cancel Workout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Keep Going") }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WorkoutScreenContentPreview() {
    WorkoutPlannerTheme {
        WorkoutScreenContent(
            uiState = ActiveWorkoutUiState(
                isActive = true,
                workoutDayName = "Push Day",
                elapsedTime = 1_230_000L,
                isFinished = false,
                currentExerciseIndex = 0,
                currentSetIndex = 1,
                exercises = listOf(
                    ExerciseUiState(
                        exerciseId = "e1",
                        name = "Bench Press",
                        sets = listOf(
                            SetUiState(index = 0, weight = "60", reps = "10", isAmrap = false, isDone = true, originalReps = "10"),
                            SetUiState(index = 1, weight = "60", reps = "10", isAmrap = false, isDone = false, originalReps = "10"),
                            SetUiState(index = 2, weight = "60", reps = "8", isAmrap = true, isDone = false, originalReps = "8")
                        )
                    ),
                    ExerciseUiState(
                        exerciseId = "e2",
                        name = "Overhead Press",
                        sets = listOf(
                            SetUiState(index = 0, weight = "40", reps = "8", isAmrap = false, isDone = false, originalReps = "8")
                        )
                    )
                )
            ),
            restTimer = null,
            availableExercises = listOf(Exercise(id = "e3", name = "Incline Press", muscleGroup = "Chest")),
            onMinimize = {},
            onCompleteSet = {},
            onAddExercise = {},
            onSwapExercise = { _, _ -> },
            onFinishWorkout = {},
            onCancelWorkout = {},
            onIncrementReps = { _, _ -> },
            onDecrementReps = { _, _ -> },
            onIncrementWeight = { _, _ -> },
            onDecrementWeight = { _, _ -> },
            onGoBack = {},
            onSkipExercise = {},
            onJumpToSet = { _, _ -> },
            onToggleExerciseExpanded = {},
            onNavigateBack = {}
        )
    }
}
```

- [ ] **Step 2: Update the `WorkoutScreen` stateful composable to pass new lambdas**

In `WorkoutScreen.kt`, update the `WorkoutScreenContent(...)` call inside `WorkoutScreen` to add the two new parameters. Find the existing call (around line 81) and add these two lines before `modifier = modifier`:

```kotlin
onJumpToSet = { exerciseIndex, setIndex -> viewModel.jumpToSet(exerciseIndex, setIndex) },
onToggleExerciseExpanded = { exerciseIndex -> viewModel.toggleExerciseExpanded(exerciseIndex) },
```

- [ ] **Step 3: Add missing imports to `WorkoutScreen.kt`**

Ensure these imports are present at the top of `WorkoutScreen.kt` (add any that are missing):

```kotlin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberLazyListState
```

- [ ] **Step 4: Build to verify no compile errors**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL`. Fix any import errors (the build output will name the exact missing imports).

- [ ] **Step 5: Delete `WorkoutExerciseContent.kt`**

```bash
git rm app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutExerciseContent.kt
```

- [ ] **Step 6: Build again to confirm deletion causes no errors**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Run all unit tests**

```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` — all existing tests still pass.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutScreen.kt
git commit -m "feat: rewrite WorkoutScreenContent with LazyColumn and ExerciseCard"
```

---

## Task 5: Verify with lint and final build

**Files:** no changes — verification only.

- [ ] **Step 1: Run lint**

```bash
./gradlew :app:lintDebug 2>&1 | grep -E "^(Error|Warning|BUILD)" | head -30
```

Expected: `BUILD SUCCESSFUL`. Investigate any errors (warnings about unused imports from deleted file are acceptable).

- [ ] **Step 2: Run full test suite**

```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Final commit (if lint produced auto-fixable changes)**

Only commit if lint made changes:

```bash
git status
# If any files modified:
git add -u
git commit -m "chore: fix lint warnings after workout screen redesign"
```

---

## Self-Review

**Spec coverage check:**

| Spec section | Covered by |
|---|---|
| §3 Screen layout (LazyColumn, RestTimerBanner slot, progress bar) | Task 4 |
| §4 ExerciseCard states (active/incomplete/upcoming/completed) | Task 3 |
| §4a Card header row | Task 3 |
| §4b Collapsed body variants | Task 3 |
| §4c Expanded body — completed/active/pending set rows | Task 3 |
| §5 StepperCard AMRAP variant | Task 2 |
| §6 `jumpToSet()` + auto-collapse in `completeCurrentSet()` | Task 1 |
| §6 `toggleExerciseExpanded()` | Already exists in VM; wired in Task 4 |
| §6 `isExpanded = true` on `jumpToSet` target | Task 1 |
| §7 `WorkoutExerciseContent.kt` deleted | Task 4 step 5 |
| §8 `RestTimerBanner` moved above `LazyColumn` | Task 4 |
| §8 Auto-scroll on cursor change | Task 4 (`LaunchedEffect(ei)`) |
| §9 Design constraints (CircleShape, surfaceVariant, Icons.Rounded) | Task 3 |
| §10 Unchanged: existing VM methods, summary screen, mini-bar | Not touched |

All sections covered. No gaps.
