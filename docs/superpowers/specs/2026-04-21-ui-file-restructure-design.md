# UI File Restructure Design

> **Status:** Approved — awaiting implementation plan

**Goal:** Replace the flat `ui/` directory with feature-scoped subdirectories. Each feature owns its screens, ViewModels, and feature-specific UI state. Shared composables live in `ui/common/`.

---

## Current Problem

The `ui/` directory contains 20+ files flat: screens, ViewModels, sub-component composables, UI state classes, and a utility function all in one directory. This makes navigation difficult and obscures which files belong together.

## Proposed Structure

```
app/src/main/java/de/melobeat/workoutplanner/ui/
  common/
    ExerciseCard.kt              # called by WorkoutScreen
    ExerciseSelectionDialog.kt   # called by WorkoutScreen, CreateRoutineScreen
    RestTimerBanner.kt           # called by WorkoutScreen
    RoutineDayCard.kt            # defines DayCard() + calls ExerciseEditItem()
    RoutineExerciseEditItem.kt   # defines ExerciseEditItem()
    WorkoutStepperCard.kt        # defines StepperCard(), called by ExerciseCard
  feature/
    home/
      HomeScreen.kt
      HomeViewModel.kt
    workout/
      WorkoutScreen.kt
      WorkoutSummaryScreen.kt
      ActiveWorkoutViewModel.kt
      WorkoutUiState.kt          # ExerciseHistory, ActiveWorkoutUiState, ExerciseUiState, SetUiState, RestTimerContext/UiState/Event
    history/
      HistoryScreen.kt
      HistoryViewModel.kt
    routines/
      RoutinesScreen.kt
      RoutineDetailScreen.kt
      CreateRoutineScreen.kt
      RoutinesViewModel.kt
    exercises/
      ExercisesScreen.kt
      ExerciseLibraryViewModel.kt
    equipment/
      EquipmentScreen.kt
    settings/
      SettingsScreen.kt
      TimerSettingsScreen.kt
      TimerSettingsViewModel.kt  # used by SettingsScreen, TimerSettingsScreen, MainActivity
    profile/
      ProfileScreen.kt
      UserProfileViewModel.kt    # only used by ProfileScreen
  navigation/
    NavRoutes.kt
    WorkoutNavGraph.kt
  theme/
    Color.kt
    Theme.kt
    Type.kt
  FormatElapsedTime.kt           # stays at ui/ root — truly shared util
```

## Import Changes After Move

Files that will need new imports (cross-package references):

| File | New Imports Needed |
|---|---|
| `feature/workout/WorkoutScreen.kt` | `import ...ui.common.ExerciseCard`, `import ...ui.common.RestTimerBanner`, `import ...ui.common.ExerciseSelectionDialog` |
| `common/ExerciseCard.kt` | `import ...ui.common.StepperCard` (same package, no import needed) |
| `feature/routines/CreateRoutineScreen.kt` | `import ...ui.common.ExerciseSelectionDialog` |
| `common/RoutineDayCard.kt` | `import ...ui.common.ExerciseEditItem` (same package, no import needed) |
| `data/WorkoutRepository.kt` | `import ...ui.feature.workout.ExerciseHistory` (UI state class moving to workout feature) |
| `MainActivity.kt` | `import ...ui.feature.settings.TimerSettingsViewModel` |
| `feature/settings/SettingsScreen.kt` | `import ...ui.feature.settings.TimerSettingsViewModel` (same package, no import) |
| `ui/FormatElapsedTimeTest.kt` | No change (same package) |
| `ui/ActiveWorkoutViewModelTest.kt` | `import ...ui.feature.workout.ActiveWorkoutViewModel`, `import ...ui.feature.workout.*` for state classes |
| `ui/RoutinesViewModelTest.kt` | `import ...ui.feature.routines.RoutinesViewModel` |

## Key Decisions

1. **ViewModels live with their feature screens** — each feature folder owns its ViewModel(s)
2. **Shared composables in `ui/common/`** — ExerciseCard, RestTimerBanner, WorkoutStepperCard, ExerciseSelectionDialog, RoutineDayCard (defines `DayCard()`), RoutineExerciseEditItem (defines `ExerciseEditItem()`)
3. **WorkoutUiState.kt → `feature/workout/`** — all UI state classes are workout-specific
4. **TimerSettingsViewModel → `feature/settings/`** — consumed by Settings graph screens + MainActivity
5. **UserProfileViewModel → `feature/profile/`** — only consumed by ProfileScreen
6. **FormatElapsedTime.kt stays at `ui/` root** — pure utility used across multiple features
7. **navigation/ and theme/ stay as-is** — already well-organized
8. **data/, di/, model/ packages unchanged** — scope is `ui/` only

## Dependency Graph

```
common/ExerciseCard → common/WorkoutStepperCard (same package)
common/RoutineDayCard → common/RoutineExerciseEditItem (same package)
feature/workout/WorkoutScreen → common/ExerciseCard, common/RestTimerBanner, common/ExerciseSelectionDialog
feature/routines/CreateRoutineScreen → common/ExerciseSelectionDialog
data/WorkoutRepository → feature/workout/ExerciseHistory (from WorkoutUiState.kt)
MainActivity → feature/settings/TimerSettingsViewModel
```

## Test File Moves

| Current | New |
|---|---|
| `test/.../ui/ActiveWorkoutViewModelTest.kt` | `test/.../ui/feature/workout/ActiveWorkoutViewModelTest.kt` |
| `test/.../ui/RoutinesViewModelTest.kt` | `test/.../ui/feature/routines/RoutinesViewModelTest.kt` |
| `test/.../ui/FormatElapsedTimeTest.kt` | `test/.../ui/FormatElapsedTimeTest.kt` (stays) |
