# Design: Seed Data Loading Fix + FAB Visual Fix

**Date:** 2026-04-10  
**Scope:** Two bug fixes — seed data not loading on fresh install, and FABs visually invisible in Settings screens.

---

## Bug 1 — Seed Data Not Loading

### Root Cause

`WorkoutDatabase.kt` `onCreate` callback (line 45) calls `getDatabase(databaseContext)` recursively before `Instance` has been assigned. The `.also { Instance = it }` assignment runs synchronously after `.build()` returns, but the callback coroutine references `getDatabase()` which finds `Instance == null` and builds a second, orphaned `WorkoutDatabase`. That second instance gets seeded; the primary instance (returned to callers) is never seeded.

Additionally, `fallbackToDestructiveMigrationFrom(1,2,3,4,5,6,7,8)` includes `8`, the current DB version. This is meaningless (Room cannot destructively migrate from version N to the same version N) and should be removed to avoid confusion.

### Fix

Replace the recursive call with a direct reference to `Instance!!`:

```kotlin
// Before
val dao = getDatabase(databaseContext).workoutDao()

// After
val dao = Instance!!.workoutDao()
```

This is safe because `Instance` is assigned synchronously by `.also { Instance = it }` before the `CoroutineScope(Dispatchers.IO).launch` block can execute on the IO thread.

Remove `8` from the `fallbackToDestructiveMigrationFrom(...)` argument list.

### Scope

- File: `app/src/main/java/de/melobeat/workoutplanner/data/WorkoutDatabase.kt`
- Lines affected: 38, 45
- No migration bump required. No data loss for existing users.
- Seeding only fires on a fresh install (`onCreate`). Existing users with empty DB are unaffected.

---

## Bug 2 — FABs Look Wrong in Settings Screens

### Root Cause

All three Settings-area list screens use `ExtendedFloatingActionButton` without explicit color parameters. M3's default `containerColor` for `ExtendedFloatingActionButton` is `colorScheme.primaryContainer`. In this app's dark theme, `primaryContainer = Color(0x2427AE60)` — 14% alpha emerald green — which is nearly invisible against the `#0A0E0B` background.

### Fix

Explicitly pass colors to all three FABs:

```kotlin
ExtendedFloatingActionButton(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    ...
)
```

`primary` = solid emerald green (`#27AE60` dark / `#16A34A` light).  
`onPrimary` = white.

This matches the color role assignment in AGENTS.md: "`primary`/`primaryContainer` — Active/selected states, CTA buttons."

### Screens affected

| File | FAB text | Line |
|---|---|---|
| `ui/RoutinesScreen.kt` | "New Routine" | 74 |
| `ui/ExercisesScreen.kt` | "Add Exercise" | 79 |
| `ui/EquipmentScreen.kt` | "Add Equipment" | 71 |

---

## Fix 3 — Design Guidelines Doc Corrections

`docs/design-guidelines.md` contains several references to `Icons.Default.*` which is banned per AGENTS.md. These are doc-only corrections; the screens already use `Icons.Rounded.*`.

| Line | Current | Corrected |
|---|---|---|
| 367 | `Icons.Default.Add` | `Icons.Rounded.Add` |
| 561 | `Icons.Default.Add` | `Icons.Rounded.Add` |
| 564 | `Icons.Default.CheckCircle` | `Icons.Rounded.CheckCircle` |
| 565 | `Icons.Default.RadioButtonUnchecked` | `Icons.Rounded.RadioButtonUnchecked` |
| 572 | `Icons.Default.Edit` | `Icons.Rounded.Edit` |

---

## Out of Scope

- Adding seed-on-empty startup check (Option B) — not requested.
- DB version bump / destructive migration (Option C) — not requested.
- Any other icon family audit beyond the five lines above.
