# WorkoutPlanner Design Guidelines

**Last updated:** 2026-03-28
**Status:** Authoritative — supersedes all individual spec files in `docs/superpowers/specs/`

This is the single source of truth for the app's visual design, component patterns, and interaction conventions. Read this before implementing any new screen or modifying an existing one.

---

## 1. App Identity

### Personality

**Bold, expressive, and purposeful.** WorkoutPlanner is a focused fitness tool — not a social app or a game. The design should feel confident and modern without being aggressive or clinical. M3 Expressive motion and gradient surfaces signal energy and polish; large touch targets and clear hierarchy serve gym use with sweaty hands.

### Color Story

Deep purple is the app's primary color story. Two gradient variants carry this identity:

- **Home hero gradient:** `150° #4A0080 → #6750A4 → #B5488A` (purple to pink) — used only on the Home screen hero banner.
- **Launcher icon gradient:** `135° #1E003E → #3D0070` (deep purple to mid-purple) — used only on the launcher icon background.

The launcher icon foreground is an acid green (`#C8FF00`) outlined dumbbell. The acid green is exclusive to the icon and must not appear in-app.

### Launcher Icon

| Dimension | Decision | Rationale |
|---|---|---|
| Symbol | Outlined dumbbell | Universal fitness symbol; legible at 48px |
| Treatment | Line art (fill none) | Clean, modern; works at all sizes |
| Background | Deep purple gradient `#1E003E → #3D0070` (135°) | Distinctive in launcher; echoes app theme |
| Foreground stroke | Acid green `#C8FF00`, 3.5px on 108×108 canvas | Stands out; strong personality |
| Themed icon (Android 13+) | White outline on mid-grey | System tints automatically; no separate asset needed |

Dumbbell geometry (108×108 canvas):

| Element | x | y | width | height | rx |
|---|---|---|---|---|---|
| Left weight plate | 12 | 44 | 18 | 20 | 5 |
| Left collar | 30 | 36 | 9 | 36 | 3 |
| Bar | 39 | 48 | 30 | 12 | 3 |
| Right collar | 69 | 36 | 9 | 36 | 3 |
| Right weight plate | 78 | 44 | 18 | 20 | 5 |

---

## 2. Color Tokens

### M3 Seed Palette (fallback — dynamic color enabled on Android 12+)

| Token | Value | Role |
|---|---|---|
| `Purple10` | `#21005D` | Deep background tint |
| `Purple40` | `#6750A4` | M3 `primary` |
| `Purple80` | `#D0BCFF` | M3 `primaryContainer` / on-dark primary |
| `Pink40` | `#B5488A` | M3 `secondary` |
| `Pink80` | `#FFB0C8` | M3 `secondaryContainer` |
| `surfaceVariant` | `#F3EDF7` | All card backgrounds |

### Gradient Identity

```
150deg · #4A0080 → #6750A4 → #B5488A
```

Applied via `Brush.linearGradient(...)` on `Modifier.background(brush)`. **Used only on the Home screen hero banner.**

### Semantic Usage Rules

| Token / Value | Used for | Rule |
|---|---|---|
| `primary` | Primary CTAs, active tab indicator, active routine left-border accent | Never use raw hex — always use `MaterialTheme.colorScheme.primary` |
| `surfaceVariant` | All card backgrounds | All cards must use `surfaceVariant` — never a custom background color |
| `onSurfaceVariant` | Section header labels | Uppercase `labelSmall` section headers only |
| `error` | "End" workout button | Nowhere else in the app |
| White @ 15–20% alpha | Frosted-glass pills and secondary buttons on the gradient hero | Only on gradient backgrounds |

---

## 3. Typography

`ui/theme/Type.kt` has no custom overrides — M3 defaults (Roboto) apply everywhere.

### Type Roles in Use

| Role | Weight | Usage |
|---|---|---|
| `headlineSmall` | 900 | Workout day name (hero), active workout screen headline |
| `titleMedium` | 800 | Card primary text — routine name, history entry title |
| `bodyMedium` | default | Exercise descriptions, card body text |
| `bodySmall` | default | Dates, durations, secondary metadata |
| `labelSmall` | 600, uppercase | Section headers ("RECENT WORKOUTS", "EXERCISE 1 OF 5") |

### Special: Stepper Numbers

Reps and weight values in the workout stepper use `44sp` / weight `900` — larger than any standard M3 role. They must be readable at arm's length in a gym.

### White-on-gradient text

- Title / primary text: `white` at 100% alpha
- Sub-labels / supporting text: `white` at 65–70% alpha

### Conventions

- **Weight 900** is reserved for hero and action headlines only (day name, stepper numbers).
- **Uppercase labels** are always `labelSmall` + `onSurfaceVariant` + letter-spacing — used for section headers only. Never use uppercase for body text.
- No custom font family. Do not add one.

---

## 4. Shape, Spacing & Touch Targets

### Shape Tokens

| Shape | Value | Applied to |
|---|---|---|
| Pill | `CircleShape` / `RoundedCornerShape(50%)` | All buttons, stepper ± controls, set dot indicators, nav pills |
| Large card | 20 dp | History session cards, workout stepper cards |
| Standard card | 16 dp | Routine list cards, recent workout cards on Home |
| Chip | M3 default | `SuggestionChip`, `InputChip`, set chips |

**Rule: actions are pills, containers are rounded rectangles.** No square or lightly-rounded buttons. Shape is never overridden in `Theme.kt` — apply at call sites.

### Spacing

| Context | Value |
|---|---|
| Screen horizontal content padding | 16 dp |
| Card vertical spacing in lists | 8–12 dp |
| Section header bottom margin | 8 dp |

### Touch Targets (gym-use priority)

| Element | Min size | Rationale |
|---|---|---|
| All interactive elements | 48 × 48 dp | M3 default; gloves/sweat |
| Stepper cards (reps, weight) | ~120 dp height | Readable at arm's length; thumb-friendly |
| Active workout mini-bar | 64 dp height | Always reachable from bottom of screen |

---

## 5. Motion

### M3 Expressive Spring Physics

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
MaterialTheme(
    motionScheme = MotionScheme.expressive(),
    ...
)
```

Spring physics apply automatically to all M3 components (buttons, dialogs, bottom sheets). **Do not override with custom easing curves or duration values** — spring-based animation has no fixed duration by design.

### Manual Animations

| Pattern | Usage |
|---|---|
| `AnimatedVisibility` with `expandVertically` + `shrinkVertically`, `spring(dampingRatio = Spring.DampingRatioMediumBouncy)` | History card expand/collapse |

No other manual animation specs are defined. Default to M3 Expressive motion for anything new.

---

## 6. Component Patterns

### Buttons

All buttons use pill shape (`CircleShape`). No square or lightly-rounded buttons anywhere in the app.

| Variant | When to use |
|---|---|
| `FilledButton` (white bg, primary text) | Primary CTA on gradient hero (e.g. "Start Workout") |
| `FilledButton` (gradient pill) | Active workout "Done — Next Set" / "Done — Finish Exercise" CTA |
| `FilledTonalButton` | All secondary actions: Back, Skip Exercise, Swap exercise, Resume |
| `FilledTonalButton` with `error` color | "End" workout only |
| `TextButton` | Low-priority secondary actions in CreateRoutine |
| `ExtendedFloatingActionButton` | Add actions on list screens (Routines, Exercises, Equipment) |

### App Bars

| Type | Used on |
|---|---|
| None (gradient hero replaces it) | Home screen |
| `TopAppBar` (standard) | Workout screen — title is workout day name, subtitle is elapsed timer |
| `LargeTopAppBar` (collapses on scroll) | History, Settings, Routines, RoutineDetail, CreateRoutine, Exercises, Equipment |

`LargeTopAppBar` must always be paired with:
- `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()`
- `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)` on the `Scaffold`

### Cards

| Pattern | Usage |
|---|---|
| `surfaceVariant` card, 20 dp radius | History session cards (expandable via `AnimatedVisibility`) |
| `surfaceVariant` card, 16 dp radius | Recent workout cards (Home), routine list items |
| Active routine: 4 dp `primary` left border | Highlights the current active routine in the list |
| `OutlinedCard` | Day containers in CreateRoutine |

**Rule:** All cards use `surfaceVariant` as their container color. Never use a custom background color for cards.

### List Items

Use `ListItem` composable (not manual rows) for:
- Settings screen rows — `leadingContent` with tonal icon in `surfaceVariant` container, `trailingContent` with `chevron_right`
- Exercise and equipment list rows — `trailingContent` with delete icon button

### Chips

| Type | Usage |
|---|---|
| `SuggestionChip` with `schedule` icon | Duration on history/home workout cards |
| `InputChip` | Exercises within a routine day (RoutineDetail) |
| Frosted-glass pills (15% white bg) | Exercise name chips on Home hero |
| Set chips (filled/dashed) | Completed sets log in workout screen |

### Icons

Use `Icons.Rounded.*` from `material-icons-extended` (already in dependencies).

| Icon | Usage |
|---|---|
| `Icons.Rounded.FitnessCenter` | Home tab, active workout mini-bar |
| `Icons.Rounded.History` | History tab |
| `Icons.Rounded.Tune` | Settings button (in Home hero) |
| `Icons.Rounded.Add` | FAB on list screens |
| `Icons.Rounded.ChevronRight` | Settings list item trailing |

### Active Workout Mini-Bar

Shown in `MainActivity`'s inner `Scaffold` `bottomBar` when `isActive && !isFullScreen`.

- `surfaceVariant` surface, 64 dp height
- Leading: `FitnessCenter` icon + workout name (`titleMedium` weight 700) + elapsed time (`bodySmall`)
- Trailing: `FilledTonalButton` "Resume" (pill)
- Full row is tappable → navigates to `ActiveWorkoutRoute`

---

## 7. Navigation Structure

### Tab Layout

Two tabs using `NavigationSuiteScaffold`:

| Tab | Icon | Route |
|---|---|---|
| Home | `Icons.Rounded.FitnessCenter` | `HomeRoute` |
| History | `Icons.Rounded.History` | `HistoryRoute` |

Settings is reachable via the `tune` icon button in the Home hero — not a tab. All other screens are back-stack destinations.

### Back Stack Behavior

| Trigger | Behavior |
|---|---|
| Minimize workout screen | Pops back stack; `DisposableEffect` in `WorkoutNavGraph` sets `isFullScreen = false` → mini-bar appears |
| Tap mini-bar / Resume | Navigates to `ActiveWorkoutRoute` |
| Last set completed | `requestFinish()` → `LaunchedEffect(uiState.showSummary)` navigates to `WorkoutSummaryRoute` |
| Summary → Confirm finish | Persists to DB, sets `isFinished = true`, pops to Home |
| Summary → Resume | Resets `showSummary = false`, returns to workout screen |

### UDF Callback Convention

Composables receive lambdas and **never hold a `NavController` reference**:

```kotlin
// ✓ Correct
fun HomeScreenContent(
    onStartWorkout: () -> Unit,
    onNavigateToRoutines: () -> Unit,
)

// ✗ Wrong — composables must not hold NavController references
fun HomeScreen(navController: NavController)
```

One-shot async outcomes (workout finished) are observed via `LaunchedEffect`, never directly in the composable body.

### ViewModel Creation Rule

- All `@HiltViewModel` screen ViewModels inside `NavHost` destinations must use `hiltViewModel()`
- Only `ActiveWorkoutViewModel` uses `viewModel(viewModelStoreOwner = LocalActivity.current)` — it is scoped to the Activity, not the back stack entry
- Using `viewModel()` for any Hilt ViewModel inside `NavHost` will crash at runtime

---

## 8. Screen-by-Screen Designs

### Home Screen (`ui/HomeScreen.kt`)

No `TopAppBar` — the gradient hero acts as the combined header and CTA.

**Hero banner (routine selected):**
```
gradient background: 150° #4A0080 → #6750A4 → #B5488A
├── Row: "Workout Planner" title (white) + tune icon button (ghost pill)
├── Label: "ROUTINE NAME · Day X of N" (white 70% alpha, uppercase, labelSmall)
├── Headline: day name (headlineSmall, weight 900, white)
├── Subtext: "N exercises" (white 65% alpha)
├── Exercise chips: frosted-glass pills (15% white bg)
└── Button row:
    ├── FilledButton (white bg, purple text): "▶ Start Workout" — flex 1
    └── FilledTonalButton (20% white bg): swap icon — fixed width
```

**Hero banner (no routine selected):**
- Same gradient
- Headline: "No Active Routine"
- Body: "Select a routine to start tracking your progress."
- `FilledTonalButton`: "Manage Routines" → navigates to Settings

**Recent Workouts section (below hero):**
- Section header: "RECENT WORKOUTS" — `labelSmall`, uppercase, `onSurfaceVariant`
- Cards: `surfaceVariant`, 16 dp radius
  - Leading: workout name (`titleMedium` weight 800) + date (`bodySmall`)
  - Trailing: duration `SuggestionChip` with `schedule` icon

---

### Workout Screen (`ui/WorkoutScreen.kt`)

One set at a time — completed sets shown as a running log below the steppers.

**TopAppBar:** workout day name + elapsed timer subtitle. Trailing: `+ Exercise` tonal pill, `End` tonal pill (error color).

**Per-exercise layout:**
```
├── Progress bar: "EXERCISE X OF N" label + linear progress indicator
├── Exercise name (headlineSmall, weight 900) + Swap tonal pill button
├── Set dot indicators (filled pill = current, small dots = remaining) + "SET X OF N" label
├── Stepper cards row (flex, two equal cards):
│   ├── Reps card: surfaceVariant, 20dp radius, 44sp weight-900 number, − / + pill buttons
│   └── Weight (kg) card: same structure
├── AMRAP toggle: inline Switch + label (visible only on last set)
├── Gradient CTA pill: "Done — Next Set" / "Done — Finish Exercise"
├── Navigation row:
│   ├── FilledTonalButton "← Back" (disabled at exercise 0, set 0)
│   └── FilledTonalButton "Skip Exercise →" (always enabled; last exercise triggers requestFinish)
└── Completed sets log: row of set chips — filled = done, dashed = pending
    e.g. "Set 1: 8×80" filled, "Set 2: —" dashed
```

**Next exercise preview:** `surfaceVariant` card at bottom — always visible unless current exercise is last.

---

### Workout Summary Screen (`ui/WorkoutSummaryScreen.kt`)

No ViewModel of its own. Reads `ActiveWorkoutViewModel` via `viewModel(viewModelStoreOwner = LocalActivity.current)` — same Activity-scoped pattern as `WorkoutScreen`.

Shows per-exercise set log. Sets with `isDone = false` render as "Skipped". Actions: Resume (→ `resumeWorkout()`) and Confirm Finish (→ `finishWorkout()`).

---

### History Screen (`ui/HistoryScreen.kt`)

`LargeTopAppBar` "History" + `LazyColumn` with date-grouped sections.

**Date groups:** "This Week", "Last Week", then month+year (e.g. "March 2026"). Headers: `labelSmall` uppercase `onSurfaceVariant`.

**Session cards** (`surfaceVariant`, 20 dp radius):
- Collapsed: workout name (`titleMedium` weight 800) + date + routine name + duration `SuggestionChip` + `expand_more` chevron
- Expanded: same header + white inner card with per-exercise summary ("Exercise — N × M reps · W kg") + "N more exercises" tonal text link if > 3
- Toggle: spring `AnimatedVisibility` (`DampingRatioMediumBouncy`)

**Empty state:** centered `Column` with body text + `FitnessCenter` icon.

---

### Settings Screen (`ui/SettingsScreen.kt`)

`LargeTopAppBar` "Settings" + back navigation icon. Three `ListItem` rows:
- `leadingContent`: tonal icon in `surfaceVariant` container
- `headlineContent` / `supportingContent`: title + subtitle
- `trailingContent`: `chevron_right`
- `HorizontalDivider` between items

---

### Routines Screen (`ui/RoutinesScreen.kt`)

`LargeTopAppBar`. Routine list as `surfaceVariant` cards (16 dp radius). Active routine has a 4 dp `primary` left border accent. `ExtendedFloatingActionButton` "New Routine" (add icon).

---

### Routine Detail Screen (`ui/RoutineDetailScreen.kt`)

`LargeTopAppBar`. Day cards in `surfaceVariant` containers. Exercises within each day as `InputChip`.

---

### Create Routine Screen (`ui/CreateRoutineScreen.kt`)

`LargeTopAppBar`. Day containers as `OutlinedCard`. Primary action: full-width pill `FilledButton`. Secondary actions: `TextButton`.

---

### Exercises Screen (`ui/ExercisesScreen.kt`)

`LargeTopAppBar`. `ListItem` rows with `trailingContent` delete icon button. `ExtendedFloatingActionButton` for add. Equipment filter dropdown unchanged.

---

### Equipment Screen (`ui/EquipmentScreen.kt`)

`LargeTopAppBar`. `ListItem` rows with `trailingContent` delete icon button. `ExtendedFloatingActionButton` for add.

---

## 9. Interaction Patterns

### Workout Cursor Model

`ActiveWorkoutUiState` tracks `currentExerciseIndex` and `currentSetIndex`. These are the only pointers to the "current" set.

| Action | Effect on cursor | Effect on `isDone` |
|---|---|---|
| `completeCurrentSet()` | Advances to next set; if last set → `requestFinish()` | Sets current set `isDone = true` |
| `goToPreviousSet()` | `setIndex - 1`; wraps to previous exercise's last set at `setIndex == 0`; no-op at start | None |
| `skipExercise()` | `exerciseIndex + 1`, `setIndex = 0`; if last exercise → `requestFinish()` | None — skipped sets remain `isDone = false` |

**Key invariant:** `isDone` is never modified by navigation (back/skip). Only `completeCurrentSet()` sets it. The summary renders `isDone = false` sets as "Skipped" automatically — no special skip state needed.

### Workout Finish Flow

```
completeCurrentSet() on last set
  └── requestFinish()         → stops timer, showSummary = true
        └── LaunchedEffect     → navigates to WorkoutSummaryRoute

  Summary: Resume
    └── resumeWorkout()        → showSummary = false, cursor unchanged

  Summary: Confirm Finish
    └── finishWorkout()        → persists to DB, isFinished = true, pops to Home
```

`requestFinish()` / `resumeWorkout()` do not reset the cursor — if the user resumes, they return to exactly where they were.

### Home Screen State Transitions

The Home hero has two states driven by whether a routine is selected:
- **Routine selected:** hero shows day name, exercise chips, Start Workout CTA
- **No routine:** hero shows empty state with "Manage Routines" button

Routine selection persists via `HomeViewModel` which streams the active routine from the repository.

---

## Implementation Notes

- All `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` annotations required for `MotionScheme.expressive()` and any other Expressive components
- Gradient backgrounds: `Brush.linearGradient(...)` passed to `Modifier.background(brush)`
- Icons: `Icons.Rounded.*` from `material-icons-extended` (already in dependencies)
- No new dependencies required — all M3 Expressive components are in `compose-bom:2026.03.01`
- Edge-to-edge already enabled via `enableEdgeToEdge()` in `MainActivity` — the Home hero bleeds under the status bar by default
