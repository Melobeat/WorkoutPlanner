# Localization Design — WorkoutPlanner

**Date:** 2026-04-11  
**Status:** Approved  
**Scope:** Full string extraction, English + German, all UI files

---

## Overview

Add localization support using Android's standard resource system. Extract all user-visible strings from Kotlin/Compose source files into `res/values/strings.xml` (English default) and `res/values-de/strings.xml` (German). No new dependencies required.

---

## Architecture

### Resource Files

| File | Purpose |
|---|---|
| `app/src/main/res/values/strings.xml` | English strings (default/fallback) |
| `app/src/main/res/values-de/strings.xml` | German translations |

Android resolves the correct file automatically based on device locale. If no German string exists for a key, the default (English) is used as fallback — this prevents crashes during incremental development.

### Kotlin Usage Pattern

**Before:**
```kotlin
Text("Cancel Workout?")
```

**After:**
```kotlin
Text(stringResource(R.string.dialog_cancel_workout_title))
```

**With format arguments:**
```kotlin
// strings.xml: <string name="workout_exercise_counter">EXERCISE %1$d OF %2$d</string>
Text(stringResource(R.string.workout_exercise_counter, currentIndex, totalCount))
```

All composables already have access to `stringResource` via Compose's standard `androidx.compose.ui.res.stringResource` import. No additional setup needed.

---

## String Key Naming Convention

Keys use `snake_case` with a prefix indicating their context:

| Prefix | Used for | Example |
|---|---|---|
| `action_` | Buttons and actionable labels | `action_cancel`, `action_save` |
| `unit_` | Measurement units and abbreviations | `unit_kg`, `unit_reps`, `unit_seconds` |
| `home_` | HomeScreen strings | `home_no_active_routine` |
| `workout_` | WorkoutScreen + ExerciseCard | `workout_finish_button` |
| `summary_` | WorkoutSummaryScreen | `summary_total_volume` |
| `history_` | HistoryScreen | `history_empty_state` |
| `settings_` | SettingsScreen | `settings_title` |
| `timer_` | TimerSettingsScreen | `timer_easy_set_rest` |
| `profile_` | ProfileScreen | `profile_title` |
| `exercises_` | ExercisesScreen | `exercises_title` |
| `equipment_` | EquipmentScreen | `equipment_title` |
| `routines_` | RoutinesScreen + RoutineDetailScreen | `routines_title` |
| `create_routine_` | CreateRoutineScreen | `create_routine_title_new` |
| `rest_timer_` | RestTimerBanner | `rest_timer_label` |
| `dialog_` | Dialog titles and bodies | `dialog_delete_routine_title` |
| `cd_` | contentDescriptions (accessibility) | `cd_back`, `cd_collapse` |
| `nav_` | Navigation bar labels | `nav_home`, `nav_history` |

### Shared Keys (used in multiple files, defined once)

These reduce total key count and ensure consistent translations:

```
action_cancel        → "Cancel" / "Abbrechen"
action_save          → "Save" / "Speichern"
action_delete        → "Delete" / "Löschen"
action_back_cd       → "Back" (contentDescription) / "Zurück"
action_add           → "Add" / "Hinzufügen"
unit_kg              → "kg" / "kg"
unit_reps            → "Reps" / "Wdh."
unit_seconds_suffix  → "s" / "s"
label_amrap          → "AMRAP" / "AMRAP"
```

---

## German Translation Decisions

| English | German | Notes |
|---|---|---|
| "Workout" | "Workout" | Loanword, standard in German fitness |
| "AMRAP" | "AMRAP" | Universal gym abbreviation |
| "Reps" | "Wdh." | Standard German gym abbreviation for Wiederholungen |
| "Sets" | "Sätze" | Standard German |
| "kg" | "kg" | Same in both languages |
| "s" (seconds suffix) | "s" | Same in both languages |
| "REST" | "PAUSE" | Standard German gym term |
| "UP NEXT" | "ALS NÄCHSTES" | |
| "ACTIVE" | "AKTIV" | |
| "Skipped" | "Übersprungen" | |
| "Save & Exit" | "Speichern & Beenden" | |
| "No Active Routine" | "Kein aktives Programm" | |
| "Manage Routines" | "Programme verwalten" | "Routinen" also acceptable |

Dynamic strings use `%1$d` / `%1$s` / `%2$d` positional placeholders to allow German word order flexibility.

---

## Scope

### In Scope — All UI Files

| File | String count (approx.) |
|---|---|
| `HomeScreen.kt` | ~15 |
| `WorkoutScreen.kt` | ~12 |
| `ExerciseCard.kt` | ~18 |
| `WorkoutSummaryScreen.kt` | ~8 |
| `HistoryScreen.kt` | ~8 |
| `SettingsScreen.kt` | ~15 |
| `TimerSettingsScreen.kt` | ~10 |
| `ProfileScreen.kt` | ~7 |
| `ExercisesScreen.kt` | ~15 |
| `EquipmentScreen.kt` | ~12 |
| `RoutinesScreen.kt` | ~10 |
| `RoutineDetailScreen.kt` | ~5 |
| `CreateRoutineScreen.kt` | ~8 |
| `RestTimerBanner.kt` | ~5 |
| `ExerciseSelectionDialog.kt` | ~2 |
| `RoutineExercisePicker.kt` | ~2 |
| `RoutineExerciseEditItem.kt` | ~6 |
| `WorkoutStepperCard.kt` | ~3 |
| `MainActivity.kt` | ~3 |

**Estimated total unique keys (after shared key deduplication):** ~130

### Out of Scope

- ViewModel files — no user-visible strings
- Data layer — no user-visible strings
- Navigation routes — not user-visible
- `themes.xml` — no translatable content
- Seed JSON assets (`exercises.json`, `equipment.json`) — not runtime UI strings
- Schema JSON files — not runtime UI strings

---

## Implementation Order

1. Create `res/values-de/strings.xml` (empty scaffold)
2. Extract shared keys first — update `res/values/strings.xml`, populate both locale files
3. Extract per-file, in this order:
   - `HomeScreen.kt`
   - `WorkoutScreen.kt` + `ExerciseCard.kt` (active workout flow)
   - `WorkoutSummaryScreen.kt`
   - `HistoryScreen.kt`
   - `SettingsScreen.kt` + `TimerSettingsScreen.kt`
   - `ProfileScreen.kt`
   - `ExercisesScreen.kt`
   - `EquipmentScreen.kt`
   - `RoutinesScreen.kt` + `RoutineDetailScreen.kt`
   - `CreateRoutineScreen.kt` + `RoutineExerciseEditItem.kt` + `RoutineExercisePicker.kt`
   - `RestTimerBanner.kt` + `ExerciseSelectionDialog.kt` + `WorkoutStepperCard.kt`
   - `MainActivity.kt`
4. Build verification after each major file group
5. Final: `./gradlew assembleDebug` + `./gradlew :app:testDebugUnitTest`

---

## Testing

- Unit tests: No changes required — no strings are tested directly
- Manual verification: Switch device/emulator locale to German (`de`) and run through all screens
- Build check: `./gradlew :app:lintDebug` will flag any missing string references
- Both `strings.xml` files must have identical key sets — missing keys fall back to English silently, but this should be verified

---

## Non-Goals

- RTL language support (not needed for EN/DE)
- Plural strings (`<plurals>`) — the current UI uses strings like "${routine.workoutDays.size} days" and "${exercise.sets.size} sets done", but German pluralization (1 Tag / 3 Tage) can be handled with format strings for the initial implementation. Proper `<plurals>` resources can be added as a follow-up if needed.
- Runtime language switching (device locale controls language selection; no in-app language picker)
- Adding additional languages beyond EN and DE at this time
