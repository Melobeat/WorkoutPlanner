# Workout Navigation Controls

**Date:** 2026-03-28

## Overview

Add backward navigation and exercise skipping to the active workout screen. Currently the workout only supports moving forward — users cannot revisit a previous set or exercise, nor skip ahead past an exercise they don't want to do.

---

## Requirements

- **Go to previous set**: move the current-set cursor back one step (wrapping to the previous exercise's last set when at set index 0). Does not undo the `isDone` state of any set — it only repositions the cursor for review or value editing.
- **Go to previous exercise**: implicit via "Back" when already on the first set of an exercise.
- **Skip exercise**: jump past the current exercise to the next one. All undone sets of the skipped exercise remain undone, so the summary screen renders them as "Skipped" automatically.
- Skipped exercises appear in the workout summary under their name with each set labelled "Skipped" — no changes to the summary screen required, as it already handles `isDone = false` this way.

---

## Design

### ViewModel — `ActiveWorkoutViewModel`

**`goToPreviousSet()`**

```
if currentSetIndex > 0:
    currentSetIndex -= 1
else if currentExerciseIndex > 0:
    currentExerciseIndex -= 1
    currentSetIndex = exercises[currentExerciseIndex].sets.size - 1
// else: already at the start — button is disabled in UI
```

No state other than the two index fields changes.

**`skipExercise()`**

```
if currentExerciseIndex < exercises.size - 1:
    currentExerciseIndex += 1
    currentSetIndex = 0
else:
    requestFinish()
```

All sets of the skipped exercise remain `isDone = false`. The summary renders them as "Skipped" via its existing logic (`!set.isDone`).

---

### UI — `WorkoutScreenContent` (`WorkoutScreen.kt`)

A `Row` of two `FilledTonalButton` pills is placed directly below the gradient CTA button, using `Arrangement.SpaceBetween`:

```
[ ✓  Done — Set 2  (gradient pill, full width)  ]
[  ←  Back  ]                  [  Skip Exercise  →  ]
```

- **`← Back`** is disabled (`enabled = false`) when `ei == 0 && si == 0` (very first set of first exercise). Matches the M3 Expressive design language: pill shape (`CircleShape`), `FilledTonalButton`, large enough touch target for gym use.
- **`Skip Exercise`** is always enabled. On the last exercise it triggers `requestFinish()`.

New callbacks added to `WorkoutScreenContent`:
- `onGoBack: () -> Unit`
- `onSkipExercise: () -> Unit`

Wired in `WorkoutScreen` to `viewModel.goToPreviousSet()` and `viewModel.skipExercise()`.

---

## What is NOT changing

- `SetUiState.isDone` is never modified by back/skip navigation.
- `WorkoutSummaryScreen` — no changes. Existing per-set "Skipped" rendering already handles exercises that were skipped.
- Data layer, repository, history recording — unchanged.
- The gradient CTA button and all existing workout screen behaviour.