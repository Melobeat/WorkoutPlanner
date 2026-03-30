# Rest Timer — Design Spec

**Date:** 2026-03-30
**Status:** Approved

---

## Overview

A rest timer that starts automatically after each completed set or skipped exercise. It counts up and fires haptic + visual notifications at configurable milestones to prompt the user when it's time to start their next set. The timer is informational only — the user decides when to continue.

---

## 1. State & ViewModel

### New types in `ActiveWorkoutViewModel.kt`

```kotlin
enum class RestTimerContext { BetweenSets, BetweenExercises }

data class RestTimerUiState(
    val elapsedSeconds: Int = 0,
    val context: RestTimerContext,
    val easyThresholdSeconds: Int,
    val hardThresholdSeconds: Int,
    val singleThresholdSeconds: Int
)

sealed class RestTimerEvent {
    object EasyMilestone : RestTimerEvent()
    object HardMilestone : RestTimerEvent()
    object ExerciseMilestone : RestTimerEvent()
}
```

- `BetweenSets` uses `easyThresholdSeconds` (default 90) and `hardThresholdSeconds` (default 180).
- `BetweenExercises` uses `singleThresholdSeconds` (default 60).

### `ActiveWorkoutUiState` change

```kotlin
val restTimer: RestTimerUiState? = null   // null = not resting
```

### Event channel

`ActiveWorkoutViewModel` exposes:

```kotlin
val restTimerEvents: SharedFlow<RestTimerEvent>
```

The workout screen collects this via `LaunchedEffect` to trigger vibration and update the banner label.

### Timer lifecycle

| Trigger | Effect |
|---|---|
| `completeCurrentSet()` (mid-exercise) | Cancel previous rest job → start `BetweenSets` rest timer |
| `completeCurrentSet()` (last set of exercise, not last exercise) | Cancel previous rest job → start `BetweenExercises` rest timer |
| `completeCurrentSet()` (last set, last exercise) | No rest timer — calls `requestFinish()` |
| `skipExercise()` | Cancel previous rest job → start `BetweenExercises` rest timer (if not last exercise) |
| Next `completeCurrentSet()` call while resting | Cancel rest job, null out `restTimer`, then proceed normally |
| `cancelWorkout()` / `requestFinish()` | Cancel rest job, null out `restTimer` |

The rest timer job increments `restTimer.elapsedSeconds` every second. At each threshold it emits the corresponding `RestTimerEvent` via the SharedFlow.

### Settings integration

`ActiveWorkoutViewModel` receives a `RestTimerPreferencesRepository` via Hilt injection and reads the current settings once when constructing a rest timer, so thresholds reflect whatever the user has configured.

---

## 2. Settings Persistence

### `RestTimerSettings` data class (in `data/`)

```kotlin
data class RestTimerSettings(
    val betweenSetsEasySeconds: Int = 90,
    val betweenSetsHardSeconds: Int = 180,
    val betweenExercisesSeconds: Int = 60
)
```

### `RestTimerPreferencesRepository` (in `data/`)

- Backed by `Preferences DataStore` (not Room — no schema change needed).
- Exposes `Flow<RestTimerSettings>` and `suspend fun update(settings: RestTimerSettings)`.
- Three `Preferences.Key<Int>` keys with the defaults above.

### DI (`DatabaseModule.kt`)

Two new `@Provides` entries:
1. `DataStore<Preferences>` singleton via `PreferenceDataStoreFactory`
2. `RestTimerPreferencesRepository` singleton

---

## 3. UI — Rest Timer Banner

**Location:** `WorkoutScreen.kt`, between the Done CTA and the Back/Skip navigation row.

**Visibility:** `AnimatedVisibility(visible = uiState.restTimer != null, enter = expandVertically(), exit = shrinkVertically())`

### Banner layout

A `surfaceVariant` card (16 dp radius) containing:

| Element | Spec |
|---|---|
| Elapsed time | `displaySmall`, weight 900 — e.g. `01:23` |
| Milestone progress track | `LinearProgressIndicator`, `primary` fill. Fills 0→easy threshold; after easy fires it resets and fills 0→hard threshold; stays full after the last threshold is passed |
| Milestone label | `labelSmall`, uppercase, `onSurfaceVariant` — updates per milestone (see table below) |

### Milestone labels

| State | Label |
|---|---|
| Elapsed < easy threshold | *(empty — just shows timer)* |
| Elapsed ≥ easy threshold (BetweenSets) | `"EASY? TIME TO GO"` |
| Elapsed ≥ hard threshold (BetweenSets) | `"HARD? TIME TO GO"` |
| Elapsed ≥ single threshold (BetweenExercises) | `"READY FOR NEXT EXERCISE?"` |

### Vibration

Collected from `restTimerEvents` SharedFlow in `WorkoutScreen` via `LaunchedEffect`:

```kotlin
LaunchedEffect(Unit) {
    viewModel.restTimerEvents.collect { event ->
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 100, 50), -1))
    }
}
```

Uses `Vibrator` from `LocalContext.current.getSystemService(Vibrator::class.java)`. No system notification permission required.

---

## 4. Settings Screen

### Nav changes

- New `@Serializable object TimerSettingsRoute` in `NavRoutes.kt`
- New composable destination in `WorkoutNavGraph`
- `SettingsScreen` gets an `onNavigateToTimerSettings: () -> Unit` callback
- New `SettingsListItem` entry: "Timer Settings" with a timer icon

### `TimerSettingsScreen`

- `LargeTopAppBar` (collapses on scroll), paired with `exitUntilCollapsedScrollBehavior` per design guidelines
- Three `ListItem` rows, one per threshold:
  - "Easy set rest" — default 90s
  - "Hard set rest" — default 3:00
  - "Between exercises" — default 1:00
- Each row shows the current value formatted as `m:ss`. Tapping opens a simple dialog with a numeric input (seconds) or a time picker.
- Changes persist immediately via `TimerSettingsViewModel` → `RestTimerPreferencesRepository`

### `TimerSettingsViewModel`

- `@HiltViewModel`, injected with `RestTimerPreferencesRepository`
- Exposes `StateFlow<RestTimerSettings>` and `fun update(...)` functions per field

---

## 5. Testing

### `ActiveWorkoutViewModelTest` additions

1. **Milestone emission** — start workout, call `completeCurrentSet()` (mid-exercise), advance fake clock to `easyThreshold + 1` seconds, assert `RestTimerEvent.EasyMilestone` emitted from `restTimerEvents`
2. **Timer reset on next set** — assert `uiState.restTimer` is `null` after calling `completeCurrentSet()` while a rest timer is running
3. **Between-exercises context** — complete last set of a non-final exercise, assert `uiState.restTimer?.context == RestTimerContext.BetweenExercises`

### `RestTimerPreferencesRepositoryTest`

- Write updated values → collect from `Flow<RestTimerSettings>` → assert roundtrip matches
- Uses in-memory DataStore via `PreferenceDataStoreFactory` with a temp file

---

## 6. Files Changed / Created

| File | Change |
|---|---|
| `ui/ActiveWorkoutViewModel.kt` | Add `RestTimerUiState`, `RestTimerContext`, `RestTimerEvent`, `restTimerEvents` SharedFlow, rest timer job logic |
| `ui/WorkoutScreen.kt` | Add rest timer banner composable, vibration `LaunchedEffect` |
| `ui/TimerSettingsScreen.kt` | New file — `TimerSettingsScreen` + `TimerSettingsViewModel` (co-located per project convention) |
| `data/RestTimerPreferencesRepository.kt` | New file |
| `ui/navigation/NavRoutes.kt` | Add `TimerSettingsRoute` |
| `ui/navigation/WorkoutNavGraph.kt` | Wire `TimerSettingsRoute` destination |
| `ui/SettingsScreen.kt` | Add "Timer Settings" list item + nav callback |
| `di/DatabaseModule.kt` | Add DataStore + `RestTimerPreferencesRepository` providers |
| `test/.../ActiveWorkoutViewModelTest.kt` | Three new test cases |
| `test/.../RestTimerPreferencesRepositoryTest.kt` | New test file |