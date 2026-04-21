# UI File Restructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure the flat `ui/` directory into feature-scoped subdirectories with shared composables in `ui/common/`.

**Architecture:** Move files to feature folders (`ui/feature/home/`, `ui/feature/workout/`, etc.), shared composables to `ui/common/`, update all cross-package imports, and move test files to matching packages. No behavioral changes — pure file reorganization.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Room, Gradle

---

### Task 1: Move shared composables to `ui/common/`

**Files:**
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/common/ExerciseCard.kt`
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/common/ExerciseSelectionDialog.kt`
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/common/RestTimerBanner.kt`
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/common/RoutineDayCard.kt`
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/common/RoutineExerciseEditItem.kt`
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/common/WorkoutStepperCard.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseCard.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseSelectionDialog.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/RestTimerBanner.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/RoutineDayCard.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/RoutineExerciseEditItem.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutStepperCard.kt`

- [ ] **Step 1: Create `ui/common/` directory and move 6 files**

Run:
```bash
mkdir -p app/src/main/java/de/melobeat/workoutplanner/ui/common
git mv app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseCard.kt app/src/main/java/de/melobeat/workoutplanner/ui/common/ExerciseCard.kt
git mv app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseSelectionDialog.kt app/src/main/java/de/melobeat/workoutplanner/ui/common/ExerciseSelectionDialog.kt
git mv app/src/main/java/de/melobeat/workoutplanner/ui/RestTimerBanner.kt app/src/main/java/de/melobeat/workoutplanner/ui/common/RestTimerBanner.kt
git mv app/src/main/java/de/melobeat/workoutplanner/ui/RoutineDayCard.kt app/src/main/java/de/melobeat/workoutplanner/ui/common/RoutineDayCard.kt
git mv app/src/main/java/de/melobeat/workoutplanner/ui/RoutineExerciseEditItem.kt app/src/main/java/de/melobeat/workoutplanner/ui/common/RoutineExerciseEditItem.kt
git mv app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutStepperCard.kt app/src/main/java/de/melobeat/workoutplanner/ui/common/WorkoutStepperCard.kt
```

- [ ] **Step 2: Update package declarations in all 6 moved files**

Change the package declaration at the top of each file from:
```kotlin
package de.melobeat.workoutplanner.ui
```
to:
```kotlin
package de.melobeat.workoutplanner.ui.common
```

Affected files:
- `ui/common/ExerciseCard.kt`
- `ui/common/ExerciseSelectionDialog.kt`
- `ui/common/RestTimerBanner.kt`
- `ui/common/RoutineDayCard.kt`
- `ui/common/RoutineExerciseEditItem.kt`
- `ui/common/WorkoutStepperCard.kt`

- [ ] **Step 3: Update import statements in files that reference moved composables**

Files that need new imports (they call composables now in `ui.common`):

**`ui/WorkoutScreen.kt`** — add after existing imports:
```kotlin
import de.melobeat.workoutplanner.ui.common.ExerciseCard
import de.melobeat.workoutplanner.ui.common.RestTimerBanner
import de.melobeat.workoutplanner.ui.common.ExerciseSelectionDialog
```

**`ui/CreateRoutineScreen.kt`** — add after existing imports:
```kotlin
import de.melobeat.workoutplanner.ui.common.ExerciseSelectionDialog
```

Note: `RoutineDayCard.kt` calls `ExerciseEditItem()` and `ExerciseCard.kt` calls `StepperCard()` — these are now in the same `ui.common` package, so no imports needed.

- [ ] **Step 4: Verify build compiles**

Run:
```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/common/
git add app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutScreen.kt
git add app/src/main/java/de/melobeat/workoutplanner/ui/CreateRoutineScreen.kt
git commit -m "refactor: move shared composables to ui/common/"
```

---

### Task 2: Move `ui/feature/workout/` files

**Files:**
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/feature/workout/WorkoutScreen.kt`
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/feature/workout/WorkoutSummaryScreen.kt`
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/feature/workout/ActiveWorkoutViewModel.kt`
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/feature/workout/WorkoutUiState.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutScreen.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutSummaryScreen.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModel.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutUiState.kt`

- [ ] **Step 1: Move 4 files to `ui/feature/workout/`**

Run:
```bash
mkdir -p app/src/main/java/de/melobeat/workoutplanner/ui/feature/workout
git mv app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutScreen.kt app/src/main/java/de/melobeat/workoutplanner/ui/feature/workout/WorkoutScreen.kt
git mv app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutSummaryScreen.kt app/src/main/java/de/melobeat/workoutplanner/ui/feature/workout/WorkoutSummaryScreen.kt
git mv app/src/main/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModel.kt app/src/main/java/de/melobeat/workoutplanner/ui/feature/workout/ActiveWorkoutViewModel.kt
git mv app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutUiState.kt app/src/main/java/de/melobeat/workoutplanner/ui/feature/workout/WorkoutUiState.kt
```

- [ ] **Step 2: Update package declarations**

Change from `package de.melobeat.workoutplanner.ui` to `package de.melobeat.workoutplanner.ui.feature.workout` in all 4 files.

- [ ] **Step 3: Update imports in `ui/feature/workout/WorkoutScreen.kt`**

The file already has imports from Task 1 (`ui.common.*`). Add these new imports for the common package types it uses:

```kotlin
import de.melobeat.workoutplanner.ui.common.ExerciseCard
import de.melobeat.workoutplanner.ui.common.RestTimerBanner
import de.melobeat.workoutplanner.ui.common.ExerciseSelectionDialog
```

Also add imports for types from `WorkoutUiState.kt` (now in same package — no import needed for `ActiveWorkoutUiState`, `ExerciseUiState`, `SetUiState`, `RestTimerUiState`, `RestTimerContext`, `RestTimerEvent`).

- [ ] **Step 4: Update imports in `ui/feature/workout/ActiveWorkoutViewModel.kt`**

Add import for `ExerciseHistory` (defined in `WorkoutUiState.kt`, now same package — no import needed).

Add imports for data layer types:
```kotlin
import de.melobeat.workoutplanner.data.WorkoutRepository
import de.melobeat.workoutplanner.data.RestTimerPreferencesRepository
import de.melobeat.workoutplanner.model.WorkoutDay
import de.melobeat.workoutplanner.model.Exercise
import de.melobeat.workoutplanner.model.RoutineSet
```
(These should already exist — verify they are correct.)

- [ ] **Step 5: Update imports in `ui/navigation/WorkoutNavGraph.kt`**

Replace:
```kotlin
import de.melobeat.workoutplanner.ui.ActiveWorkoutViewModel
import de.melobeat.workoutplanner.ui.WorkoutScreen
import de.melobeat.workoutplanner.ui.WorkoutSummaryScreen
```
With:
```kotlin
import de.melobeat.workoutplanner.ui.feature.workout.ActiveWorkoutViewModel
import de.melobeat.workoutplanner.ui.feature.workout.WorkoutScreen
import de.melobeat.workoutplanner.ui.feature.workout.WorkoutSummaryScreen
```

- [ ] **Step 6: Update imports in `MainActivity.kt`**

Replace:
```kotlin
import de.melobeat.workoutplanner.ui.ActiveWorkoutViewModel
```
With:
```kotlin
import de.melobeat.workoutplanner.ui.feature.workout.ActiveWorkoutViewModel
```

- [ ] **Step 7: Update imports in `data/WorkoutRepository.kt`**

Replace:
```kotlin
import de.melobeat.workoutplanner.ui.ExerciseHistory
```
With:
```kotlin
import de.melobeat.workoutplanner.ui.feature.workout.ExerciseHistory
```

- [ ] **Step 8: Verify build compiles**

Run:
```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/feature/workout/
git add app/src/main/java/de/melobeat/workoutplanner/ui/navigation/WorkoutNavGraph.kt
git add app/src/main/java/de/melobeat/workoutplanner/MainActivity.kt
git add app/src/main/java/de/melobeat/workoutplanner/data/WorkoutRepository.kt
git commit -m "refactor: move workout feature files to ui/feature/workout/"
```

---

### Task 3: Move `ui/feature/history/` files

**Files:**
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/feature/history/HistoryScreen.kt`
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/feature/history/HistoryViewModel.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/HistoryScreen.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/HistoryViewModel.kt`

- [ ] **Step 1: Move 2 files**

Run:
```bash
mkdir -p app/src/main/java/de/melobeat/workoutplanner/ui/feature/history
git mv app/src/main/java/de/melobeat/workoutplanner/ui/HistoryScreen.kt app/src/main/java/de/melobeat/workoutplanner/ui/feature/history/HistoryScreen.kt
git mv app/src/main/java/de/melobeat/workoutplanner/ui/HistoryViewModel.kt app/src/main/java/de/melobeat/workoutplanner/ui/feature/history/HistoryViewModel.kt
```

- [ ] **Step 2: Update package declarations**

Change from `package de.melobeat.workoutplanner.ui` to `package de.melobeat.workoutplanner.ui.feature.history` in both files.

- [ ] **Step 3: Update imports in `ui/navigation/WorkoutNavGraph.kt`**

Replace:
```kotlin
import de.melobeat.workoutplanner.ui.HistoryScreen
```
With:
```kotlin
import de.melobeat.workoutplanner.ui.feature.history.HistoryScreen
```

- [ ] **Step 4: Verify build compiles**

Run:
```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/feature/history/
git add app/src/main/java/de/melobeat/workoutplanner/ui/navigation/WorkoutNavGraph.kt
git commit -m "refactor: move history feature files to ui/feature/history/"
```

---

### Task 4: Move `ui/feature/home/` files

**Files:**
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/feature/home/HomeScreen.kt`
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/feature/home/HomeViewModel.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/HomeScreen.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/HomeViewModel.kt`

- [ ] **Step 1: Move 2 files**

Run:
```bash
mkdir -p app/src/main/java/de/melobeat/workoutplanner/ui/feature/home
git mv app/src/main/java/de/melobeat/workoutplanner/ui/HomeScreen.kt app/src/main/java/de/melobeat/workoutplanner/ui/feature/home/HomeScreen.kt
git mv app/src/main/java/de/melobeat/workoutplanner/ui/HomeViewModel.kt app/src/main/java/de/melobeat/workoutplanner/ui/feature/home/HomeViewModel.kt
```

- [ ] **Step 2: Update package declarations**

Change from `package de.melobeat.workoutplanner.ui` to `package de.melobeat.workoutplanner.ui.feature.home` in both files.

- [ ] **Step 3: Update imports in `ui/navigation/WorkoutNavGraph.kt`**

Replace:
```kotlin
import de.melobeat.workoutplanner.ui.HomeScreen
```
With:
```kotlin
import de.melobeat.workoutplanner.ui.feature.home.HomeScreen
```

- [ ] **Step 4: Verify build compiles**

Run:
```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/feature/home/
git add app/src/main/java/de/melobeat/workoutplanner/ui/navigation/WorkoutNavGraph.kt
git commit -m "refactor: move home feature files to ui/feature/home/"
```

---

### Task 5: Move `ui/feature/routines/` files

**Files:**
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/feature/routines/RoutinesScreen.kt`
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/feature/routines/RoutineDetailScreen.kt`
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/feature/routines/CreateRoutineScreen.kt`
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/feature/routines/RoutinesViewModel.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/RoutinesScreen.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/RoutineDetailScreen.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/CreateRoutineScreen.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/RoutinesViewModel.kt`

- [ ] **Step 1: Move 4 files**

Run:
```bash
mkdir -p app/src/main/java/de/melobeat/workoutplanner/ui/feature/routines
git mv app/src/main/java/de/melobeat/workoutplanner/ui/RoutinesScreen.kt app/src/main/java/de/melobeat/workoutplanner/ui/feature/routines/RoutinesScreen.kt
git mv app/src/main/java/de/melobeat/workoutplanner/ui/RoutineDetailScreen.kt app/src/main/java/de/melobeat/workoutplanner/ui/feature/routines/RoutineDetailScreen.kt
git mv app/src/main/java/de/melobeat/workoutplanner/ui/CreateRoutineScreen.kt app/src/main/java/de/melobeat/workoutplanner/ui/feature/routines/CreateRoutineScreen.kt
git mv app/src/main/java/de/melobeat/workoutplanner/ui/RoutinesViewModel.kt app/src/main/java/de/melobeat/workoutplanner/ui/feature/routines/RoutinesViewModel.kt
```

- [ ] **Step 2: Update package declarations**

Change from `package de.melobeat.workoutplanner.ui` to `package de.melobeat.workoutplanner.ui.feature.routines` in all 4 files.

- [ ] **Step 3: Update imports in `ui/feature/routines/CreateRoutineScreen.kt`**

Replace the existing `ui.common` import (added in Task 1) is already correct. Verify these imports exist:
```kotlin
import de.melobeat.workoutplanner.ui.common.ExerciseSelectionDialog
import de.melobeat.workoutplanner.ui.common.DayCard
```
(`DayCard` is defined in `ui/common/RoutineDayCard.kt` — same `ui.common` package, no import needed.)

- [ ] **Step 4: Update imports in `ui/navigation/WorkoutNavGraph.kt`**

Replace:
```kotlin
import de.melobeat.workoutplanner.ui.CreateRoutineScreen
import de.melobeat.workoutplanner.ui.RoutineDetailScreen
import de.melobeat.workoutplanner.ui.RoutinesScreen
```
With:
```kotlin
import de.melobeat.workoutplanner.ui.feature.routines.CreateRoutineScreen
import de.melobeat.workoutplanner.ui.feature.routines.RoutineDetailScreen
import de.melobeat.workoutplanner.ui.feature.routines.RoutinesScreen
```

- [ ] **Step 5: Verify build compiles**

Run:
```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/feature/routines/
git add app/src/main/java/de/melobeat/workoutplanner/ui/navigation/WorkoutNavGraph.kt
git commit -m "refactor: move routines feature files to ui/feature/routines/"
```

---

### Task 6: Move `ui/feature/exercises/` files

**Files:**
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/feature/exercises/ExercisesScreen.kt`
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/feature/exercises/ExerciseLibraryViewModel.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/ExercisesScreen.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseLibraryViewModel.kt`

- [ ] **Step 1: Move 2 files**

Run:
```bash
mkdir -p app/src/main/java/de/melobeat/workoutplanner/ui/feature/exercises
git mv app/src/main/java/de/melobeat/workoutplanner/ui/ExercisesScreen.kt app/src/main/java/de/melobeat/workoutplanner/ui/feature/exercises/ExercisesScreen.kt
git mv app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseLibraryViewModel.kt app/src/main/java/de/melobeat/workoutplanner/ui/feature/exercises/ExerciseLibraryViewModel.kt
```

- [ ] **Step 2: Update package declarations**

Change from `package de.melobeat.workoutplanner.ui` to `package de.melobeat.workoutplanner.ui.feature.exercises` in both files.

- [ ] **Step 3: Update imports in `ui/navigation/WorkoutNavGraph.kt`**

Replace:
```kotlin
import de.melobeat.workoutplanner.ui.ExercisesScreen
```
With:
```kotlin
import de.melobeat.workoutplanner.ui.feature.exercises.ExercisesScreen
```

- [ ] **Step 4: Verify build compiles**

Run:
```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/feature/exercises/
git add app/src/main/java/de/melobeat/workoutplanner/ui/navigation/WorkoutNavGraph.kt
git commit -m "refactor: move exercises feature files to ui/feature/exercises/"
```

---

### Task 7: Move `ui/feature/equipment/` files

**Files:**
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/feature/equipment/EquipmentScreen.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/EquipmentScreen.kt`

- [ ] **Step 1: Move 1 file**

Run:
```bash
mkdir -p app/src/main/java/de/melobeat/workoutplanner/ui/feature/equipment
git mv app/src/main/java/de/melobeat/workoutplanner/ui/EquipmentScreen.kt app/src/main/java/de/melobeat/workoutplanner/ui/feature/equipment/EquipmentScreen.kt
```

- [ ] **Step 2: Update package declaration**

Change from `package de.melobeat.workoutplanner.ui` to `package de.melobeat.workoutplanner.ui.feature.equipment`.

- [ ] **Step 3: Update imports in `ui/navigation/WorkoutNavGraph.kt`**

Replace:
```kotlin
import de.melobeat.workoutplanner.ui.EquipmentScreen
```
With:
```kotlin
import de.melobeat.workoutplanner.ui.feature.equipment.EquipmentScreen
```

- [ ] **Step 4: Verify build compiles**

Run:
```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/feature/equipment/
git add app/src/main/java/de/melobeat/workoutplanner/ui/navigation/WorkoutNavGraph.kt
git commit -m "refactor: move equipment feature files to ui/feature/equipment/"
```

---

### Task 8: Move `ui/feature/settings/` files

**Files:**
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/feature/settings/SettingsScreen.kt`
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/feature/settings/TimerSettingsScreen.kt`
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/feature/settings/TimerSettingsViewModel.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/SettingsScreen.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/TimerSettingsScreen.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/TimerSettingsViewModel.kt`

- [ ] **Step 1: Move 3 files**

Run:
```bash
mkdir -p app/src/main/java/de/melobeat/workoutplanner/ui/feature/settings
git mv app/src/main/java/de/melobeat/workoutplanner/ui/SettingsScreen.kt app/src/main/java/de/melobeat/workoutplanner/ui/feature/settings/SettingsScreen.kt
git mv app/src/main/java/de/melobeat/workoutplanner/ui/TimerSettingsScreen.kt app/src/main/java/de/melobeat/workoutplanner/ui/feature/settings/TimerSettingsScreen.kt
git mv app/src/main/java/de/melobeat/workoutplanner/ui/TimerSettingsViewModel.kt app/src/main/java/de/melobeat/workoutplanner/ui/feature/settings/TimerSettingsViewModel.kt
```

- [ ] **Step 2: Update package declarations**

Change from `package de.melobeat.workoutplanner.ui` to `package de.melobeat.workoutplanner.ui.feature.settings` in all 3 files.

- [ ] **Step 3: Update imports in `ui/navigation/WorkoutNavGraph.kt`**

Replace:
```kotlin
import de.melobeat.workoutplanner.ui.SettingsScreen
import de.melobeat.workoutplanner.ui.TimerSettingsScreen
```
With:
```kotlin
import de.melobeat.workoutplanner.ui.feature.settings.SettingsScreen
import de.melobeat.workoutplanner.ui.feature.settings.TimerSettingsScreen
```

- [ ] **Step 4: Update imports in `MainActivity.kt`**

Replace:
```kotlin
import de.melobeat.workoutplanner.ui.TimerSettingsViewModel
```
With:
```kotlin
import de.melobeat.workoutplanner.ui.feature.settings.TimerSettingsViewModel
```

- [ ] **Step 5: Verify build compiles**

Run:
```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/feature/settings/
git add app/src/main/java/de/melobeat/workoutplanner/ui/navigation/WorkoutNavGraph.kt
git add app/src/main/java/de/melobeat/workoutplanner/MainActivity.kt
git commit -m "refactor: move settings feature files to ui/feature/settings/"
```

---

### Task 9: Move `ui/feature/profile/` files

**Files:**
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/feature/profile/ProfileScreen.kt`
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/feature/profile/UserProfileViewModel.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/ProfileScreen.kt`
- Delete: `app/src/main/java/de/melobeat/workoutplanner/ui/UserProfileViewModel.kt`

- [ ] **Step 1: Move 2 files**

Run:
```bash
mkdir -p app/src/main/java/de/melobeat/workoutplanner/ui/feature/profile
git mv app/src/main/java/de/melobeat/workoutplanner/ui/ProfileScreen.kt app/src/main/java/de/melobeat/workoutplanner/ui/feature/profile/ProfileScreen.kt
git mv app/src/main/java/de/melobeat/workoutplanner/ui/UserProfileViewModel.kt app/src/main/java/de/melobeat/workoutplanner/ui/feature/profile/UserProfileViewModel.kt
```

- [ ] **Step 2: Update package declarations**

Change from `package de.melobeat.workoutplanner.ui` to `package de.melobeat.workoutplanner.ui.feature.profile` in both files.

- [ ] **Step 3: Update imports in `ui/navigation/WorkoutNavGraph.kt`**

Replace:
```kotlin
import de.melobeat.workoutplanner.ui.ProfileScreen
```
With:
```kotlin
import de.melobeat.workoutplanner.ui.feature.profile.ProfileScreen
```

- [ ] **Step 4: Verify build compiles**

Run:
```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/feature/profile/
git add app/src/main/java/de/melobeat/workoutplanner/ui/navigation/WorkoutNavGraph.kt
git commit -m "refactor: move profile feature files to ui/feature/profile/"
```

---

### Task 10: Move test files to matching packages

**Files:**
- Create: `app/src/test/java/de/melobeat/workoutplanner/ui/feature/workout/ActiveWorkoutViewModelTest.kt`
- Create: `app/src/test/java/de/melobeat/workoutplanner/ui/feature/routines/RoutinesViewModelTest.kt`
- Delete: `app/src/test/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModelTest.kt`
- Delete: `app/src/test/java/de/melobeat/workoutplanner/ui/RoutinesViewModelTest.kt`

- [ ] **Step 1: Move 2 test files**

Run:
```bash
mkdir -p app/src/test/java/de/melobeat/workoutplanner/ui/feature/workout
mkdir -p app/src/test/java/de/melobeat/workoutplanner/ui/feature/routines
git mv app/src/test/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModelTest.kt app/src/test/java/de/melobeat/workoutplanner/ui/feature/workout/ActiveWorkoutViewModelTest.kt
git mv app/src/test/java/de/melobeat/workoutplanner/ui/RoutinesViewModelTest.kt app/src/test/java/de/melobeat/workoutplanner/ui/feature/routines/RoutinesViewModelTest.kt
```

- [ ] **Step 2: Update package declaration in `ActiveWorkoutViewModelTest.kt`**

Change from:
```kotlin
package de.melobeat.workoutplanner.ui
```
To:
```kotlin
package de.melobeat.workoutplanner.ui.feature.workout
```

Add imports for types now in different packages:
```kotlin
import de.melobeat.workoutplanner.ui.feature.workout.ActiveWorkoutViewModel
import de.melobeat.workoutplanner.ui.feature.workout.RestTimerContext
import de.melobeat.workoutplanner.ui.feature.workout.ExerciseUiState
import de.melobeat.workoutplanner.ui.feature.workout.SetUiState
```

Remove the now-redundant `import de.melobeat.workoutplanner.ui.ActiveWorkoutViewModel` if present (same package after move).

- [ ] **Step 3: Update package declaration in `RoutinesViewModelTest.kt`**

Change from:
```kotlin
package de.melobeat.workoutplanner.ui
```
To:
```kotlin
package de.melobeat.workoutplanner.ui.feature.routines
```

Add import:
```kotlin
import de.melobeat.workoutplanner.ui.feature.routines.RoutinesViewModel
```

- [ ] **Step 4: Run all unit tests**

Run:
```bash
./gradlew :app:testDebugUnitTest
```
Expected: All tests pass (6 test files, ~80+ tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/test/java/de/melobeat/workoutplanner/ui/feature/
git commit -m "refactor: move test files to matching feature packages"
```

---

### Task 11: Final verification and cleanup

- [ ] **Step 1: Run full debug build**

Run:
```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run lint**

Run:
```bash
./gradlew :app:lintDebug
```
Expected: BUILD SUCCESSFUL (no new lint errors introduced by moves)

- [ ] **Step 3: Run all tests**

Run:
```bash
./gradlew :app:testDebugUnitTest
```
Expected: All tests pass

- [ ] **Step 4: Verify `ui/` directory structure**

Run:
```bash
find app/src/main/java/de/melobeat/workoutplanner/ui -type f -name "*.kt" | sort
```

Expected output:
```
app/src/main/java/de/melobeat/workoutplanner/ui/FormatElapsedTime.kt
app/src/main/java/de/melobeat/workoutplanner/ui/common/ExerciseCard.kt
app/src/main/java/de/melobeat/workoutplanner/ui/common/ExerciseSelectionDialog.kt
app/src/main/java/de/melobeat/workoutplanner/ui/common/RestTimerBanner.kt
app/src/main/java/de/melobeat/workoutplanner/ui/common/RoutineDayCard.kt
app/src/main/java/de/melobeat/workoutplanner/ui/common/RoutineExerciseEditItem.kt
app/src/main/java/de/melobeat/workoutplanner/ui/common/WorkoutStepperCard.kt
app/src/main/java/de/melobeat/workoutplanner/ui/feature/equipment/EquipmentScreen.kt
app/src/main/java/de/melobeat/workoutplanner/ui/feature/exercises/ExerciseLibraryViewModel.kt
app/src/main/java/de/melobeat/workoutplanner/ui/feature/exercises/ExercisesScreen.kt
app/src/main/java/de/melobeat/workoutplanner/ui/feature/history/HistoryScreen.kt
app/src/main/java/de/melobeat/workoutplanner/ui/feature/history/HistoryViewModel.kt
app/src/main/java/de/melobeat/workoutplanner/ui/feature/home/HomeScreen.kt
app/src/main/java/de/melobeat/workoutplanner/ui/feature/home/HomeViewModel.kt
app/src/main/java/de/melobeat/workoutplanner/ui/feature/profile/ProfileScreen.kt
app/src/main/java/de/melobeat/workoutplanner/ui/feature/profile/UserProfileViewModel.kt
app/src/main/java/de/melobeat/workoutplanner/ui/feature/routines/CreateRoutineScreen.kt
app/src/main/java/de/melobeat/workoutplanner/ui/feature/routines/RoutineDetailScreen.kt
app/src/main/java/de/melobeat/workoutplanner/ui/feature/routines/RoutinesScreen.kt
app/src/main/java/de/melobeat/workoutplanner/ui/feature/routines/RoutinesViewModel.kt
app/src/main/java/de/melobeat/workoutplanner/ui/feature/settings/SettingsScreen.kt
app/src/main/java/de/melobeat/workoutplanner/ui/feature/settings/TimerSettingsScreen.kt
app/src/main/java/de/melobeat/workoutplanner/ui/feature/settings/TimerSettingsViewModel.kt
app/src/main/java/de/melobeat/workoutplanner/ui/feature/workout/ActiveWorkoutViewModel.kt
app/src/main/java/de/melobeat/workoutplanner/ui/feature/workout/WorkoutScreen.kt
app/src/main/java/de/melobeat/workoutplanner/ui/feature/workout/WorkoutSummaryScreen.kt
app/src/main/java/de/melobeat/workoutplanner/ui/feature/workout/WorkoutUiState.kt
app/src/main/java/de/melobeat/workoutplanner/ui/navigation/NavRoutes.kt
app/src/main/java/de/melobeat/workoutplanner/ui/navigation/WorkoutNavGraph.kt
app/src/main/java/de/melobeat/workoutplanner/ui/theme/Color.kt
app/src/main/java/de/melobeat/workoutplanner/ui/theme/Theme.kt
app/src/main/java/de/melobeat/workoutplanner/ui/theme/Type.kt
```

No `.kt` files should remain directly in `ui/` except `FormatElapsedTime.kt`.

- [ ] **Step 5: Commit**

```bash
git commit --allow-empty -m "refactor: verify final structure — all checks pass"
```
(Only if there are uncommitted changes from verification steps.)
