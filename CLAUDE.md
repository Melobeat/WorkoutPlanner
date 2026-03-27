# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

Java must be set explicitly — the shell does not have `java` on PATH:

```bash
# Build debug APK
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug

# Run unit tests
JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest

# Run a single test class
JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest --tests "com.example.workoutplanner.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator)
JAVA_HOME=/opt/android-studio/jbr ./gradlew connectedDebugAndroidTest

# Lint
JAVA_HOME=/opt/android-studio/jbr ./gradlew lintDebug
```

## Architecture

Single-module app (`app/`) with a flat package structure under `com.example.workoutplanner`:

```
ui/          — Composables and ViewModels (co-located per screen)
ui/navigation/ — NavRoutes (type-safe @Serializable objects/data classes) + WorkoutNavGraph
data/        — Room entities, DAO, Repository, Mappers
model/       — Domain models (Routine, Exercise, WorkoutDay, RoutineSet, Equipment)
di/          — Hilt DatabaseModule, @IoDispatcher qualifier
```

### State & Navigation flow

`WorkoutPlannerApp` (in `MainActivity.kt`) owns the `NavController` and a single `ActiveWorkoutViewModel` scoped to the `Activity`. This Activity-scoped ViewModel is obtained via `viewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity)` — the only place `viewModel()` (not `hiltViewModel()`) is used.

**ViewModel creation rule:** All `@HiltViewModel` screen-level ViewModels inside `NavHost` composable destinations **must** use `hiltViewModel()` from `hilt-navigation-compose`. Using `viewModel()` from `lifecycle-viewmodel-compose` will crash at runtime because `NavBackStackEntry` does not have Hilt's factory. Only the Activity-scoped `ActiveWorkoutViewModel` (explicitly passed `viewModelStoreOwner = LocalActivity.current`) uses `viewModel()`.

The active-workout bar (shown when `isActive && !isFullScreen`) lives in `MainActivity`'s `Scaffold.bottomBar`. Minimizing the workout screen pops the back stack; the `DisposableEffect` in `WorkoutNavGraph` then sets `isFullScreen = false`, making the bar appear.

Navigation callbacks follow strict UDF: composables receive lambdas (`onNavigateBack`, `onStartWorkout`, etc.) and never hold a `NavController` reference. One-shot async outcomes (e.g. workout finished) are observed with `LaunchedEffect`.

### Data layer

`WorkoutRepository` is the single access point — all callers go through it, never the DAO directly. All suspend functions use `withContext(@IoDispatcher)` for main-safety. Flows from Room are mapped from entities → domain models in the repository using extension functions in `Mappers.kt`.

`RoutineSet` (a `List<RoutineSet>`) is stored as a JSON column on `WorkoutDayExerciseEntity` via a `TypeConverter` using `kotlinx-serialization`.

**Database seeding**: On first open, `WorkoutDatabase` seeds exercises and equipment from `assets/exercises.json` and `assets/equipment.json`. The DB version is currently `7`; migrations are destructive (`.fallbackToDestructiveMigrationFrom`), so incrementing the version wipes all data in dev.

### Key ViewModel responsibilities

| ViewModel | Scope | Owns |
|---|---|---|
| `ActiveWorkoutViewModel` | Activity | Live workout state (`isActive`, `isFullScreen`, timer, exercises) |
| `HomeViewModel` | Composable | Selected routine stream + recent history |
| `RoutinesViewModel` | Composable | Routines CRUD |
| `ExerciseLibraryViewModel` | Composable | Exercise/equipment CRUD |
| `HistoryViewModel` | Composable | Workout history stream |

### Tech stack versions (from `gradle/libs.versions.toml`)

- Compose BOM `2026.03.01`, Material3 1.4.0, adaptive nav suite
- Navigation Compose `2.9.7` (type-safe routes via `@Serializable`)
- Hilt `2.59.2` + KSP (not KAPT)
- Room `2.8.4`
- Kotlin `2.3.10`, AGP `9.1.0`, `compileSdk 36`, `minSdk 33`
