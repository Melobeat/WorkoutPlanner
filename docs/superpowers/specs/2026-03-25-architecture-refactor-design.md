# Architecture Refactor Design
**Date:** 2026-03-25
**Status:** Approved
**Scope:** Single-module production code only (no tests, no multi-module split)

---

## Problem Statement

The current codebase has several architectural issues that will cause pain as the app grows:

1. Monolithic `WorkoutViewModel` handles all features (workout, routines, exercises, equipment, history)
2. `WorkoutDao` injected directly into ViewModel — no repository abstraction
3. Manual `mutableStateOf` navigation in `MainActivity` instead of Navigation Compose
4. Mutable `ExerciseState`/`SetState` classes with `var` fields — Compose recomposition unreliable
5. No error handling in database operations — silent failures
6. Fragile timer coroutine that is not automatically cancelled
7. `fallbackToDestructiveMigration()` risks wiping future user data
8. `collectAsState()` used instead of lifecycle-aware `collectAsStateWithLifecycle()`
9. Dead `onClick = {}` FABs in ExercisesScreen and EquipmentScreen
10. `Dispatchers.IO` hardcoded instead of injected

---

## Approach: Layer-by-Layer Refactor

Fix in dependency order: data layer → ViewModels → navigation → quick wins. Each layer builds on the previous.

---

## Layer 1 — Data Layer (Repository)

Introduce `WorkoutRepository` that wraps `WorkoutDao`. No ViewModel will depend on the DAO directly after this change.

### Interface

```kotlin
class WorkoutRepository @Inject constructor(
    private val dao: WorkoutDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    // Streams (cold Flows backed by Room)
    fun getRoutinesStream(): Flow<List<Routine>>
    fun getSelectedRoutineStream(): Flow<Routine?>
    fun getRoutineStream(routineId: String): Flow<Routine?>  // single routine by ID for detail screen
    fun getExercisesStream(): Flow<List<Exercise>>
    fun getEquipmentStream(): Flow<List<Equipment>>
    fun getWorkoutHistoryStream(): Flow<List<WorkoutHistoryWithExercises>>
    fun getHistoryForExercise(exerciseId: String): Flow<List<ExerciseHistoryEntity>>

    // Mutations (suspend, main-safe via withContext)
    suspend fun selectRoutine(routineId: String)
    suspend fun saveRoutine(name: String, description: String, days: List<WorkoutDay>, existingId: String?)
    suspend fun deleteRoutine(routineId: String)
    suspend fun updateLastCompletedDayIndex(routineId: String, dayIndex: Int)
    // equipmentId is nullable — exercises may have no equipment
    suspend fun saveExercise(name: String, muscleGroup: String, description: String, equipmentId: String?, existingId: String?)
    suspend fun deleteExercise(exerciseId: String)
    suspend fun saveEquipment(name: String, existingId: String?)
    suspend fun deleteEquipment(equipmentId: String)
    // routineName passed by caller (ActiveWorkoutViewModel already holds it)
    suspend fun finishWorkout(history: List<ExerciseHistory>, workoutDay: WorkoutDay, dayIndex: Int, durationMs: Long, routineName: String)
}
```

`getRoutineStream(routineId)` requires adding a `@Transaction @Query("SELECT * FROM routines WHERE id = :routineId LIMIT 1")` method to `WorkoutDao` returning `Flow<RoutineWithDays?>`.

### Hilt

- Add `@IoDispatcher` qualifier + binding in `DatabaseModule`
- Bind `WorkoutRepository` as `@Singleton`

### Entity-to-domain conversions

Move `toDomain()` extension functions out of ViewModel into a `data/Mappers.kt` file.

---

## Layer 2 — ViewModels

Split `WorkoutViewModel` into 5 focused ViewModels. Each:
- Injects only `WorkoutRepository`
- Exposes `StateFlow<UiState>` via `stateIn(WhileSubscribed(5_000))`
- Uses immutable `data class UiState(...)` — no `var` fields on state objects
- Handles errors: catches exceptions, emits `error: String?` in UiState

### ViewModels

| ViewModel | Screen(s) | Key UiState fields |
|---|---|---|
| `HomeViewModel` | HomeScreen | `routineName`, `nextDay`, `recentHistory`, `isLoading`, `error` |
| `ActiveWorkoutViewModel` | WorkoutScreen | `exercises: ImmutableList<ExerciseUiState>`, `elapsedSeconds`, `isFinished`, `error` |
| `HistoryViewModel` | HistoryScreen | `sessions`, `exerciseNameMap: Map<String, String>`, `isLoading`, `error` |
| `RoutinesViewModel` | RoutinesScreen, RoutineDetailScreen, CreateRoutineScreen | `routines`, `selectedRoutine: Routine?`, `isLoading`, `error` |
| `ExerciseLibraryViewModel` | ExercisesScreen, EquipmentScreen | `exercises`, `equipment`, `isLoading`, `error` |

**`HistoryViewModel` note:** Must join `getWorkoutHistoryStream()` with `getExercisesStream()` inside the ViewModel to build `exerciseNameMap: Map<String, String>` (exerciseId → name). This avoids a cross-ViewModel dependency from `HistoryScreen`.

**`RoutinesViewModel` note:** Exposes `selectedRoutine` (the routine currently being viewed/edited, loaded via `getRoutineStream(routineId)`) separately from `routines` (the full list). `RoutineDetailRoute` passes `routineId`; the ViewModel loads it on `init` via `savedStateHandle.toRoute<RoutineDetailRoute>()`.

### HomeViewModel additional operations

- `fun updateNextDay(routineId: String, dayIndex: Int)` — calls `repository.updateLastCompletedDayIndex(...)`. Owned here because it is triggered from HomeScreen's "Swap Next Day" action.

### ActiveWorkoutViewModel operations

- `fun startWorkout(day: WorkoutDay, dayIndex: Int, routineName: String)` — initialises UiState with exercise states loaded from last history; sets `isActive = true`, `isFullScreen = true`
- `fun cancelWorkout()` — resets UiState to empty/inactive. Pure in-memory reset, does not touch the repository.
- `fun finishWorkout()` — calls `repository.finishWorkout(...)` with `routineName` from the ViewModel's own state
- `fun setFullScreen(fullScreen: Boolean)` — sets `uiState.isFullScreen`; called by `ActiveWorkoutRoute` when it enters/leaves the composition
- `fun toggleSetDone(exerciseIndex: Int, setIndex: Int)`
- `fun updateSetWeight(exerciseIndex: Int, setIndex: Int, weight: String)`
- `fun updateSetReps(exerciseIndex: Int, setIndex: Int, reps: String)`
- `fun addSet(exerciseIndex: Int)`
- `fun removeSet(exerciseIndex: Int, setIndex: Int)`
- `fun toggleExerciseExpanded(exerciseIndex: Int)`
- `fun addExercise(exercise: Exercise)` — appends a new `ExerciseUiState`, initialising sets from `exercise.routineSets`
- `fun swapExercise(exerciseIndex: Int, newExercise: Exercise)`
- `fun removeExercise(exerciseIndex: Int)`
- `fun reorderExercise(from: Int, to: Int)`

### Immutable state for workout tracking

Replace mutable `ExerciseState`/`SetState` classes with immutable data classes. All mutations go through `ActiveWorkoutViewModel` event methods that copy-and-update `UiState`.

```kotlin
data class ActiveWorkoutUiState(
    val isActive: Boolean = false,        // true after startWorkout, false after cancel/finish
    val isFullScreen: Boolean = false,    // true when ActiveWorkoutRoute is in foreground
    val exercises: ImmutableList<ExerciseUiState> = persistentListOf(),
    val elapsedTime: Long = 0L,           // milliseconds
    val isFinished: Boolean = false,
    val error: String? = null
)

data class ExerciseUiState(
    val exerciseId: String,
    val name: String,
    val sets: ImmutableList<SetUiState>,
    val isExpanded: Boolean = false,
    val lastSets: ImmutableList<Pair<Double, Int>> = persistentListOf()
)

data class SetUiState(
    val index: Int,
    val weight: String,
    val reps: String,
    val isAmrap: Boolean,
    val isDone: Boolean = false,
    val originalReps: String
)
```

**Dialog state:** The existing `WorkoutScreen` has several dialogs (add exercise, swap exercise, cancel confirmation) whose `onDismissRequest` are currently no-ops. Dialog visibility state (e.g. `showAddExerciseDialog`, `exerciseToSwapIndex`) should be held as local `remember` state in the composable — not in UiState. `onDismissRequest` must reset that local state (not remain a no-op).

### Timer fix

The existing timer uses a manually-managed `Job` that must be explicitly cancelled. Replace with a `MutableStateFlow<Long>` that is reset on each `startWorkout()` call and driven by a per-workout coroutine job:

```kotlin
private val _elapsedTime = MutableStateFlow(0L)
val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()

private var timerJob: Job? = null

fun startWorkout(...) {
    timerJob?.cancel()
    _elapsedTime.value = 0L
    val start = System.currentTimeMillis()
    timerJob = viewModelScope.launch {
        while (true) {
            _elapsedTime.value = System.currentTimeMillis() - start
            delay(1000)
        }
    }
    // ... rest of startWorkout
}

fun cancelWorkout() {
    timerJob?.cancel()
    timerJob = null
    _elapsedTime.value = 0L
    // reset UiState to inactive
}

fun finishWorkout() {
    timerJob?.cancel()
    timerJob = null
    // save history...
}
```

This correctly resets on each new workout start and zeroes out on cancel/finish. The coroutine is still scoped to `viewModelScope` so it cannot outlive the ViewModel. `elapsedTime` emits **milliseconds** to remain compatible with the existing `formatElapsedTime(millis: Long)` helper in `WorkoutScreen`.

---

## Layer 3 — Navigation

Replace all `mutableStateOf` navigation state in `MainActivity` with a type-safe Navigation Compose graph.

### Routes

```kotlin
@Serializable object HomeRoute
@Serializable object HistoryRoute
@Serializable object SettingsRoute
@Serializable object ExercisesRoute
@Serializable object EquipmentRoute
@Serializable object RoutinesRoute
@Serializable data class RoutineDetailRoute(val routineId: String)
@Serializable data class CreateRoutineRoute(val routineId: String? = null)
@Serializable object ActiveWorkoutRoute
```

### Graph structure

```
NavHost
├── HomeRoute            ← bottom nav tab
├── HistoryRoute         ← bottom nav tab
├── ActiveWorkoutRoute   ← launched from HomeScreen, full-screen (no bottom bar)
└── settings/ (nested graph)
    ├── SettingsRoute    ← bottom nav tab
    ├── ExercisesRoute
    ├── EquipmentRoute
    ├── RoutinesRoute
    ├── RoutineDetailRoute(routineId)
    └── CreateRoutineRoute(routineId?)
```

### Minimized workout banner

The current app shows a persistent bottom banner while a workout is active and the user navigates away. This is **not** a navigation route — it is an overlay rendered above the `NavHost`.

- `ActiveWorkoutViewModel` is scoped to the **Activity** `ViewModelStore` (via `hiltViewModel()` at the Activity level or `viewModels()` in `MainActivity`), not to the back stack entry. This ensures a single instance persists across tab switches.
- `WorkoutPlannerApp` composable observes `activeWorkoutViewModel.uiState` and renders the minimized banner when `uiState.isActive && !uiState.isFullScreen`.
- Tapping the banner navigates to `ActiveWorkoutRoute` (full-screen view).
- The `ActiveWorkoutRoute` composable calls `activeWorkoutViewModel.setFullScreen(true/false)` when entered/left.

### MainActivity

Becomes a thin shell:
- `@AndroidEntryPoint` activity
- Sets content to `WorkoutPlannerApp()`
- `WorkoutPlannerApp` contains `NavHost` + `NavigationSuiteScaffold` + minimized workout banner overlay
- Bottom nav shows Home, History, Settings tabs (hidden on `ActiveWorkoutRoute`)
- No manual navigation state — all handled by NavController

---

## Layer 4 — Error Handling & Quick Wins

### Error handling
- All repository suspend functions wrapped in `try-catch(Exception)`
- Errors emitted as `error: String?` in UiState
- Screens observe error field and show `Snackbar`
- ViewModel exposes `fun clearError()` to dismiss

### Database migrations
```kotlin
// Replace:
fallbackToDestructiveMigration()
// With:
fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7)
```
Future schema versions will not be destructively migrated.

### collectAsStateWithLifecycle
Replace all `collectAsState()` with `collectAsStateWithLifecycle()` in all screen composables.

### Dead FABs
Fix `onClick = {}` in `ExercisesScreen` and `EquipmentScreen` — wire to the add dialog (same pattern as the edit dialog already present in each screen).

### @IoDispatcher
Add qualifier:
```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
```
Bind in `DatabaseModule`. Inject into `WorkoutRepository`. Remove hardcoded `Dispatchers.IO`.

---

## File Changes Summary

**New files:**
- `data/WorkoutRepository.kt`
- `data/Mappers.kt`
- `di/DispatcherModule.kt` (or extend DatabaseModule)
- `ui/HomeViewModel.kt`
- `ui/ActiveWorkoutViewModel.kt`
- `ui/HistoryViewModel.kt`
- `ui/RoutinesViewModel.kt`
- `ui/ExerciseLibraryViewModel.kt`
- `ui/navigation/NavRoutes.kt`
- `ui/navigation/WorkoutNavGraph.kt`

**Modified files:**
- `data/WorkoutDao.kt` — add the following query (required by `getRoutineStream` in the repository; do NOT implement `getRoutineStream` by filtering `getAllRoutinesWithDays()` as that loads all routines unnecessarily):
  ```kotlin
  @Transaction
  @Query("SELECT * FROM routines WHERE id = :routineId LIMIT 1")
  fun getRoutineWithDays(routineId: String): Flow<RoutineWithDays?>
  ```
- `di/DatabaseModule.kt` — add repository + dispatcher bindings
- `MainActivity.kt` — replace nav state with NavHost + workout banner overlay
- `ui/HomeScreen.kt` — use HomeViewModel, collectAsStateWithLifecycle
- `ui/WorkoutScreen.kt` — use ActiveWorkoutViewModel, immutable state
- `ui/HistoryScreen.kt` — use HistoryViewModel (includes exerciseNameMap)
- `ui/RoutinesScreen.kt`, `RoutineDetailScreen.kt`, `CreateRoutineScreen.kt` — use RoutinesViewModel
- `ui/ExercisesScreen.kt`, `EquipmentScreen.kt` — use ExerciseLibraryViewModel, fix FABs
- `data/WorkoutDatabase.kt` — fix migration strategy

**Deleted files:**
- `WorkoutViewModel.kt` — replaced by 5 focused ViewModels