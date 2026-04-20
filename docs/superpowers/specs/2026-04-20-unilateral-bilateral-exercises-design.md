# Unilateral/Bilateral Exercise Support — Design Spec

**Date:** 2026-04-20
**Status:** Draft — awaiting review

## Overview

Add support for unilateral (one-side) and bilateral (both-sides) exercises. Unilateral exercises track reps separately for left and right sides while sharing one weight value. Volume calculations account for both sides (weight × leftReps + weight × rightReps).

## Decisions Summary

| Question | Decision |
|---|---|
| Side type location | On exercise definition (default), overridable at routine level |
| Reps entry during workout | Two stepper fields per set (L and R side by side) |
| History/volume display | Combined total volume; split shown as "L:10 / R:10" |
| Approach | Extend `RoutineSet` with optional side fields |
| DB migration | Destructive (development phase — no version bump needed) |

## Data Layer

### New Enum: `SideType`

```kotlin
enum class SideType { Bilateral, Unilateral }
```

Stored as `String` in Room (`"Bilateral"` / `"Unilateral"`).

### `ExerciseEntity` — new column

```kotlin
val sideType: String = "Bilateral"
```

### `WorkoutDayExerciseEntity` — new column (routine-level override)

```kotlin
val sideType: String? = null
```

When null, inherit from `ExerciseEntity.sideType`.

### `RoutineSet` — new fields

```kotlin
@Serializable
data class RoutineSet(
    val reps: Int,
    val weight: Double,
    val isAmrap: Boolean = false,
    val sideType: String = "Bilateral",
    val leftReps: Int? = null,
    val rightReps: Int? = null
)
```

Backward compatible: new fields have defaults. Existing JSON blobs deserialize correctly.

### `ExerciseHistory` — new fields

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

### Volume calculation utilities

```kotlin
fun RoutineSet.effectiveReps(): Int = when (sideType) {
    "Bilateral" -> reps
    "Unilateral" -> (leftReps ?: 0) + (rightReps ?: 0)
}

fun RoutineSet.volume(): Double = weight * effectiveReps()
```

### Seed data

- `exercises.json`: add `"sideType": "Bilateral"` or `"Unilateral"` to each entry.
- `InitialData.kt`: add `sideType` field to `InitialExercise`.
- Default: `"Bilateral"` for backward compatibility.

### DB changes (destructive migration)

- Add `sideType TEXT NOT NULL DEFAULT 'Bilateral'` to `exercises` table.
- Add `sideType TEXT` (nullable) to `workout_day_exercises` table.
- No version bump needed during development; `fallbackToDestructiveMigrationFrom` handles schema recreation.

## ViewModel & State Layer

### `SetUiState` — new fields

```kotlin
data class SetUiState(
    // ... existing fields ...
    val sideType: String = "Bilateral",
    val leftReps: Int? = null,
    val rightReps: Int? = null,
    val leftOriginalReps: Int? = null,
    val rightOriginalReps: Int? = null,
)
```

### `ActiveWorkoutViewModel` changes

- **`startWorkout()`**: Resolve effective `sideType` per exercise (routine override > exercise default). Populate `leftReps`/`rightReps` from `RoutineSet`.
- **New methods**:
  - `setLeftReps(exerciseIndex, setIndex, reps)` — stepper callback for left side.
  - `setRightReps(exerciseIndex, setIndex, reps)` — stepper callback for right side.
- **`toggleSetDone()`**:
  - Bilateral: unchanged behavior.
  - Unilateral: mark done only if `leftReps > 0 && rightReps > 0`.
  - Tapping a done unilateral set: decrement both sides by 1 (floor at 0). Reset to `leftOriginalReps`/`rightOriginalReps` when both reach 0.
- **`completeCurrentSet()`**: For unilateral, requires both sides to have reps > 0.
- **`updateSetReps()`**: For AMRAP dialog — accepts left/right reps for unilateral.
- **`finishWorkout()`**: Build `ExerciseHistory` with `sideType`, `leftReps`, `rightReps`.
- **`addSet()`**: For unilateral exercises, initializes `leftReps = 0`, `rightReps = 0`.

### `RoutinesViewModel` changes

- When adding an exercise to a routine, set `sideType` from exercise definition.
- Allow override: if user changes side type on a routine exercise, store on `WorkoutDayExerciseEntity.sideType`.

### Mapper updates (`Mappers.kt`)

- `ExerciseWithEquipment.toDomain()`: pass through `sideType`.
- `WorkoutDayExerciseWithDetails.toDomain()`: resolve effective `sideType` (override > exercise default).

## UI Layer

### Exercise Library (`ExercisesScreen.kt`)

- **`AddExerciseDialog`**: Add `SingleChoiceSegmentedButtonRow` with "Bilateral" / "Unilateral" options. Default: Bilateral.
- **`ExerciseLibraryItem`**: Show "Unilateral" badge (small `Text`, `labelSmall`, `secondary` color) next to `muscleGroup` when `sideType != "Bilateral"`.

### Routine Editor (`RoutineDetailScreen.kt`)

- `WorkoutDayItem` / `RoutineExerciseEditItem`: Show inherited side type with a small dropdown or segmented button to override.
- Override stored on `WorkoutDayExerciseEntity.sideType`. Null = inherit.

### Active Workout (`ExerciseCard.kt` / `WorkoutStepperCard.kt`)

- **Bilateral sets**: current single-stepper behavior.
- **Unilateral sets**: two steppers side by side labeled "L" and "R", with one shared weight stepper.
- Layout: weight stepper centered above, L/R steppers in a `Row` with equal weight below.
- Set completion indicator: active only when both sides have reps > 0.

### Workout Summary (`WorkoutSummaryScreen.kt`)

- Unilateral sets display as "L:10 / R:10" instead of "10".
- Volume uses `effectiveReps()` (leftReps + rightReps).

### AMRAP Reps Dialog

- For unilateral AMRAP sets: dialog shows two rep counters (L/R) instead of one.
- Shared weight field.

## Localization

New string keys needed:

| Key | English | German |
|---|---|---|
| `label_bilateral` | Bilateral | Beidseitig |
| `label_unilateral` | Unilateral | Einseitig |
| `label_left` | L | L |
| `label_right` | R | R |
| `workout_side_override` | Side type | Seitentyp |
| `workout_left_reps` | Left reps | Links Wdh. |
| `workout_right_reps` | Right reps | Rechts Wdh. |

## Testing

- `ActiveWorkoutViewModelTest`:
  - Unilateral set requires both sides to have reps before marking done.
  - Tapping done unilateral set decrements both sides.
  - Volume calculation: `weight * (leftReps + rightReps)`.
  - `addSet` on unilateral exercise initializes left/right reps to 0.
  - `setLeftReps` / `setRightReps` update correct side independently.
  - Bilateral exercises behave unchanged.

## Files to Modify

| File | Changes |
|---|---|
| `data/Entities.kt` | Add `sideType` to `ExerciseEntity`, `WorkoutDayExerciseEntity` |
| `model/Exercise.kt` | Add `sideType` to `Exercise`, `SideType` enum |
| `model/Exercise.kt` | Add `sideType` to `Exercise`, `SideType` enum; add `sideType`, `leftReps`, `rightReps` to `RoutineSet` |
| `WorkoutUiState.kt` | Add fields to `SetUiState`, `ExerciseHistory` |
| `data/Mappers.kt` | Pass through `sideType`, resolve override |
| `data/WorkoutDao.kt` | No changes (Room auto-maps new columns) |
| `data/WorkoutRepository.kt` | Pass `sideType` in `saveExercise` |
| `data/InitialData.kt` | Add `sideType` to `InitialExercise` |
| `assets/exercises.json` | Add `sideType` to each entry |
| `ActiveWorkoutViewModel.kt` | Side-aware set logic, new methods |
| `RoutinesViewModel.kt` | Side type inheritance and override |
| `ExercisesScreen.kt` | Side type selector in dialog, badge in list |
| `RoutineDetailScreen.kt` | Override UI |
| `ExerciseCard.kt` | Dual steppers for unilateral |
| `WorkoutStepperCard.kt` | Accept side label parameter |
| `WorkoutSummaryScreen.kt` | Display L/R split |
| `res/values/strings.xml` | New string keys |
| `res/values-de/strings.xml` | German translations |
| `ActiveWorkoutViewModelTest.kt` | New test cases |
