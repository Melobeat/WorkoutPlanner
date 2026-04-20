# Unify Exercise Picker in Create Routine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the bare `AlertDialog`-based `ExercisePicker` in Create Routine with the existing `ExerciseSelectionDialog` (bottom sheet with search) used on the Workout screen, eliminating the visual inconsistency.

**Architecture:** `CreateRoutineScreen.kt` already calls `ExerciseSelectionDialog` is available in the same package. The change is purely a call-site swap: replace `ExercisePicker(...)` with `ExerciseSelectionDialog(...)`, passing the correct title string. `RoutineExercisePicker.kt` is then deleted since it has no other callers.

**Tech Stack:** Kotlin, Jetpack Compose, Material3 `ModalBottomSheet`

---

### Task 1: Replace `ExercisePicker` with `ExerciseSelectionDialog` in `CreateRoutineScreen.kt`

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/CreateRoutineScreen.kt:224-239`

- [ ] **Step 1: Open `CreateRoutineScreen.kt` and locate the `ExercisePicker` call block (lines 224–239)**

Current code at lines 224–239:
```kotlin
if (showExercisePickerForDayIndex != null) {
    ExercisePicker(
        exercises = availableExercises,
        onDismiss = { showExercisePickerForDayIndex = null },
        onExerciseSelected = { exercise ->
            val dayIndex = showExercisePickerForDayIndex!!
            days = days.toMutableList().apply {
                val day = this[dayIndex]
                this[dayIndex] = day.copy(exercises = day.exercises + exercise.copy(
                    routineSets = listOf(RoutineSet(10, 0.0), RoutineSet(10, 0.0), RoutineSet(10, 0.0))
                ))
            }
            showExercisePickerForDayIndex = null
        }
    )
}
```

- [ ] **Step 2: Replace it with `ExerciseSelectionDialog`**

Replace the entire block above with:
```kotlin
if (showExercisePickerForDayIndex != null) {
    ExerciseSelectionDialog(
        title = stringResource(R.string.create_routine_select_exercise),
        exercises = availableExercises,
        onDismiss = { showExercisePickerForDayIndex = null },
        onExerciseSelected = { exercise ->
            val dayIndex = showExercisePickerForDayIndex!!
            days = days.toMutableList().apply {
                val day = this[dayIndex]
                this[dayIndex] = day.copy(exercises = day.exercises + exercise.copy(
                    routineSets = listOf(RoutineSet(10, 0.0), RoutineSet(10, 0.0), RoutineSet(10, 0.0))
                ))
            }
            showExercisePickerForDayIndex = null
        }
    )
}
```

- [ ] **Step 3: Remove the `ExercisePicker` import (if present) and verify `ExerciseSelectionDialog` is resolvable**

`ExerciseSelectionDialog` is in the same package (`de.melobeat.workoutplanner.ui`) — no import needed. Remove any explicit import of `ExercisePicker` if the IDE added one.

Also remove the now-unused import of `androidx.compose.material3.TextButton` — check if it is used elsewhere in the file first. (It is used: `TextButton` for the "Save" action in the `TopAppBar` — keep it.)

- [ ] **Step 4: Build to verify no compile errors**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/CreateRoutineScreen.kt
git commit -m "feat(routines): use ExerciseSelectionDialog in create routine flow"
```

---

### Task 2: Delete `RoutineExercisePicker.kt`

**Files:**
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/RoutineExercisePicker.kt`

- [ ] **Step 1: Confirm no other callers of `ExercisePicker`**

Run:
```bash
grep -r "ExercisePicker" app/src/main/java/
```

Expected output: zero results (Task 1 already removed the only call site). If any results remain, do not delete the file — investigate first.

- [ ] **Step 2: Delete the file**

```bash
rm app/src/main/java/de/melobeat/workoutplanner/ui/RoutineExercisePicker.kt
```

- [ ] **Step 3: Build to confirm no references remain**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add -A app/src/main/java/de/melobeat/workoutplanner/ui/RoutineExercisePicker.kt
git commit -m "chore: remove unused ExercisePicker (replaced by ExerciseSelectionDialog)"
```

---

### Task 3: Update design guidelines

**Files:**
- Modify: `docs/design-guidelines.md:587`

- [ ] **Step 1: Update the Create/Edit Routine Screen section**

Find line 587:
```
- Exercise picker: `AlertDialog` (`ExercisePicker`) with flat `LazyColumn` list. (Not a bottom sheet.)
```

Replace with:
```
- Exercise picker: `ExerciseSelectionDialog` — `ModalBottomSheet` with `skipPartiallyExpanded = true`, search `OutlinedTextField`, and `LazyColumn`. Title: `R.string.create_routine_select_exercise`. Same component as the active workout add/swap flow.
```

- [ ] **Step 2: Commit**

```bash
git add docs/design-guidelines.md
git commit -m "docs: update design guidelines — exercise picker unified to ExerciseSelectionDialog"
```
