# Design: Manual Reps & Weight Input in WorkoutScreen

## Problem

Reps and weight in the active workout view are only modifiable via `+`/`-` stepper buttons. Users cannot type values directly, making it slow to enter large jumps or custom values.

## Solution

Tap the value display in `StepperCard` to enter an inline edit mode. The value becomes a `BasicTextField` with the appropriate keyboard (Number for reps, Decimal for weight). On confirmation (IME Done or focus loss), the value is submitted to the ViewModel. Invalid input is silently ignored and the display reverts.

## Architecture

### 1. `StepperCard` Component (`ui/common/WorkoutStepperCard.kt`)

**New parameters:**
```kotlin
onValueSubmit: ((String) -> Unit)? = null,
keyboardType: KeyboardType = KeyboardType.Number,
```

**Internal state:**
- `isEditing: Boolean` — toggles between display (`Text`) and edit (`BasicTextField`) mode
- `editValue: String` — buffer for the text field during editing
- `FocusRequester` — auto-focuses the field when entering edit mode

**Behavior:**
- Tap value → `isEditing = true`, `editValue = value`, focus field
- IME `Done` or focus loss → validate → `onValueSubmit?.invoke(editValue)` → `isEditing = false`
- Invalid/empty input → `onValueSubmit` not called, `editValue` resets to `value`
- `+`/`-` buttons remain functional in both modes; they update `value` from parent (which triggers recomposition and clears edit mode)

**Visual:**
- `BasicTextField` styled identically to the existing `Text` (same `displaySmall`, `FontWeight.Black`, same color)
- No visible decoration border — looks like the current value text but with a cursor
- `SingleLine`, `Center` alignment, `IME_ACTION_DONE`

### 2. ViewModel (`ui/feature/workout/ActiveWorkoutViewModel.kt`)

**New public methods:**
```kotlin
fun setRepsDirectly(exerciseIndex: Int, setIndex: Int, reps: String)
fun setWeightDirectly(exerciseIndex: Int, setIndex: Int, weight: String)
fun setLeftRepsDirectly(exerciseIndex: Int, setIndex: Int, reps: String)
fun setRightRepsDirectly(exerciseIndex: Int, setIndex: Int, reps: String)
```

**Validation:**
- Reps: `reps.toIntOrNull()?.takeIf { it >= 0 }` → calls `setRepsValue()`
- Weight: `weight.toDoubleOrNull()?.takeIf { it >= 0 }` → calls `setWeightValue()`
- Invalid → no-op (silent, no toast)

### 3. `ExerciseCard` (`ui/common/ExerciseCard.kt`)

**New parameters:**
```kotlin
onRepsSubmit: (Int, Int, String) -> Unit,
onWeightSubmit: (Int, Int, String) -> Unit,
onLeftRepsSubmit: (Int, Int, String) -> Unit,
onRightRepsSubmit: (Int, Int, String) -> Unit,
```

Passed to each `StepperCard` call site as `{ reps -> onRepsSubmit(exerciseIndex, setIndex, reps) }`.

### 4. `WorkoutScreen` (`ui/feature/workout/WorkoutScreen.kt`)

- Wire new lambdas to ViewModel methods
- Pass through `WorkoutScreenContent` to `ExerciseCard`

### 5. Localization

**New string keys:**
- `workout_tap_to_edit` — "Tap to edit" (not used as placeholder; the field has no placeholder, the tap is the affordance)
- This string is optional — the current design has no visible hint text. The tap target is the value itself.

## Data Flow

```
User taps value in StepperCard
  → isEditing = true, BasicTextField focused
  → User types "15"
  → IME Done pressed
  → onValueSubmit("15") called
    → ExerciseCard forwards: onRepsSubmit(exerciseIndex, setIndex, "15")
      → WorkoutScreen forwards: viewModel.setRepsDirectly(exerciseIndex, setIndex, "15")
        → ActiveWorkoutViewModel parses, validates, calls setRepsValue()
          → StateFlow emits new state → recomposition → StepperCard shows "15"
```

## Edge Cases

- **Empty input:** `onValueSubmit` not called, field reverts to last valid value
- **Negative values:** Rejected by validation, no-op
- **Non-numeric input:** Rejected by `toIntOrNull`/`toDoubleOrNull`, no-op
- **AMRAP sets:** Reps input is still functional (AMRAP restriction only applies to `toggleSetDone`)
- **Completed/pending sets:** No editing — only the active set shows `StepperCard` with edit capability
- **Parent value change during edit:** Recomposition resets `editValue` to new `value`, exits edit mode

## Files Changed

| File | Change |
|------|--------|
| `ui/common/WorkoutStepperCard.kt` | Add edit mode, `onValueSubmit`, `keyboardType` params |
| `ui/common/ExerciseCard.kt` | Add `*Submit` lambdas, pass to `StepperCard` |
| `ui/feature/workout/WorkoutScreen.kt` | Wire `*Submit` lambdas to ViewModel |
| `ui/feature/workout/ActiveWorkoutViewModel.kt` | Add `*Directly` methods |
| `res/values/strings.xml` | (Optional) Add `workout_tap_to_edit` |
| `res/values-de/strings.xml` | (Optional) Add German translation |
