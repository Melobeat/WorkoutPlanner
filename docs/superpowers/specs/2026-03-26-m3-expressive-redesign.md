# M3 Expressive Redesign

**Date:** 2026-03-26
**Branch:** `feature/m3-expressive-redesign`

## Overview

Full app-wide visual redesign using Material 3 Expressive — spring-physics motion, gradient surfaces, pill shapes, and large touch targets. Every screen is updated for visual coherence. Architecture and data layer are unchanged.

---

## Design Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Personality | M3 Expressive (light surface, vibrant gradient accents) | Modern, polished — fits the workout context without being aggressively dark |
| Navigation tabs | 2 tabs: Home + History | Minimal navigation; Settings accessible via icon |
| Home hero | Full-width gradient banner (edge-to-edge) | Maximum impact on the most-visited screen |
| Active workout | One set at a time, large stepper cards | Thumb-friendly for gym use with sweaty hands |
| Icons | `fitness_center` (Home), `history` (History), `tune` (Settings) | More contextual and refined than defaults |

---

## 1. Theme

**File:** `ui/theme/Theme.kt`

- Add `motionScheme = MotionScheme.expressive()` to `MaterialTheme(...)` — enables spring-physics animations across all M3 components
- Dynamic color remains enabled (Android 12+)
- Fallback `LightColorScheme` / `DarkColorScheme` updated to deep purple primary + pink secondary (matching gradient identity)
- Requires `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` on the theme composable

**File:** `ui/theme/Color.kt`

- Replace boilerplate purple/pink values with a proper M3 seed palette:
  - `Purple10 = Color(0xFF21005D)`, `Purple40 = Color(0xFF6750A4)`, `Purple80 = Color(0xFFD0BCFF)`
  - `Pink40 = Color(0xFFB5488A)`, `Pink80 = Color(0xFFFFB0C8)`

**File:** `ui/theme/Type.kt` — no changes.

**Shapes** — no custom overrides; M3 defaults apply. Pill shapes achieved with `shape = CircleShape` / `RoundedCornerShape(50%)` at call sites.

---

## 2. Home Screen

**File:** `ui/HomeScreen.kt`

### Layout

- **No `TopAppBar`** — the gradient hero acts as the combined header + CTA
- **Edge-to-edge gradient hero** bleeds under the status bar (already enabled by `enableEdgeToEdge()` in `MainActivity`)

### Hero banner (routine selected state)

```
gradient background: 150deg, #4a0080 → #6750a4 → #b5488a
├── Row: "Workout Planner" title (white) + `tune` icon button (ghost pill)
├── Label: "ROUTINE NAME · Day X of N" (white, 70% alpha, uppercase)
├── Headline: day name (white, 32sp, weight 900)
├── Subtext: "N exercises" (white, 65% alpha)
├── Exercise chips: frosted glass pills (15% white bg)
└── Buttons row:
    ├── FilledButton (white bg, purple text): "▶ Start Workout" — flex 1
    └── FilledTonalButton (20% white bg): swap icon — fixed width
```

### Empty state (no routine selected)

- Same gradient hero
- Headline: "No Active Routine"
- Body text: "Select a routine to start tracking your progress."
- `FilledTonalButton`: "Manage Routines" → navigates to Settings

### Recent Workouts section (below hero)

- Section header: "RECENT WORKOUTS" — `labelSmall`, uppercase, `onSurfaceVariant`
- Cards: `surfaceVariant` container (`#f3edf7`), `16.dp` corner radius
  - Leading: workout name (`titleMedium`, weight 800) + date (`bodySmall`)
  - Trailing: duration `SuggestionChip` with `schedule` icon

### Icons

- Home tab: `Icons.Rounded.FitnessCenter`
- History tab: `Icons.Rounded.History`
- Settings button: `Icons.Rounded.Tune`

---

## 3. Active Workout Screen

**File:** `ui/WorkoutScreen.kt`

### Layout change: one set at a time

Current design shows all sets as table rows simultaneously. New design focuses on one set at a time, showing completed sets as a running log below the steppers.

### TopAppBar

- Title: workout day name + elapsed timer subtitle
- Trailing actions: `+ Exercise` tonal pill button, `End` tonal pill button (error color)
- No overflow menu

### Per-exercise layout

```
├── Progress bar: "EXERCISE X OF N" + linear gradient progress indicator
├── Exercise name (headlineSmall, weight 900) + Swap tonal pill button
├── Set dot indicators: filled pill for current set, small dots for remaining
│   + "SET X OF N" label
├── Stepper cards row (two cards, flex):
│   ├── Reps card: white surface, elevation, large number (44sp), − / + pill buttons
│   └── Weight (kg) card: same structure
├── AMRAP toggle: inline Switch + label (visible only on last set)
├── "Done — Next Set" / "Done — Finish Exercise" gradient pill CTA
└── Completed sets log: horizontal row of set chips (filled = done, dashed = pending)
    e.g. "Set 1: 8×80" filled chip, "Set 2: —" dashed chip
```

### Next exercise preview

- Always visible at the bottom: `surfaceVariant` card showing next exercise name, sets×reps, weight
- Hidden when current exercise is the last one

### Active workout mini-bar (MainActivity)

When workout is active but not full-screen, the `bottomBar` in `MainActivity`'s inner `Scaffold` becomes:
- `surfaceVariant` surface, 64dp height
- Leading: `FitnessCenter` icon + workout name + elapsed time
- Trailing: `FilledTonalButton` "Resume"
- Tapping anywhere navigates to `ActiveWorkoutRoute`

---

## 4. History Screen

**File:** `ui/HistoryScreen.kt`

### Layout

- `LargeTopAppBar` with title "History" — collapses on scroll
- `LazyColumn` with date-grouped sections

### Date grouping

Sessions grouped by: "This Week", "Last Week", then month+year (e.g. "March 2026"). Section headers use `labelSmall` uppercase + `onSurfaceVariant`.

### Session cards

- Container: `surfaceVariant`, `cornerRadius = 20.dp`
- Collapsed state (default):
  - Leading: day name (`titleMedium`, weight 800) + date + routine name
  - Trailing: duration `SuggestionChip` with `schedule` icon + `expand_more` chevron
- Expanded state (tap to toggle, spring animation):
  - Same header
  - White inner card showing per-exercise summary: "Exercise Name — N × M reps · W kg"
  - "N more exercises" tonal text link if more than 3

### Empty state

- Centered `Column`: body text + `FitnessCenter` icon illustration

---

## 5. Settings Screen

**File:** `ui/SettingsScreen.kt`

- `LargeTopAppBar` with title "Settings" + back navigation icon
- 3 `ListItem` composables replacing manual rows:
  - `leadingContent`: tonal icon in `surfaceVariant` container
  - `headlineContent` / `supportingContent`: title + subtitle
  - `trailingContent`: `chevron_right` icon
- `HorizontalDivider` between items unchanged

---

## 6. Routines, RoutineDetail, CreateRoutine

**Files:** `ui/RoutinesScreen.kt`, `ui/RoutineDetailScreen.kt`, `ui/CreateRoutineScreen.kt`

- `LargeTopAppBar` on all three
- **RoutinesScreen**: routine list items as `surfaceVariant` cards (16dp radius). Active routine gets a `primary` color left border accent (4dp). FAB replaced by `ExtendedFloatingActionButton` with `add` icon + "New Routine" label.
- **RoutineDetailScreen**: day cards in `surfaceVariant` containers; exercise chips as `InputChip`.
- **CreateRoutineScreen**: day containers as `OutlinedCard`; primary action button as full-width `FilledButton` (pill shape). Secondary actions as `TextButton`.

---

## 7. Exercises & Equipment Screens

**Files:** `ui/ExercisesScreen.kt`, `ui/EquipmentScreen.kt`

- `LargeTopAppBar` with title
- `ExtendedFloatingActionButton` for add action (replaces icon button in TopAppBar)
- List items: `ListItem` composable with `trailingContent` delete icon button
- Filter/dropdown stays as-is (ExercisesScreen only)

---

## Implementation Notes

- All `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` annotations required for `MotionScheme.expressive()` and any Expressive components used
- `LargeTopAppBar` requires `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()` + `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)` on the `Scaffold`
- Icons use `Icons.Rounded.*` from `material-icons-extended` (already in dependencies)
- Gradient backgrounds use `Brush.linearGradient(...)` passed to `Modifier.background(brush)`
- Spring animations on expandable history cards: `AnimatedVisibility` with `expandVertically` + `shrinkVertically` using `spring(dampingRatio = Spring.DampingRatioMediumBouncy)`
- No new dependencies required — all M3 Expressive components are in the existing `compose-bom:2026.03.01`