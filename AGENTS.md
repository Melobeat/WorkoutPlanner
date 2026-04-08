# AGENTS.md — WorkoutPlanner

Single-module Android app (`de.melobeat.workoutplanner`). Kotlin + Jetpack Compose + Room + Hilt + Navigation Compose.

## Toolchain

| Item | Version |
|---|---|
| AGP | 9.1.0 |
| Kotlin | 2.3.20 |
| KSP | 2.2.20-2.0.3 |
| Navigation Compose | 2.9.7 |
| Gradle wrapper | 9.4.1 |
| JVM toolchain | 21 (via Foojay resolver in `settings.gradle.kts`; source/target compat: 11) |
| compileSdk / targetSdk | 36 |
| minSdk | **33** — no code path for API < 33 |
| Compose BOM | 2026.03.01 (includes M3 Expressive) |
| Room | 2.8.4 |
| Hilt | 2.59.2 |

## Developer Commands

```bash
./gradlew assembleDebug
./gradlew assembleRelease              # R8 full mode active
./gradlew :app:testDebugUnitTest       # unit tests (JVM, no device)
./gradlew connectedAndroidTest         # instrumented (device/emulator required)
./gradlew :app:lintDebug
./gradlew clean
```

No Makefile, no CI, no pre-commit hooks, no formatter config (Ktlint/Spotless not configured).

Gradle has `org.gradle.configuration-cache=true` and `org.gradle.caching=true` active. After schema changes that require `clean`, run `./gradlew clean` normally — the configuration cache is compatible.

**JAVA_HOME**: `java` is not on the system `PATH`. Set `JAVA_HOME=/opt/android-studio/jbr` before running any `./gradlew` command, or all Gradle invocations will fail with "JAVA_HOME is not set".

**Gradle heap**: `-Xmx4g` in `gradle.properties`. On machines with < 6 GB free RAM, builds may OOM.

## Module Structure

```
app/src/main/java/de/melobeat/workoutplanner/
  WorkoutApplication.kt        @HiltAndroidApp
  MainActivity.kt              @AndroidEntryPoint; NavigationSuiteScaffold host; hosts mini-bar
  data/                        Room entities (Entities.kt), DAO, database, repository, mappers,
                               InitialData.kt (seed JSON parsing), DataStore wrapper
  di/                          DatabaseModule — provides DB, DAO, repo, DataStore, @IoDispatcher
  model/                       Domain models + SampleData.kt
  ui/
    navigation/                NavRoutes.kt (type-safe routes), WorkoutNavGraph.kt
    theme/                     Theme.kt, Color.kt, Type.kt
    FormatElapsedTime.kt       Standalone util; has its own unit test
    WorkoutUiState.kt          ActiveWorkoutUiState, ExerciseUiState, SetUiState, RestTimerUiState,
                               RestTimerContext, RestTimerEvent, ExerciseHistory (also here — not in data/)
    *.kt                       Screens, ViewModels, sub-component composables
app/src/main/assets/           equipment.json, exercises.json (DB seed — loaded only on onCreate)
                               equipment_schema.json, exercises_schema.json (not loaded at runtime)
docs/design-guidelines.md      Authoritative UI spec — read before any UI change
```

**ViewModels in `ui/`:** `ActiveWorkoutViewModel`, `RoutinesViewModel`, `HomeViewModel`, `HistoryViewModel`, `ExerciseLibraryViewModel`.

## Critical Hilt / ViewModel Scoping

`ActiveWorkoutViewModel` is **Activity-scoped**, not NavBackStackEntry-scoped:

```kotlin
viewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity)
```

Every other `@HiltViewModel` inside `NavHost` destinations uses `hiltViewModel()`. Mixing these causes a runtime crash.

## Room — No Migrations

- DB version: **7**; entities: `ExerciseEntity`, `RoutineEntity`, `WorkoutDayEntity`, `WorkoutDayExerciseEntity`, `WorkoutHistoryEntity`, `ExerciseHistoryEntity`, `EquipmentEntity`.
- `fallbackToDestructiveMigrationFrom(1,2,3,4,5,6,7)` — schema changes **destroy all data**. No migration objects exist.
- `exportSchema = false` — no schema export files.
- When adding an entity/column: bump version integer + update `@Database` entities list.
- Seed data in `assets/` reloads only on destructive migration (via `RoomDatabase.Callback` in `WorkoutDatabase.getDatabase()`), not on app update.

## TypeConverter — RoutineSet Is a JSON Blob

`WorkoutDayExerciseEntity.routineSets` stores `List<RoutineSet>` as a JSON string. `RoutineSet` must remain `@Serializable`. Never add non-serializable fields to it.

## Navigation — Type-Safe Routes

Routes are `@Serializable` objects/data classes in `NavRoutes.kt`, not string constants. Arguments extracted via `backStackEntry.toRoute<T>()`.

`SettingsGraphRoute` is the **nested graph key** with `SettingsRoute` as its `startDestination`. The following routes live **inside** this nested graph:
- `SettingsRoute`, `TimerSettingsRoute`
- `RoutinesRoute`
- `RoutineDetailRoute(routineId: String)` — required arg, no default
- `CreateRoutineRoute(routineId: String? = null)` — `null` = create new, non-null = edit existing
- `ExercisesRoute`, `EquipmentRoute`

Navigate to `SettingsGraphRoute` (not `SettingsRoute`) — Navigation Compose resolves the start destination automatically.

Top-level destinations (outside the nested graph): `HomeRoute`, `HistoryRoute`, `ActiveWorkoutRoute`, `WorkoutSummaryRoute`.

Composables receive navigation lambdas — never `NavController` directly.

## Workout State Conventions

- `isDone` on a set is set only by `completeCurrentSet()`. Skipped sets stay `isDone = false`; the Summary screen renders them as "Skipped". Do not add a separate skip flag.
- `WEIGHT_STEP = 2.5` (kg), hardcoded.
- `formatWeight()` strips `.0`: `80.0 → "80"`, `82.5 → "82.5"`.
- `SetUiState` has `originalReps: String` — used to reset reps when tapping a completed set back to zero.
- `ExerciseUiState.lastSets: List<Pair<Double, Int>>` — populated from last session history for pre-filling weight.
- `ExerciseUiState.isExpanded: Boolean = true` — exercises start expanded; advancing to next exercise auto-collapses the previous one.

### showSummary vs isFinished

These are distinct flags with different lifecycles — do not conflate:
- `requestFinish()` → sets `showSummary = true`, stops timer, captures `summaryDurationMs`. Does **not** save data.
- `finishWorkout()` → persists to DB, sets `isFinished = true`. This is the actual save.
- `resumeWorkout()` → clears `showSummary`, restarts timer offset by `summaryDurationMs`.

### RestTimerContext / RestTimerEvent

- `RestTimerContext`: `BetweenSets` (two thresholds: easy + hard) vs. `BetweenExercises` (single threshold). Timer behavior differs.
- `RestTimerEvent`: `EasyMilestone`, `HardMilestone`, `ExerciseMilestone` — emitted from `restTimerEvents: SharedFlow`. UI must collect this in a `LaunchedEffect` for haptics/audio. It is **not** part of `uiState`.

### ExerciseHistory

`ExerciseHistory` is defined in `WorkoutUiState.kt` (not `data/`). Required when calling `repository.finishWorkout(...)`:
```kotlin
data class ExerciseHistory(
    val exerciseId: String,
    val reps: Int,
    val weight: Double,
    val setIndex: Int = 1,
    val isAmrap: Boolean = false
)
```
`finishWorkout` silently skips sets where `reps`/`weight` cannot be parsed (logs a warning, does not abort).

## ActiveWorkoutViewModel — Non-Obvious Behaviors

- **AMRAP sets**: `toggleSetDone` does **nothing** on AMRAP sets. Only `updateSetReps` (called from the reps dialog) marks an AMRAP set done.
- **Tapping a done set**: `toggleSetDone` on a completed non-AMRAP set **decrements reps by 1** on each tap; only resets to `originalReps` + `isDone = false` when reps reach zero. It is not a simple toggle.
- **`setRepsValue` vs `updateSetReps`**: `setRepsValue` (used by steppers) does NOT flip `isDone`. `updateSetReps` (used by AMRAP dialog) DOES flip `isDone = true`. Choose correctly when adding new reps-edit flows.
- **`addSet`**: always adds weight="0", reps="0", isAmrap=false. No defaults from history.
- **`removeSet`**: guards minimum 1 set per exercise (`if (sets.size <= 1) return`).
- **`swapExercise(exerciseIndex, newExercise)`**: replaces the exercise, preserving set count from the replaced exercise.
- **`reorderExercise(from, to)`**: reorders exercises via mutable list remove+insert.
- **`timerSettings`**: eagerly initialized via `stateIn(SharingStarted.Eagerly)` — loads from DataStore immediately on ViewModel creation.

## upsertRoutine Behavior

`upsertRoutine` in `WorkoutDao` deletes all days/exercises for a routine then re-inserts. Day objects with temp IDs (`""` or `"temp_*"`) get fresh UUIDs on save; real persisted IDs pass through. When updating an existing routine, `isSelected` and `lastCompletedDayIndex` are preserved from the existing DB row — never overwritten.

## Active Workout Mini-Bar

Lives in `MainActivity` inner `Scaffold` `bottomBar` when `isActive && !isFullScreen`. Implementation diverges from `docs/design-guidelines.md` in two ways — do not "fix" without intent:
- Surface color: `primaryContainer` (spec says `surfaceVariant`)
- Text styles: elapsed time uses `labelSmall`, workout name uses `titleSmall` + `FontWeight.Bold` (spec says `bodySmall` and `titleMedium` weight 700)

## R8 Full Mode

`android.enableR8.fullMode=true` in `gradle.properties`. `@Serializable` classes survive via the Kotlin serialization plugin (`kotlin-serialization` applied at app level). `proguard-rules.pro` is essentially empty — do not add manual keeps for serialization.

## M3 Expressive + Dynamic Color

- `WorkoutPlannerTheme` passes `dynamicColor = true`. On API 31+ (all real devices given minSdk 33) the seed palette is irrelevant — wallpaper colors dominate. The static purple/pink palette in `Color.kt` is dead code and never activates.
- M3 Expressive components require `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`. The BOM already includes them — no extra dependency needed.
- `motionScheme` is **not** yet wired. The `MaterialTheme(...)` call in `Theme.kt` currently only passes `colorScheme` and `typography`. To add it, the parameter must be added to that call site specifically.

## UI Conventions (from `docs/design-guidelines.md`)

- All buttons: pill-shaped (`CircleShape`).
- All cards: `surfaceVariant` container color. Never custom background.
- Colors: `MaterialTheme.colorScheme.*` only. Raw hex only in the hardcoded gradient hero (`#4A0080`, `#6750A4`, `#B5488A`).
- `error` color is exclusively for the "End workout" button.
- Icons: `Icons.Rounded.*` from `material-icons-extended`.
- `LargeTopAppBar` always pairs with `exitUntilCollapsedScrollBehavior` + `nestedScroll`.
- No custom font (M3 defaults / Roboto only).
- Home screen has **no TopAppBar** — gradient hero replaces it.
- Acid green (`#C8FF00`) is launcher icon only; never in-app.

## Test Patterns

5 unit test files (all JVM, no instrumented tests — `androidTest/` directory does not exist):

- `ActiveWorkoutViewModelTest` — full ViewModel coverage with `UnconfinedTestDispatcher`
- `RoutinesViewModelTest` — ViewModel with mocked repository
- `RestTimerPreferencesRepositoryTest` — real DataStore + JUnit `TemporaryFolder`
- `ExerciseFilterTest` — pure function test, no dispatcher setup
- `FormatElapsedTimeTest` — pure function test, no dispatcher setup

Patterns:
- ViewModel tests: `UnconfinedTestDispatcher`, `Dispatchers.setMain/resetMain` in `@Before`/`@After`.
- Repository mocked with `mockk(relaxed = true)`.
- Flow assertions via Turbine (`app.cash.turbine`).
- Test names use backtick strings: `` `cancelWorkout resets state to defaults`() ``.

**No custom Hilt test runner is configured.** `testInstrumentationRunner` is the plain `AndroidJUnitRunner`. Any future instrumented Hilt tests will need `HiltTestRunner` added to `build.gradle.kts`.

## @IoDispatcher

Custom `@Qualifier` in `DatabaseModule.kt` injecting `Dispatchers.IO`. Any new repository using it must be provided in the Hilt module.
