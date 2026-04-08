# WorkoutPlanner Design Guidelines

**Last updated:** 2026-04-09
**Status:** Authoritative — supersedes all individual spec files in `docs/superpowers/specs/`

This is the single source of truth for the app's visual design, component patterns, and interaction conventions. Read this before implementing any new screen or modifying an existing one.

---

## 1. App Identity

### Personality

**Dark, premium, and purposeful.** WorkoutPlanner is a focused fitness tool. The design is confident and modern: near-black surfaces with a subtle purple tint, the gradient hero banner as the primary color moment, and premium polish through weight contrast and refined spacing — not decoration. M3 Expressive motion signals energy and polish; large touch targets serve gym use.

### Color Story

The app uses a fixed **Dark & Deep** theme by default. Dynamic color (wallpaper-driven) is available as an opt-in setting.

Two gradient variants carry the identity:

- **Home hero gradient:** `150° #4A0080 → #6750A4 → #B5488A` — used only on the Home screen hero banner.
- **Active card header gradient:** `135° #2D1060 → #4A2280` — used only on the active exercise card header strip. Intentionally darker than the hero to avoid competition.
- **CTA button gradient:** `90° #6750A4 → #B5488A` — used only on the "Done" action pill in the workout screen.

### Launcher Icon

| Dimension | Decision |
|---|---|
| Symbol | Outlined dumbbell |
| Background | Deep purple gradient `#1E003E → #3D0070` (135°) |
| Foreground stroke | Acid green `#C8FF00` — launcher icon only, never in-app |
| Themed icon (Android 13+) | White outline on mid-grey |

---

## 2. Color Tokens

### Fixed Dark Theme Surfaces

| Name | Hex | M3 mapping | Role |
|---|---|---|---|
| Screen background | `#0D0D14` | `background` / `surface` | Base surface for all screens |
| Card surface | `#1A1A28` | `surfaceVariant` / `surfaceContainer` | All cards, elevated surfaces |
| Nav bar | `#141422` | `surfaceContainerLow` | Bottom navigation bar |
| Stepper inner | `#12102A` | `surfaceContainerLowest` | Reps/weight stepper card backgrounds |
| Border / divider | `rgba(255,255,255,0.07)` | `outlineVariant` | Structural lines, nav bar top border, mini-bar top border |

### Text Colors

| Role | Value | Usage |
|---|---|---|
| Primary text | `#EEE8FF` | Body text, card titles, headlines on dark surfaces |
| Secondary text | `#7A7590` | Dates, durations, supporting metadata |
| Section label | `#4A4865` | Uppercase `labelSmall` section headers |
| On-gradient primary | `#FFFFFF` 100% | Titles on hero gradient |
| On-gradient secondary | `#FFFFFF` 65% | Sub-labels on hero gradient |

### Accent / Interactive

| Token | Value | Usage |
|---|---|---|
| `primary` | `#D0BCFF` | Active tab indicator, icon tints, chip borders |
| `primaryContainer` | `#3B2F6B` | Tonal button containers, nav indicator pill |
| `error` | M3 dark default | "End workout" button only — nowhere else |
| Hero gradient | `150° #4A0080 → #6750A4 → #B5488A` | Home hero only |
| Card header gradient | `135° #2D1060 → #4A2280` | Active exercise card header strip only |
| CTA gradient | `90° #6750A4 → #B5488A` | "Done" CTA pill only |

### Semantic Rules

- `error` is exclusively for the "End workout" button.
- The hero gradient appears only on the Home screen hero banner.
- The card header gradient appears only on the active exercise card header strip.
- The CTA gradient appears only on the "Done" action pill.
- Raw hex values are only permitted for gradients and translucent overlays where `MaterialTheme.colorScheme` cannot express the intent.
- All other interactive color must use `MaterialTheme.colorScheme.*` tokens.
- Acid green (`#C8FF00`) is launcher icon only — never in-app.

### Dynamic Color

`WorkoutPlannerTheme(useDynamicColor = false)` by default. A `useDynamicColor` boolean preference in DataStore gates the opt-in. When enabled, `dynamicDarkColorScheme` activates on API 31+ (always dark — no light variant).

---

## 3. Typography

No custom font. M3 defaults (Roboto) throughout.

### Type Scale

| Role | Weight | Size | Usage |
|---|---|---|---|
| `headlineSmall` | 900 | ~24sp | Hero day name, active card exercise name in header strip |
| `titleMedium` | 800 | ~16sp | Card primary text — routine name, history entry title |
| `titleSmall` | 700 | ~14sp | Mini-bar workout name |
| `bodyMedium` | default | ~14sp | Exercise descriptions, card body text |
| `bodySmall` | default | ~12sp | Dates, durations, secondary metadata, mini-bar elapsed time |
| `labelSmall` | 600, UPPERCASE | ~11sp | Section headers, exercise progress labels |
| Stepper numbers | 900 | 44sp | Reps and weight — readable at arm's length |

### Conventions

- **Weight 900** is reserved for hero headlines, active card exercise name, and stepper numbers.
- **UPPERCASE** is always `labelSmall` + section-label color + letter-spacing. Never uppercase body text.
- No custom font family. Do not add one.

---

## 4. Shape, Spacing & Touch Targets

### Shape Tokens

| Shape | Value | Applied to |
|---|---|---|
| Pill | `CircleShape` | All buttons, stepper ±controls, set dot indicators, nav indicator pills |
| Large card | `RoundedCornerShape(20.dp)` | History session cards |
| Standard card | `RoundedCornerShape(16.dp)` | Routine list cards, recent workout cards, exercise cards |
| Active card header strip | `topStart = 16.dp, topEnd = 16.dp` | Gradient header zone on active exercise card |
| Chip | M3 default | `SuggestionChip`, `InputChip`, set chips |

**Rule: actions are pills, containers are rounded rectangles.** Shape is never overridden in `Theme.kt` — apply at call sites.

### Spacing

| Context | Value |
|---|---|
| Screen horizontal content padding | 16 dp |
| Card vertical spacing in lists | 8–12 dp |
| Section header bottom margin | 8 dp |
| Active card header strip padding | 12 dp vertical, 16 dp horizontal |

### Touch Targets

| Element | Min size |
|---|---|
| All interactive elements | 48 × 48 dp |
| Stepper cards (reps, weight) | ~120 dp height |
| Active workout mini-bar | 64 dp height |

---

## 5. Motion

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
MaterialTheme(
    motionScheme = MotionScheme.expressive(),
    colorScheme = colorScheme,
    typography = Typography,
    content = content
)
```

Spring physics apply automatically to all M3 components. Do not override with custom easing curves or duration values.

> **Note:** `MotionScheme.expressive()` is currently internal in M3 1.4.0 and cannot be called from user code. The `MaterialTheme(colorScheme, typography, content)` overload is used until the API is promoted to stable. This section documents the intent; update `Theme.kt` when the API is stable.

### Manual Animations

| Pattern | Usage |
|---|---|
| `AnimatedVisibility` with `expandVertically` + `shrinkVertically`, `spring(dampingRatio = Spring.DampingRatioMediumBouncy)` | History card expand/collapse |

---

## 6. Component Patterns

### Buttons

All buttons use pill shape (`CircleShape`).

| Variant | When to use |
|---|---|
| `FilledButton` (white bg, dark text) | Primary CTA on gradient hero ("Start Workout") |
| Gradient pill | Active workout "Done — Next Set" / "Done — Finish Exercise" CTA |
| `FilledTonalButton` | All secondary actions: Back, Skip Exercise, Swap, Resume |
| `FilledTonalButton` with `error` container | "End" workout only |
| `TextButton` | Low-priority actions in CreateRoutine |
| `ExtendedFloatingActionButton` | Add actions on list screens |

### App Bars

| Type | Used on |
|---|---|
| None | Home screen (gradient hero replaces it) |
| `TopAppBar` (standard) | Workout screen — day name + elapsed timer subtitle |
| `LargeTopAppBar` (collapses on scroll) | History, Settings, TimerSettings, Routines, RoutineDetail, CreateRoutine, Exercises, Equipment |

`LargeTopAppBar` always paired with:
- `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()`
- `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)` on the `Scaffold`

App bar `surface` color resolves to `#0D0D14` in the dark theme — seamless with screen background on collapse.

### Cards

| Pattern | Usage |
|---|---|
| `#1A1A28` card, 20 dp radius | History session cards (expandable) |
| `#1A1A28` card, 16 dp radius | Recent workout cards (Home), routine list items |
| Active routine: 4 dp `primary` left border | Highlights active routine in Routines list |
| `OutlinedCard` with `#1A1A28` bg | Day containers in CreateRoutine |

**Rule:** All cards use the `#1A1A28` card surface. Never deviate from the surface palette.

### Active Exercise Card (Workout Screen)

Two-zone structure for the active card:

```
┌─────────────────────────────────────────┐
│  gradient strip (135° #2D1060→#4A2280)  │  ← topStart/topEnd 16dp
│  EXERCISE X OF N · labelSmall 65% white │
│  Exercise Name · headlineSmall w900     │
│  [Swap ⇄]                    (trailing) │
├─────────────────────────────────────────┤
│  card body (#1A1A28)                    │
│  set dot indicators + "SET X OF N"      │
│  ┌──────────┐  ┌──────────┐            │
│  │  REPS    │  │   KG     │  (#12102A) │
│  │  44sp/900│  │  44sp/900│            │
│  │  − pill +│  │  − pill +│            │
│  └──────────┘  └──────────┘            │
│  AMRAP toggle (last set only)           │
│  [gradient CTA pill: "Done — Next Set"] │
│  [← Back (tonal)] [Skip Exercise →]    │
│  completed sets chips row               │
└─────────────────────────────────────────┘
```

Inactive / upcoming cards: `#141422`, `alpha = 0.55f`.
Completed exercise cards: `alpha = 0.35f`.

### Rest Timer Banner

`primaryContainer` (`#3B2F6B`) card surface, `onPrimaryContainer` (`#EEE8FF`) text for elapsed time and milestone labels. 16 dp horizontal padding matches screen content padding.

### List Items

Use `ListItem` composable for Settings rows and Exercise/Equipment list rows. Leading icon tinted `onSurfaceVariant`. Trailing: chevron (Settings) or delete icon (lists).

### Navigation Bar

`NavigationSuiteScaffold` with two items: Home + History.

- Background: `#141422`
- Top border: 1px `rgba(255,255,255,0.07)`
- Active indicator: M3 default translucent `primaryContainer` pill
- Active label/icon: `primary` (`#D0BCFF`)
- Inactive: `onSurfaceVariant` (`#7A7590`)

Settings reachable via tune icon in Home hero — not a tab.

### Active Workout Mini-Bar

`isActive && !isFullScreen` in `MainActivity` inner `Scaffold` `bottomBar`.

- Surface: `primaryContainer` (`#3B2F6B`), 64 dp height
- Top border: 1px `rgba(255,255,255,0.07)`
- Leading: `FitnessCenter` icon + workout name (`titleSmall` weight 700) + elapsed time (`bodySmall`)
- Trailing: `FilledTonalButton` "Resume" (pill)
- Full row tappable → `ActiveWorkoutRoute`

---

## 7. Navigation Structure

### Tab Layout

| Tab | Icon | Route |
|---|---|---|
| Home | `Icons.Rounded.FitnessCenter` | `HomeRoute` |
| History | `Icons.Rounded.History` | `HistoryRoute` |

### Back Stack Behavior

| Trigger | Behavior |
|---|---|
| Minimize workout | Pops back stack; `isFullScreen = false` → mini-bar appears |
| Tap mini-bar / Resume | Navigates to `ActiveWorkoutRoute` |
| Last set completed | `requestFinish()` → `LaunchedEffect` navigates to `WorkoutSummaryRoute` |
| Summary → Confirm finish | Persists to DB, `isFinished = true`, pops to Home |
| Summary → Resume | `showSummary = false`, returns to workout screen |

### UDF Callback Convention

Composables receive lambdas — never `NavController` references.

### ViewModel Creation Rule

- `ActiveWorkoutViewModel`: `viewModel(viewModelStoreOwner = LocalActivity.current)` — Activity-scoped
- All other `@HiltViewModel` screen ViewModels: `hiltViewModel()`
- Mixing these crashes at runtime

---

## 8. Screen-by-Screen Designs

### Home Screen

No `TopAppBar`. Gradient hero bleeds under status bar (edge-to-edge).

**Hero (routine selected):**
- Gradient `150° #4A0080 → #6750A4 → #B5488A`
- Title row: "Workout Planner" (`titleLarge`, white) + tune icon ghost pill
- Label: "ROUTINE · DAY X OF N" (`labelSmall`, white 65%)
- Headline: day name (`headlineSmall` w900, white)
- Subtext: "N exercises" (`bodySmall`, white 65%)
- Exercise chips: `rgba(255,255,255,0.12)` bg pills
- Button row: `FilledButton` (white bg, dark text) "▶ Start Workout" flex 1 + `FilledTonalButton` (18% white bg) swap icon fixed width

**Hero (no routine):**
- Same gradient, "No Active Routine" headline, "Manage Routines" tonal button

**Recent Workouts:** `#1A1A28` cards, 16 dp radius. Name `titleMedium` w800 + date `bodySmall`. Duration `SuggestionChip`.

### Workout Screen

`TopAppBar` with day name + elapsed time subtitle. Trailing: "+ Exercise" tonal pill, "End" error-tonal pill.

Exercise list: `LazyColumn` of `ExerciseCard` composables. Active card: gradient header strip + body. Inactive: dimmed `#141422` card.

### Workout Summary Screen

`#0D0D14` background. "Total Volume" in `secondaryContainer` card. Per-exercise `#1A1A28` cards. Skipped = `isDone false`. Two pills: "Finish Workout" (primary filled) + "Resume Workout" (tonal).

### History Screen

`LargeTopAppBar` "History" + `exitUntilCollapsedScrollBehavior`. Date-grouped `LazyColumn`. Session cards `#1A1A28` 20 dp radius, spring expand/collapse. Empty state: centered icon + text.

### Settings Screen

`LargeTopAppBar` "Settings". First row: Theme toggle (`Switch`). Remaining rows: Timer Settings, Exercises, Equipment, Routines.

### Timer Settings / Routines / RoutineDetail / CreateRoutine / Exercises / Equipment

`LargeTopAppBar` + `exitUntilCollapsedScrollBehavior`. Dark palette. No structural changes.

---

## 9. Interaction Patterns

### Workout Cursor Model

| Action | Effect on cursor | Effect on `isDone` |
|---|---|---|
| `completeCurrentSet()` | Advances to next set; last set → `requestFinish()` | Sets `isDone = true` |
| `goToPreviousSet()` | setIndex - 1; wraps to previous exercise's last set | None |
| `skipExercise()` | exerciseIndex + 1, setIndex = 0; last exercise → `requestFinish()` | None |

`isDone` is only set by `completeCurrentSet()`. Summary renders `isDone = false` as "Skipped".

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

---

## Implementation Notes

- `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` required for `MotionScheme.expressive()` (currently internal in M3 1.4.0 — see Motion section)
- Gradient backgrounds: `Brush.linearGradient(...)` passed to `Modifier.background(brush)`
- Icons: `Icons.Rounded.*` from `material-icons-extended`
- No new dependencies required
- Edge-to-edge enabled via `enableEdgeToEdge()` in `MainActivity`
