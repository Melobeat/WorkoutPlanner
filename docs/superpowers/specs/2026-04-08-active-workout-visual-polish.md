# Active Workout Screen — Visual Polish

**Date:** 2026-04-08  
**Scope:** Three targeted visual fixes to the active workout screen. No new features, no new dependencies, no structural changes.

---

## Problem Summary

Three visual issues identified from screenshot review:

1. **Rest timer banner** blends into the page — `surfaceVariant` card on a `surfaceVariant`-adjacent background; progress bar track is invisible (same color as card); banner has no horizontal margin so it misaligns with card content below.
2. **Active set section** communicates active state only through a thin 3 dp left border — no background fill distinguishes it from surrounding set rows.
3. **Stepper card buttons** (− and +) use different M3 button types (`FilledTonalButton` vs `Button`), giving them different container colors and different widths — they look unbalanced.

---

## Changes

### 1. `RestTimerBanner.kt`

| Property | Before | After |
|---|---|---|
| Card `containerColor` | `surfaceVariant` | `primaryContainer` |
| "REST" label color | `onSurfaceVariant` | `onPrimaryContainer` |
| Milestone text color | `onSurfaceVariant` | `onPrimaryContainer` |
| Timer number color | default (`onSurface`) | `onPrimaryContainer` |
| Progress bar `trackColor` | `surfaceVariant` | `primary.copy(alpha = 0.2f)` |
| Progress bar `color` | `primary` | `primary` (unchanged) |

No layout changes inside `RestTimerBanner.kt` — only color token substitutions.

### 2. `WorkoutScreen.kt`

The `AnimatedVisibility` block wrapping `RestTimerBanner` gains `Modifier.padding(horizontal = 16.dp)` so the banner aligns with the 16 dp horizontal padding used by the `LazyColumn` content.

```
AnimatedVisibility(
    modifier = Modifier.padding(horizontal = 16.dp),   // ADD THIS
    visible = restTimer != null,
    ...
)
```

### 3. `ExerciseCard.kt`

The inner `Column` of the active set section (the one with `padding(start = 15.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)`) gains a background modifier:

```kotlin
.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
```

Applied to the `Column` only — not the outer `Box`. The outer `Box` retains `drawBehind` for the 3 dp `primary` left border. Card `containerColor` remains `surfaceVariant` (unchanged).

### 4. `WorkoutStepperCard.kt`

Inside `StepperCard`, the button `Row`:

- Replace `Button` (`+`) with `FilledTonalButton` — both buttons are now the same type and receive the same container color from the theme.
- Add `Modifier.weight(1f)` to both buttons so they share equal width regardless of label content.

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    FilledTonalButton(
        onClick = onDecrement,
        shape = CircleShape,
        modifier = Modifier.height(40.dp).weight(1f)   // weight(1f) added
    ) {
        Text("−", ...)
    }
    FilledTonalButton(                                  // was Button
        onClick = onIncrement,
        shape = CircleShape,
        modifier = Modifier.height(40.dp).weight(1f)   // weight(1f) added
    ) {
        Text("+", ...)
    }
}
```

---

## Files Changed

| File | Change |
|---|---|
| `ui/RestTimerBanner.kt` | Color token substitutions (4 properties) |
| `ui/WorkoutScreen.kt` | Add `padding(horizontal = 16.dp)` to `AnimatedVisibility` modifier |
| `ui/ExerciseCard.kt` | Add `primaryContainer.copy(alpha = 0.25f)` background to active set `Column` |
| `ui/WorkoutStepperCard.kt` | Replace `Button` with `FilledTonalButton`; add `weight(1f)` to both buttons |

---

## Non-Goals

- No change to the gradient CTA button (`Purple40 → Pink40`) — kept as-is.
- No change to `Back` / `Skip Exercise` / `Swap` button types — still `FilledTonalButton`.
- No new dependencies.
- No ViewModel or state changes.
- No test changes (no logic changed).
