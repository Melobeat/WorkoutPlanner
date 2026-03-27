# Compose Previews Design

**Date:** 2026-03-27
**Status:** Approved

## Goal

Add `@Preview` annotations to every composable in the app so screens and components are visible in the Android Studio preview panel without running the app or an emulator.

## Architecture

### Stateless screen split

Every screen-level composable currently takes a `viewModel: FooViewModel = hiltViewModel()`, which makes it impossible to preview (Hilt DI is not available in the preview environment). The fix is a mechanical split:

```
FooScreen          — stateful wrapper: collects ViewModel state, calls FooScreenContent
FooScreenContent   — stateless: receives uiState + callback lambdas, owns all layout code
@Preview           — annotates FooScreenContent with hardcoded sample data
```

`FooScreen` becomes a thin delegate — no layout code lives there. All layout moves to `FooScreenContent`. This is the standard Android state-hoisting pattern.

### HomeScreen special case

`HomeScreen` consumes two ViewModels: `HomeViewModel` (for `HomeUiState`) and `ActiveWorkoutViewModel` (for `ActiveWorkoutUiState`). `HomeScreenContent` will accept both states as separate parameters to avoid introducing a new combined type.

### Sub-composables

Sub-composables already accept plain data parameters — no refactor needed. They get a `@Preview` annotation added directly.

## Screens to refactor (9 total)

| File | Screen | UiState type |
|---|---|---|
| HomeScreen.kt | `HomeScreen` | `HomeUiState` + `ActiveWorkoutUiState` |
| RoutinesScreen.kt | `RoutinesScreen` | `RoutinesUiState` |
| RoutineDetailScreen.kt | `RoutineDetailScreen` | `Routine?` (from `detailRoutine` flow) |
| CreateRoutineScreen.kt | `CreateRoutineScreen` | `Routine` (draft) + `RoutinesUiState` |
| ExercisesScreen.kt | `ExercisesScreen` | `ExerciseLibraryUiState` |
| HistoryScreen.kt | `HistoryScreen` | `HistoryUiState` |
| EquipmentScreen.kt | `EquipmentScreen` | `ExerciseLibraryUiState` |
| SettingsScreen.kt | `SettingsScreen` | (no ViewModel — just callbacks) |
| WorkoutScreen.kt | `WorkoutScreen` | `ActiveWorkoutUiState` |

## Sub-composables to preview (no refactor needed)

| File | Composable |
|---|---|
| HomeScreen.kt | `WorkoutDayChooserDialog` |
| RoutineDetailScreen.kt | `WorkoutDayItem` |
| CreateRoutineScreen.kt | `DayCard`, `ExerciseEditItem`, `ExercisePicker` |
| WorkoutScreen.kt | `StepperCard`, `ExerciseSelectionDialog` |
| ExercisesScreen.kt | `AddExerciseDialog`, `ExerciseLibraryItem` |
| HistoryScreen.kt | `HistorySessionCard`, `WorkoutSessionCard` |
| EquipmentScreen.kt | `EquipmentDialog`, `EquipmentItem` |
| SettingsScreen.kt | `SettingsListItem` |

## Sample data strategy

Each `@Preview` function inlines its own fake data using the existing domain model constructors (`Routine(...)`, `Exercise(...)`, `WorkoutDay(...)`, etc.). No shared preview data file — the models have good defaults and the data is minimal.

All previews use `WorkoutPlannerTheme` as a wrapper so they render with the correct Material3 theme.

## File organization

Everything stays co-located in the existing screen files. No new files are created. `FooScreenContent` is defined immediately below `FooScreen` in each file, followed by the `@Preview` function.

## Out of scope

- Multiple preview variants (light/dark, empty state) — single preview per composable only
- Shared `PreviewParameterProvider` classes
- Interactive previews