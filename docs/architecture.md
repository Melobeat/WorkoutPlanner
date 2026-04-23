# Architecture

Single-module Android app (`de.melobeat.workoutplanner`). Kotlin + Jetpack Compose + Room + Hilt + Navigation Compose.

## Layer Responsibilities

### Data Layer (`data/`)
- Room entities (`Entities.kt`), DAO (`WorkoutDao.kt`), database (`WorkoutDatabase.kt`)
- Repositories: `WorkoutRepository`, `RestTimerPreferencesRepository`, `UserProfileRepository`
- Mappers (`Mappers.kt`): entity → domain model conversion
- Seed data: `InitialData.kt` parses `assets/exercises.json`, `assets/equipment.json`, and `assets/routines.json` on DB creation and destructive migration
- DataStore wrappers: two isolated instances with separate `@Qualifier` annotations

### Domain Layer (`domain/`)
- Models (`domain/model/`): pure Kotlin data classes (`Exercise`, `Routine`, `RoutineSet`, `WorkoutDay`, `ExerciseHistory`, `Equipment`, `UserProfile`, `SideType`, `SampleData`)
- Utilities (`domain/util/`): pure functions (`Filter.kt` — `filterExercises`)

### UI Layer (`ui/`)
- Navigation (`ui/navigation/`): type-safe routes (`NavRoutes.kt`), NavGraph (`WorkoutNavGraph.kt`)
- Theme (`ui/theme/`): `Theme.kt`, `Color.kt`, `Type.kt`
- Common (`ui/common/`): shared composables (`ExerciseCard`, `RestTimerBanner`, `WorkoutStepperCard`, `ExerciseSelectionDialog`, `RoutineDayCard`, `RoutineExerciseEditItem`)
- Features (`ui/feature/`): screen composables + ViewModels (workout, history, home, routines, exercises, equipment, settings, profile)
- Standalone utils: `FormatElapsedTime.kt` (has unit test)

## Dependency Injection

`DatabaseModule.kt` provides:
- `WorkoutDatabase` (singleton, Room builder)
- `WorkoutDao` (from database)
- `WorkoutRepository` (DAO + `@IoDispatcher`)
- `RestTimerPreferencesRepository` (`@RestTimerDataStore` DataStore instance)
- `UserProfileRepository` (`@UserProfileDataStore` DataStore instance)
- `Dispatchers.IO` via `@IoDispatcher` qualifier

**Qualifiers:** Every DataStore instance has its own `@Qualifier` to avoid Hilt ambiguity. When adding a third DataStore, add a new qualifier and provide it in `DatabaseModule`.

## Room Database

- Version: 11; entities: `ExerciseEntity`, `RoutineEntity`, `WorkoutDayEntity`, `WorkoutDayExerciseEntity`, `WorkoutHistoryEntity`, `ExerciseHistoryEntity`, `EquipmentEntity`
- `fallbackToDestructiveMigrationFrom(1,2,3,4,5,6,7,8,9,10)` — schema changes destroy all data. No migration objects exist.
- `exportSchema = false` — no schema export files.
- Seed data in `assets/` reloads only on destructive migration (via `RoomDatabase.Callback` in `WorkoutDatabase.getDatabase()`), not on app update.
- `WorkoutDayExerciseEntity.routineSets` stores `List<RoutineSet>` as a JSON string. `RoutineSet` must remain `@Serializable`. Never add non-serializable fields.

### `upsertRoutine` Behavior
Deletes all days/exercises for a routine then re-inserts. Day objects with temp IDs (`""` or `"temp_*"`) get fresh UUIDs on save; real persisted IDs pass through. When updating an existing routine, `isSelected` and `lastCompletedDayIndex` are preserved from the existing DB row — never overwritten.

## Navigation

Routes are `@Serializable` objects/data classes in `NavRoutes.kt`, not string constants. Arguments extracted via `backStackEntry.toRoute<T>()`.

### Route Inventory
```
Top-level (outside nested graph):
  HomeRoute, HistoryRoute, ActiveWorkoutRoute, WorkoutSummaryRoute

Nested graph (SettingsGraphRoute, startDestination = SettingsRoute):
  SettingsRoute, TimerSettingsRoute, RoutinesRoute,
  RoutineDetailRoute(routineId: String),
  CreateRoutineRoute(routineId: String? = null),
  ExercisesRoute, EquipmentRoute, ProfileRoute
```

Navigate to `SettingsGraphRoute` (the nested graph key) — not `SettingsRoute` directly. Navigation Compose resolves the start destination automatically.

Composables receive navigation lambdas — never `NavController` directly.

## ViewModel Scoping

```kotlin
// Activity-scoped — shared across all destinations (WorkoutScreen, WorkoutSummaryScreen)
val activeWorkoutViewModel: ActiveWorkoutViewModel =
    viewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity)

// NavBackStackEntry-scoped — standard for all other ViewModels
val routinesViewModel: RoutinesViewModel = hiltViewModel()
```

Mixing these scoping patterns crashes at runtime. `ActiveWorkoutViewModel` must always be Activity-scoped.

## Workout State Conventions

### showSummary vs isFinished
- `requestFinish()` → sets `showSummary = true`, stops timer, captures `summaryDurationMs`. Does **not** save data.
- `finishWorkout()` → persists to DB, sets `isFinished = true`. This is the actual save.
- `resumeWorkout()` → clears `showSummary`, restarts timer offset by `summaryDurationMs`.

### RestTimerContext / RestTimerEvent
- `RestTimerContext`: `BetweenSets` (two thresholds: easy + hard) vs. `BetweenExercises` (single threshold). Timer behavior differs.
- `RestTimerEvent`: `EasyMilestone`, `HardMilestone`, `ExerciseMilestone` — emitted from `restTimerEvents: SharedFlow`. UI must collect this in a `LaunchedEffect` for haptics/audio. It is **not** part of `uiState`.

### Set / Exercise State
- `isDone` on a set is set only by `completeCurrentSet()`. Skipped sets stay `isDone = false`; Summary renders them as "Skipped".
- `WEIGHT_STEP = 2.5` (kg), hardcoded.
- `formatWeight()` strips `.0`: `80.0 → "80"`, `82.5 → "82.5"`.
- `SetUiState` has `originalReps: String` — used to reset reps when tapping a completed set back to zero.
- `ExerciseUiState.lastSets: List<Pair<Double, Int>>` — populated from last session history for pre-filling weight.
- `ExerciseUiState.isExpanded: Boolean = true` — default is `true`, but `startWorkout` sets only the first exercise expanded (`index == 0`); advancing to next exercise auto-collapses the previous one.

## ActiveWorkoutViewModel — Non-Obvious Behaviors

- **AMRAP sets**: `toggleSetDone` does **nothing** on AMRAP sets. Only `updateSetReps` (called from the reps dialog) marks an AMRAP set done.
- **Tapping a done set**: `toggleSetDone` on a completed non-AMRAP set **decrements reps by 1** on each tap; only resets to `originalReps` + `isDone = false` when reps reach zero. It is not a simple toggle.
- **`setRepsValue` vs `updateSetReps`**: `setRepsValue` (used by steppers) does NOT flip `isDone`. `updateSetReps` (used by AMRAP dialog) DOES flip `isDone = true`. Choose correctly when adding new reps-edit flows.
- **`setRepsValue` vs `setRepsDirectly`**: `setRepsDirectly` (used by StepperCard tap-to-edit) validates input (non-negative int) then calls `setRepsValue`. Same pattern for `setWeightDirectly`, `setLeftRepsDirectly`, `setRightRepsDirectly`.
- **`updateSetWeight`**: exists alongside `setWeightValue`. `updateSetWeight` is public; `setWeightValue` is private. Both update weight without flipping `isDone`.
- **`addSet`**: always adds weight="0", reps="0", isAmrap=false. No defaults from history.
- **`removeSet`**: guards minimum 1 set per exercise (`if (sets.size <= 1) return`).
- **`swapExercise(exerciseIndex, newExercise)`**: replaces the exercise, preserving set count from the replaced exercise.
- **`reorderExercise(from, to)`**: reorders exercises via mutable list remove+insert.
- **`timerSettings`**: eagerly initialized via `stateIn(SharingStarted.Eagerly)` — loads from DataStore immediately on ViewModel creation.

### ExerciseHistory (for `finishWorkout`)
```kotlin
data class ExerciseHistory(
    val exerciseId: String,
    val reps: Int,
    val weight: Double,
    val setIndex: Int = 1,
    val isAmrap: Boolean = false,
    val sideType: String = "Bilateral",
    val leftReps: Int? = null,
    val rightReps: Int? = null
)
```
`finishWorkout` silently skips sets where `reps`/`weight` cannot be parsed (logs a warning, does not abort).

## Active Workout Mini-Bar

Lives in `MainActivity` inner `Scaffold` `bottomBar` when `isActive && !isFullScreen`. Implementation diverges from `design-guidelines.md` in two ways — do not "fix" without intent:
- Surface color: `primaryContainer` (spec says `surfaceVariant`)
- Text styles: elapsed time uses `labelSmall`, workout name uses `titleSmall` + `FontWeight.Bold` (spec says `bodySmall` and `titleMedium` weight 700)
- Border: `outlineVariant` (nearly transparent 5% white) — intentional subtle separation

## Theme

`WorkoutPlannerTheme(themeMode: String = "dark")` — accepts `"dark"`, `"light"`, or `"system"`. The preference is stored as a `stringPreferencesKey("theme_mode")` in DataStore, surfaced via `RestTimerPreferencesRepository.themeMode: Flow<String>` (defaults to `"dark"`).

`TimerSettingsViewModel.themeMode` uses `SharingStarted.Eagerly` (not `WhileSubscribed`) to avoid first-frame flicker. Every other StateFlow in the app uses `WhileSubscribed(5000)`.

See `design-guidelines.md` for full color token, typography, shape, and component specs.
