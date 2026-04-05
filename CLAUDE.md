# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

> **CLI prerequisite:** `JAVA_HOME` must be set. Prefix all `./gradlew` commands, or export for the session:
> ```bash
> export JAVA_HOME=/opt/android-studio/jbr
> ```

```bash
# Build
./gradlew app:assembleDebug
./gradlew app:assembleRelease

# Install on connected device/emulator
./gradlew app:installDebug
adb shell am start -n de.melobeat.workoutplanner/.MainActivity

# Unit tests (run from project root)
./gradlew test
./gradlew app:testDebugUnitTest

# Single test class (package: de.melobeat.workoutplanner)
./gradlew app:testDebugUnitTest --tests "de.melobeat.workoutplanner.ActiveWorkoutViewModelTest"

# Lint
./gradlew app:lint

# Clean
./gradlew clean
```

## Architecture

**Stack:** Kotlin + Jetpack Compose + Material3 + Hilt DI + Room + DataStore + Navigation Compose (typed routes)

**Layer structure:**
- `data/` — Room DAOs, database, repositories, entity-to-domain mappers
- `model/` — Immutable domain data classes (Routine, WorkoutDay, Exercise, RoutineSet)
- `ui/` — Composable screens + ViewModels (MVVM with StateFlow); `navigation/` subdirectory holds `NavRoutes.kt` and `WorkoutNavGraph.kt`
- `di/` — Hilt `DatabaseModule` providing Room DB, repositories, `@IoDispatcher`

**Screens & ViewModels:**
- `HomeScreen` / `HomeViewModel` — landing/dashboard
- `RoutinesScreen`, `RoutineDetailScreen`, `CreateRoutineScreen` / `RoutinesViewModel`
- `WorkoutScreen` / `ActiveWorkoutViewModel` — active workout (full-screen); `WorkoutUiState.kt` holds its state class
- `WorkoutSummaryScreen` — post-workout summary
- `HistoryScreen` / `HistoryViewModel`
- `ExercisesScreen`, `EquipmentScreen` / `ExerciseLibraryViewModel`
- `TimerSettingsScreen`, `SettingsScreen`

### State Management

All ViewModels expose `StateFlow<UiState>` using `SharingStarted.WhileSubscribed(5_000)`. State is always an immutable `data class`; mutations use `.copy()`. User actions flow via lambda callbacks (never pass `NavController` into composables).

### Navigation

Type-safe routes via `@Serializable` objects in `NavRoutes.kt`. Nested graph: `SettingsGraphRoute` contains settings destinations. Active workout is full-screen; a mini-bar shows when `!isFullScreen` via a `DisposableEffect` that sets/resets the flag.

### ActiveWorkoutViewModel

Scoped to `Activity` (not back-stack entry) — obtain with `viewModel(viewModelStoreOwner = LocalActivity.current)`, not `hiltViewModel()`. Maintains `currentExerciseIndex`/`currentSetIndex` cursor pointers. Rest timer switches context between `BetweenSets` (default 90s) and `BetweenExercises` (default 180s).

### Database

Room with JSON TypeConverters. Initial data loaded from `assets/equipment.json` and `assets/exercises.json` in `onCreate()` — this runs only on first install (database creation); changing the JSON files will not re-seed an existing install without a fresh install or manual DB clear. Destructive migration enabled (dev mode only — **disable before release**).

## Testing

Unit tests use JUnit 4 + MockK + Turbine (StateFlow/Flow assertions) + `kotlinx-coroutines-test`. Use `UnconfinedTestDispatcher` and `runTest {}`. Instrumented tests live in `src/androidTest/` (currently empty).

Key test files: `ActiveWorkoutViewModelTest.kt`, `RoutinesViewModelTest.kt`, `RestTimerPreferencesRepositoryTest.kt`, `FormatElapsedTimeTest.kt`.

## Before Release Checklist

- [ ] Set `fallbackToDestructiveMigration(false)` in `WorkoutDatabase` (currently dev-only)
- [ ] Smoke-test release APK on a real device — R8 full mode is enabled and can break reflection-heavy code
- [ ] Verify Room migration path or bump DB version with a proper `Migration` object

## Gotchas

- **AGP 9+**: `kotlinOptions { jvmTarget }` DSL is removed — `compileOptions` sets the JVM target for both Java and Kotlin sources.
- **R8 full mode** is enabled (`android.enableR8.fullMode=true` in `gradle.properties`) — always test the release APK before publishing.
- **DB re-seeding**: Changing `assets/exercises.json` / `assets/equipment.json` won't affect existing installs. Requires fresh install or manual DB clear.

## Design

`docs/design-guidelines.md` is the authoritative source for UI decisions. Key rules:
- **Home hero gradient:** `150° #4A0080 → #6750A4 → #B5488A`
- Dynamic color enabled on Android 12+; M3 seed palette used as fallback
- Shape tokens: pill buttons for primary actions, 16–20dp radius for cards
- Large touch targets (gym use with sweaty hands)
- Acid green (`#C8FF00`) is exclusive to the launcher icon — never use in-app
