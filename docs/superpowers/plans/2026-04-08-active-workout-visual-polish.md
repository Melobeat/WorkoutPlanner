# Active Workout Screen Visual Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix three visual issues on the active workout screen: rest timer banner color/alignment, active set background tint, and stepper button symmetry.

**Architecture:** Pure UI changes — no ViewModel, no state, no logic. Four files are modified; all changes are color token substitutions, modifier additions, and one button type replacement. No new files created.

**Tech Stack:** Jetpack Compose, Material 3 (dynamic color), Kotlin

---

## Files Modified

| File | Change |
|---|---|
| `app/src/main/java/de/melobeat/workoutplanner/ui/RestTimerBanner.kt` | Color tokens: `surfaceVariant` → `primaryContainer`, `onSurfaceVariant` → `onPrimaryContainer`, track color fix |
| `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutScreen.kt` | Add `padding(horizontal = 16.dp)` to `RestTimerBanner` call |
| `app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseCard.kt` | Add `primaryContainer.copy(alpha = 0.25f)` background to active set `Column` |
| `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutStepperCard.kt` | Replace `Button(+)` with `FilledTonalButton`; add `weight(1f)` to both buttons |

---

## Task 1: Fix Rest Timer Banner Colors

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/RestTimerBanner.kt`

There are no unit tests for `RestTimerBanner` (it's a composable with no logic). Verification is by build only.

- [ ] **Step 1: Open `RestTimerBanner.kt` and update the `Card` container color**

Change line 56:
```kotlin
// Before
colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),

// After
colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
```

- [ ] **Step 2: Update the "REST" label text color**

Change line 72:
```kotlin
// Before
color = MaterialTheme.colorScheme.onSurfaceVariant

// After
color = MaterialTheme.colorScheme.onPrimaryContainer
```

- [ ] **Step 3: Update the timer number text color**

The `Text` at line 74–78 currently has no explicit `color` parameter (inherits default `onSurface`). Add an explicit color:
```kotlin
Text(
    text = formatElapsedTime(elapsed * 1000L),
    style = MaterialTheme.typography.displaySmall,
    fontWeight = FontWeight.Black,
    color = MaterialTheme.colorScheme.onPrimaryContainer   // ADD THIS
)
```

- [ ] **Step 4: Fix the progress bar track color**

Change line 88:
```kotlin
// Before
trackColor = MaterialTheme.colorScheme.surfaceVariant

// After
trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
```

- [ ] **Step 5: Update the milestone label text color**

Change line 96:
```kotlin
// Before
color = MaterialTheme.colorScheme.onSurfaceVariant

// After
color = MaterialTheme.colorScheme.onPrimaryContainer
```

- [ ] **Step 6: Build to verify no compile errors**

```bash
./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/RestTimerBanner.kt
git commit -m "fix: use primaryContainer for rest timer banner colors"
```

---

## Task 2: Add Horizontal Margin to Rest Timer Banner

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutScreen.kt`

- [ ] **Step 1: Add `padding(horizontal = 16.dp)` modifier to the `RestTimerBanner` call**

In `WorkoutScreen.kt`, find the `AnimatedVisibility` block around line 208–216:
```kotlin
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

Change the `RestTimerBanner` call to:
```kotlin
RestTimerBanner(
    restTimer = restTimer,
    modifier = Modifier.padding(horizontal = 16.dp)
)
```

- [ ] **Step 2: Build to verify no compile errors**

```bash
./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutScreen.kt
git commit -m "fix: align rest timer banner with screen content padding"
```

---

## Task 3: Tint Active Set Section Background

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseCard.kt`

- [ ] **Step 1: Add background tint to the active set inner Column**

In `ExerciseCard.kt`, find the active set `Column` around line 233–237:
```kotlin
Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(start = 15.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
) {
```

Add the background modifier so it reads:
```kotlin
Column(
    modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
        .padding(start = 15.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
) {
```

Note: `.background(...)` must come **before** `.padding(...)` so the tint fills the full column area including the padding zone.

- [ ] **Step 2: Build to verify no compile errors**

```bash
./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseCard.kt
git commit -m "fix: add primaryContainer tint to active set section background"
```

---

## Task 4: Symmetrize Stepper Card Buttons

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutStepperCard.kt`

- [ ] **Step 1: Replace `Button` with `FilledTonalButton` and add `weight(1f)` to both buttons**

In `WorkoutStepperCard.kt`, find the button `Row` around lines 73–88:
```kotlin
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
```

Replace with:
```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    FilledTonalButton(
        onClick = onDecrement,
        shape = CircleShape,
        modifier = Modifier.height(40.dp).weight(1f)
    ) {
        Text("−", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    FilledTonalButton(
        onClick = onIncrement,
        shape = CircleShape,
        modifier = Modifier.height(40.dp).weight(1f)
    ) {
        Text("+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
```

- [ ] **Step 2: Remove the unused `Button` import**

Find and remove this import at the top of the file (if it becomes unused):
```kotlin
import androidx.compose.material3.Button
```

Check whether `Button` is used anywhere else in the file first. If it is not, remove the import. If it is still used, leave the import.

- [ ] **Step 3: Build to verify no compile errors**

```bash
./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Run unit tests to confirm no regressions**

```bash
./gradlew :app:testDebugUnitTest
```
Expected: `BUILD SUCCESSFUL` with all tests passing. There are no tests for `StepperCard` directly, but `ActiveWorkoutViewModelTest` and `RoutinesViewModelTest` exercise related ViewModel logic.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutStepperCard.kt
git commit -m "fix: symmetrize stepper buttons with FilledTonalButton and equal weight"
```

---

## Verification

After all four tasks are complete:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Both must pass with no errors.
