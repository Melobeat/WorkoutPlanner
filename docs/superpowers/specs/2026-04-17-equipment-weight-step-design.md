# Design: Equipment Weight Step Feature

**Date:** 2026-04-17
**Status:** Draft — pending review

## Problem

The weight stepper in active workouts uses a hardcoded `WEIGHT_STEP = 2.5` for all equipment. Different equipment types have different plate/increment sizes (barbell: 2.5 kg, dumbbell: 1 kg, machines: 5 kg, etc.), and users should be able to configure this per equipment type.

## Solution Overview

Add a `weightStep` field to the `Equipment` entity with reasonable defaults. The step is resolved when an exercise is loaded into the active workout and stored on `ExerciseUiState`. The stepper uses this per-exercise value instead of the global constant. The Equipment edit dialog exposes a field to customize the step.

## Data Model Changes

### EquipmentEntity (`data/Entities.kt`)
- Add `weightStep: Double = 2.5` to `EquipmentEntity`
- Default 2.5 preserves backward compatibility for any code that expects this field

### Domain Equipment (`model/Equipment.kt`)
- Add `weightStep: Double = 2.5` to the domain `Equipment` data class

### Mapper (`data/Mappers.kt`)
- Update `EquipmentEntity.toDomain()` to include `weightStep`

### Seed Data (`assets/equipment.json`)
Update defaults:

| Equipment | defaultWeight | weightStep |
|---|---|---|
| Barbell | 20.0 | 2.5 |
| EZ Bar | 10.0 | 2.5 |
| Dumbbell | null | 1.0 |
| Pull-up Bar | null | 1.25 |
| Kettlebell | null | 1.0 |
| Cable Machine | null | 1.0 |
| None / Bodyweight | null | 1.0 |

### InitialData.kt
- Update `InitialEquipment` serializable class to include `weightStep: Double? = null`

### Database Version
- Bump from 8 → 9
- `fallbackToDestructiveMigrationFrom(1..8)` — existing pattern

## UI Changes

### EquipmentScreen.kt

**Equipment list items:**
- Display step size alongside bar weight: `"Bar weight: 20 · Step: 2.5"`
- If no defaultWeight set, show only: `"Step: 2.5"`

**EquipmentDialog:**
- Add a third `OutlinedTextField` for "Weight step (kg)"
- Keyboard type: `Decimal`
- Default value: 2.5
- Validation: must be > 0
- Label uses string resource `equipment_weight_step`

### String Resources
- `equipment_weight_step` — "Weight step (kg)" / "Gewichtsschritt (kg)"
- `equipment_weight_step_hint` — hint text for the field

## ActiveWorkout Changes

### ExerciseUiState (`ui/WorkoutUiState.kt`)
- Add `weightStep: Double = 2.5` field

### ActiveWorkoutViewModel
- Remove `private const val WEIGHT_STEP = 2.5`
- When building exercises (from routine or exercise selection), resolve `weightStep` from the exercise's equipment:
  - If exercise has an equipment with a `weightStep`, use it
  - If exercise has no equipment or is bodyweight, use 1.0
- `incrementWeight(exerciseIndex, setIndex)`: use `exercise.weightStep` instead of constant
- `decrementWeight(exerciseIndex, setIndex)`: use `exercise.weightStep` instead of constant
- `addSet`: new set starts with weight="0" — step is irrelevant until user interacts

### Resolution Strategy
- The ViewModel already has access to the repository/equipment stream. When building `ExerciseUiState`, map the equipment's `weightStep` into the state.
- For exercises loaded from routines: the routine's exercises reference equipment IDs — resolve via a cached equipment map.
- For bodyweight exercises (`isBodyweight = true` or `equipmentId = null` or `equip_none`): use 1.0.

## Bodyweight Behavior
- Bodyweight exercises keep the weight stepper visible with step = 1.0 (as requested by user).

## Migration Notes
- Destructive migration (existing pattern) — all user data is wiped on schema change.
- Seed data reloads from updated `equipment.json` on `onCreate`.

## Files to Modify

1. `data/Entities.kt` — add `weightStep` to `EquipmentEntity`
2. `model/Equipment.kt` — add `weightStep` to domain model
3. `data/Mappers.kt` — update mapper
4. `data/InitialData.kt` — update `InitialEquipment` serializable
5. `assets/equipment.json` — add `weightStep` to each entry
6. `data/WorkoutDatabase.kt` — bump version to 9, update `fallbackToDestructiveMigrationFrom`
7. `data/WorkoutDao.kt` — no changes needed (SELECT * picks up new column)
8. `data/WorkoutRepository.kt` — no changes needed (passes through domain model)
9. `ui/WorkoutUiState.kt` — add `weightStep` to `ExerciseUiState`
10. `ui/ActiveWorkoutViewModel.kt` — resolve step, replace constant usage
11. `ui/EquipmentScreen.kt` — add step field to dialog, display in list
12. `res/values/strings.xml` — add new string keys
13. `res/values-de/strings.xml` — add German translations
