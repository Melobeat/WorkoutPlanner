# WorkoutPlanner Design Guidelines

**Last updated:** 2026-04-10
**Status:** Authoritative — supersedes all individual spec files in `docs/superpowers/specs/`

This is the single source of truth for the app's visual design, component patterns, and interaction conventions. Read this before implementing any new screen or modifying an existing one.

> **How to read this document:** Sections 1–5 define the design system (tokens, type, shape, motion). Section 6 defines component patterns. Section 7 defines navigation. Section 8 gives screen-by-screen specs. Section 9 covers interaction models. Where implementation diverges from an older spec version, this document is correct — the codebase is the reference.

---

## 1. App Identity

### Personality

**Dark, premium, and purposeful.** WorkoutPlanner is a focused fitness tool. The design is confident and modern: near-black forest-green surfaces, an emerald gradient hero banner as the primary color moment, and premium polish through weight contrast and refined spacing — not decoration. Large touch targets serve gym use.

### Color Story

The app uses a **Dark Forest** theme by default, with a **Day** (light) variant selectable in Settings. The preference is stored in DataStore as `"dark"` / `"light"` / `"system"`.

Three gradient variants carry the identity — all emerald green:

- **Home hero gradient:** diagonal (bottom-left → top-right) `GradientHeroStart → GradientHeroMid → GradientHeroEnd` (`#0D2E18 → #1A4A28 → #0F3520`) — used only on the Home screen hero banner.
- **Active card header gradient:** diagonal (top-left → bottom-right) `GradientCardStart → GradientCardEnd` (`#0C2B16 → #1A4A28`) — used only on the active exercise card header strip. Intentionally darker than the hero to avoid competition.
- **CTA button gradient:** horizontal `GradientCtaStart → GradientCtaEnd` (`#1E8449 → #27AE60`) — used only on action CTA pills (workout Done, Home Start Workout, Summary Finish).

### Launcher Icon

| Dimension | Decision |
|---|---|
| Symbol | Outlined dumbbell |
| Background | Deep purple gradient `#1E003E → #3D0070` (135°) |
| Foreground stroke | Acid green `#C8FF00` — launcher icon only, never in-app |
| Themed icon (Android 13+) | White outline on mid-grey |

---

## 2. Color Tokens

### Dark Forest Theme Surfaces (dark mode)

| Name | Constant | Hex | M3 token in `AppDarkColorScheme` | Role |
|---|---|---|---|---|
| Screen background | `DarkBackground` | `#0A0E0B` | `background` | Base surface for all screens |
| App bar / nav surface | `DarkSurface` | `#0D1410` | `surface` | App bars, nav bar background |
| Card surface | `DarkSurfaceContainer` | `#111A12` | `surfaceVariant` | All cards, elevated surfaces |
| Stepper inner | `DarkSurfaceContainerHigh` | `#172019` | (semantic — not mapped) | Documented intent; steppers use `colorScheme.surface` in code |
| Border (subtle) | `DarkOutlineVariant` | `0x0DFFFFFF` (~5% white) | `outlineVariant` | Mini-bar top border, subtle dividers. Never use as progress track. |
| Border (visible) | `DarkOutline` | `0x14FFFFFF` (~8% white) | `outline` | Structural dividers |

### Day Theme Surfaces (light mode)

| Name | Constant | Hex | M3 token in `AppLightColorScheme` | Role |
|---|---|---|---|---|
| Screen background | `LightBackground` | `#F4F8F5` | `background` | Base surface |
| App bar / nav surface | `LightSurface` | `#FFFFFF` | `surface` | App bars, nav bar |
| Card surface | `LightSurfaceContainer` | `#F0F4F1` | `surfaceVariant` | All cards |
| Border (subtle) | `LightOutlineVariant` | `#DDE8DE` | `outlineVariant` | Subtle borders |
| Border (visible) | `LightOutline` | `#C8DECA` | `outline` | Structural dividers |

### Text Colors

| Role | Token | Dark hex | Light hex | Usage |
|---|---|---|---|---|
| Primary text | `colorScheme.onSurface` / `colorScheme.onBackground` | `#D4E8D6` | `#1A2E1C` | Body text, card titles, headlines |
| Secondary text | `colorScheme.onSurfaceVariant` | `#D4E8D6` @ 40% alpha | `#1A2E1C` @ 50% alpha | Dates, durations, supporting metadata, inactive icons |
| On-gradient primary | `Color.White` 100% | `#FFFFFF` | `#FFFFFF` | Titles and icons directly on gradient backgrounds |
| On-gradient secondary | `Color.White.copy(alpha = 0.65f)` | `#FFFFFF` ~65% | `#FFFFFF` ~65% | Sub-labels on gradient backgrounds |

### Accent / Interactive (dark / light)

| Token | Dark hex | Light hex | Usage |
|---|---|---|---|
| `colorScheme.primary` | `#27AE60` | `#16A34A` | Active tab indicators, icon tints, elapsed-time text, progress bar fill, `+N more` labels, active routine name |
| `colorScheme.primaryContainer` | `#27AE60` @ 14% alpha | `#16A34A` @ 10% alpha | Tonal button containers, nav indicator pill, mini-bar surface |
| `colorScheme.onPrimaryContainer` | `#D4E8D6` | `#15803D` | Text/icons on `primaryContainer` surfaces |
| `colorScheme.secondary` | `#A78BFA` | `#7C3AED` | **Rest timer and UP NEXT badge only.** Do not use for general text or generic UI. |
| `colorScheme.secondaryContainer` | `#A78BFA` @ 14% alpha | `#7C3AED` @ 10% alpha | Rest timer banner surface, UP NEXT badge surface |
| `colorScheme.onSecondaryContainer` | `#A78BFA` | `#7C3AED` | Text on `secondaryContainer` surfaces |
| `colorScheme.tertiary` | `#60A5FA` | `#2563EB` | **History chart bars and PR/stat values only.** |
| `colorScheme.tertiaryContainer` | `#60A5FA` @ 14% alpha | `#2563EB` @ 10% alpha | AMRAP stepper card background, AMRAP set badge |
| `colorScheme.onTertiaryContainer` | `#60A5FA` | `#2563EB` | Text on `tertiaryContainer` surfaces |
| `colorScheme.error` | `#E05252` | `#DC2626` | Delete icons, delete confirmation actions, "End / Cancel" workout destructive actions |
| `colorScheme.errorContainer` | `#E05252` @ 12% alpha | `#DC2626` @ 7% alpha | "End" button tonal container |

> **Color role rules:**
> - `secondary`/`secondaryContainer` — violet — **rest timer and UP NEXT badge only**. Do not use for text labels or generic UI elements.
> - `tertiary`/`tertiaryContainer` — steel blue — **history chart bars and PR/stat values only**.
> - `error`/`errorContainer` — used for all destructive/irreversible actions (delete, cancel workout, end workout).

### Gradient Named Constants (in `Color.kt`)

| Constant | Dark hex | Light hex | Used in |
|---|---|---|---|
| `GradientHeroStart` | `#0D2E18` | `#14532D` | Home hero start |
| `GradientHeroMid` | `#1A4A28` | `#1A6B38` | Home hero mid |
| `GradientHeroEnd` | `#0F3520` | `#1A7A3E` | Home hero end |
| `GradientCardStart` | `#0C2B16` | `#14532D` | Active exercise card header start |
| `GradientCardEnd` | `#1A4A28` | `#166534` | Active exercise card header end |
| `GradientCtaStart` | `#1E8449` | `#15803D` | CTA button gradient start |
| `GradientCtaEnd` | `#27AE60` | `#16A34A` | CTA button gradient end |

**Usage rules:**
- Raw hex is only permitted for gradients and translucent overlays where `MaterialTheme.colorScheme` cannot express the intent. Use the named constants — never inline hex for these.
- All other color must use `MaterialTheme.colorScheme.*` tokens.
- Acid green (`#C8FF00`) is launcher icon only — never in-app.
- In light mode, use the `*Light` gradient variants (`GradientHeroStartLight`, etc.).

### Gradient Brush Construction

```kotlin
// Home hero — diagonal bottom-left to top-right
Brush.linearGradient(
    colors = listOf(GradientHeroStart, GradientHeroMid, GradientHeroEnd),
    start = Offset(0f, Float.POSITIVE_INFINITY),
    end = Offset(Float.POSITIVE_INFINITY, 0f)
)

// Active card header — diagonal top-left to bottom-right
Brush.linearGradient(
    colors = listOf(GradientCardStart, GradientCardEnd),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

// CTA gradient pill — horizontal left to right
Brush.linearGradient(colors = listOf(GradientCtaStart, GradientCtaEnd))
```

Apply with `Modifier.background(brush = ..., shape = CircleShape)`.

### Gradient CTA Button Pattern

Used in HomeScreen, ExerciseCard, WorkoutSummaryScreen:

```kotlin
Button(
    modifier = Modifier.background(
        brush = Brush.linearGradient(listOf(GradientCtaStart, GradientCtaEnd)),
        shape = CircleShape
    ),
    shape = CircleShape,
    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)
)
```

### Theme System

`WorkoutPlannerTheme(themeMode: String = "dark")` — accepts `"dark"`, `"light"`, `"system"`. Preference stored as `stringPreferencesKey("theme_mode")` in DataStore via `RestTimerPreferencesRepository.themeMode: Flow<String>`. Settings screen uses `SingleChoiceSegmentedButtonRow` + `SegmentedButton` (not `FilterChip`s).

---

## 3. Typography

No custom font. M3 defaults (Roboto) throughout. Only `bodyLarge` is overridden in `Type.kt` (16sp, normal weight, 24sp line height). All other styles are M3 defaults.

### Type Scale

The table below documents every style actively used in the codebase with their actual parameters.

| Style | Weight | Notes | Used for |
|---|---|---|---|
| `typography.displaySmall` | `FontWeight.Black` (900) | ~36sp M3 default | Hero workout day name, stepper values (reps/weight) |
| `typography.headlineMedium` | `FontWeight.Black` | ~28sp M3 default | Home empty-state headline |
| `typography.headlineSmall` | `FontWeight.Black` or `FontWeight.ExtraBold` | ~24sp M3 default | Active exercise name (header strip), non-active exercise name |
| `typography.titleLarge` | `FontWeight.Bold` | ~22sp M3 default | Volume value in Summary, ExerciseLibraryItem name |
| `typography.titleMedium` | `FontWeight.Bold` or `FontWeight.ExtraBold` | ~16sp M3 default | App title in hero, card primary text (routine name, session name), mini-bar workout name |
| `typography.titleSmall` | `FontWeight.Bold` | ~14sp M3 default | Exercise name in Summary, rest timer display |
| `typography.bodyLarge` | `FontWeight.Bold` or `FontWeight.Medium` | 16sp (overridden) | Exercise name in RoutineDetail, ExerciseEditItem name |
| `typography.bodyMedium` | default / `FontWeight.Medium` | ~14sp M3 default | Exercise descriptions, set reps×weight in completed sets |
| `typography.bodySmall` | default / `FontWeight.Medium` | ~12sp M3 default | Dates, durations, secondary metadata, mini-bar elapsed time, set rows |
| `typography.labelLarge` | `FontWeight.Bold` | ~14sp M3 default | Timer values in TimerSettings trailing text, RoutineDetail weight text, WorkoutSessionCard duration |
| `typography.labelMedium` | `FontWeight.Bold` | ~12sp M3 default | "Add Exercise" / "Skip" / "Back" button text, WorkoutSessionCard date, equipment dropdown label |
| `typography.labelSmall` | `FontWeight.Bold`, UPPERCASE, `letterSpacing = 1.5.sp` | ~11sp M3 default | Section headers, "EXERCISE N OF M" counter in card, "SET N — ACTIVE" label, "REST" label, milestone labels, exercise chip text |

### Conventions

- **`FontWeight.Black`** (900) is reserved for hero headlines (day name), active exercise name in the gradient strip, stepper values, and empty-state headlines.
- **UPPERCASE** is always `labelSmall` + `letterSpacing = 1.5.sp` + `onSurfaceVariant` or white-alpha color. Never uppercase body text.
- **TopAppBar titles** use `FontWeight.Black` consistently across all screens.
- No custom font family. Do not add one.

---

## 4. Shape, Spacing & Touch Targets

### Shape Tokens

| Shape | Value | Applied to |
|---|---|---|
| Pill | `CircleShape` | All action buttons, stepper ± controls, badge indicators, nav indicator pills, exercise chips, "Resume" mini-bar button |
| Extra-large card | `RoundedCornerShape(20.dp)` | History session cards (`HistorySessionCard`), `StepperCard` |
| Standard card | `RoundedCornerShape(16.dp)` | Routine list cards, `HistorySessionCard` inner detail card body shape, rest timer banner, routine day cards in CreateRoutine, workout day items in RoutineDetail, day chooser dialog items |
| Exercise card (active) | `RoundedCornerShape(16.dp)` | Active exercise card (via `Modifier.clip`) |
| Exercise card (inactive) | `RoundedCornerShape(12.dp)` | Non-active exercise cards |
| Active card header strip | `RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)` | Gradient header zone on active exercise card |
| Inner detail card | `RoundedCornerShape(12.dp)` | History expanded detail card |
| Bottom sheet | M3 default | `ExerciseSelectionDialog` |
| Dialogs | M3 default | All `AlertDialog`s |

**Rule: actions are pills, containers are rounded rectangles.** Shape is never overridden globally in `Theme.kt` — apply at call sites.

### Spacing

| Context | Value |
|---|---|
| Screen horizontal content padding | `16.dp` |
| Home hero horizontal padding | `20.dp` |
| Card vertical spacing in `LazyColumn` lists | `8–12.dp` (commonly `spacedBy(10.dp)` or `spacedBy(8.dp)`) |
| Section header to content gap | `4–8.dp` |
| Active card header strip padding | `12.dp` vertical, `16.dp` horizontal |
| Active card body section padding | `start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp` |
| Stepper card inner padding | `12.dp` all sides |
| Rest timer banner inner padding | `horizontal = 16.dp, vertical = 12.dp` |
| Mini-bar horizontal padding | `16.dp` |

### Touch Targets

| Element | Min size |
|---|---|
| All interactive elements | 48 × 48 dp |
| Stepper buttons (± controls) | `CircleShape`, height `40.dp` |
| Active workout CTA pill | height `52.dp` |
| "Add Day" button (CreateRoutine) | height `52.dp` |
| Active workout mini-bar | `64.dp` height |

---

## 5. Motion

`MotionScheme.expressive()` is currently `internal` in M3 1.4.0. `MaterialTheme(colorScheme, typography, content)` is used until the API is promoted to stable. Do not attempt to wire `motionScheme` — it will not compile from user code.

### Manual Animations

| Pattern | Parameters | Usage |
|---|---|---|
| `AnimatedVisibility` with `expandVertically` + `shrinkVertically` | `spring(dampingRatio = Spring.DampingRatioMediumBouncy)` for **both** enter and exit | History session card expand/collapse, exercise card body expansion |
| `AnimatedVisibility` with `expandVertically` + `shrinkVertically` | Default spec (no custom spring) | Rest timer banner show/hide, RoutineDayCard expand/collapse, ExerciseEditItem expand/collapse |

Do not override with custom easing curves or duration values beyond these established patterns.

---

## 6. Component Patterns

### Buttons

All action buttons use pill shape (`CircleShape`).

| Variant | When to use |
|---|---|
| `Button` with gradient `Modifier.background(GradientCtaStart→GradientCtaEnd)`, `containerColor = Color.Transparent`, `contentColor = Color.White` | Primary CTA on gradient hero ("Start Workout", "Manage Routines") and other primary actions |
| Gradient pill (`Surface(shape = RoundedCornerShape(50))` + gradient brush) | Active workout "Done — Next Set" / "Done — Finish Exercise" CTA |
| `FilledTonalButton` | All secondary actions: Back, Skip Exercise, Swap, Resume (mini-bar), "Swap Day" in hero |
| `FilledTonalButton` with `containerColor = colorScheme.errorContainer` | "End" workout action in TopAppBar |
| `TextButton` | "Save" action in CreateRoutine TopAppBar; low-priority actions |
| `ExtendedFloatingActionButton` | Add actions on list screens (Exercises, Equipment, Routines) |

### App Bars

| Type | Used on |
|---|---|
| None | Home screen (gradient hero replaces it) |
| `TopAppBar` (compact, no scroll behavior) | WorkoutScreen, WorkoutSummaryScreen |
| `TopAppBar` + `enterAlwaysScrollBehavior` + `nestedScroll` | ExercisesScreen, EquipmentScreen, RoutinesScreen, RoutineDetailScreen, CreateRoutineScreen |
| `LargeTopAppBar` + `exitUntilCollapsedScrollBehavior` + `nestedScroll` | HistoryScreen, SettingsScreen, TimerSettingsScreen |

> **Note:** The previous spec listed `LargeTopAppBar` for Routines, RoutineDetail, CreateRoutine, Exercises, and Equipment. Those screens use compact `TopAppBar` with `enterAlwaysScrollBehavior` — the bar hides entirely on scroll up and reappears on scroll down.

`LargeTopAppBar` setup:
```kotlin
val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = { LargeTopAppBar(scrollBehavior = scrollBehavior, ...) }
)
```

`TopAppBar` + `enterAlwaysScrollBehavior` setup:
```kotlin
val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = { TopAppBar(scrollBehavior = scrollBehavior, ...) }
)
```

App bar `surface` color resolves to `#0D1410` (dark) / `#FFFFFF` (light) — seamless with screen background.

TopAppBar titles always use `FontWeight.Black`.

### Cards

| Pattern | Shape | Container color | Usage |
|---|---|---|---|
| History session card | `RoundedCornerShape(20.dp)` | `colorScheme.surfaceVariant` (`#111A12` dark) | Expandable session cards in History |
| History inner detail card | `RoundedCornerShape(12.dp)` | `colorScheme.surface` (`#0D1410` dark) | Exercise breakdown inside expanded session |
| Recent workout card (`WorkoutSessionCard`) | `MaterialTheme.shapes.small` | `colorScheme.surfaceVariant` | Home screen recent workouts |
| Routine list card | `RoundedCornerShape(16.dp)` | `colorScheme.surfaceVariant` | Routines list items |
| Workout day item (RoutineDetail) | `RoundedCornerShape(16.dp)` | `colorScheme.surfaceVariant` | Day breakdown in RoutineDetail |
| Rest timer banner | `RoundedCornerShape(16.dp)` | `colorScheme.primaryContainer` | Rest timer between sets/exercises |
| Total volume (Summary) | `RoundedCornerShape(16.dp)` | `colorScheme.secondaryContainer` | Total volume surface in WorkoutSummary |
| Routine day card (CreateRoutine) | `OutlinedCard`, `RoundedCornerShape(16.dp)` | Default outlined card bg | Day containers in CreateRoutine |

**Rule:** All standard cards use `colorScheme.surfaceVariant` (`#111A12` dark / `#F0F4F1` light). Never use raw hex directly for card surface color.

### Active Exercise Card (Workout Screen)

Two-zone structure for the active card:

```
┌─────────────────────────────────────────────┐
│  gradient strip (top-left→bottom-right       │  ← topStart/topEnd 16dp corners
│  GradientCardStart→GradientCardEnd)          │
│  EXERCISE X OF N · labelSmall 65% white      │
│  Exercise Name · headlineSmall Black         │
│  [Swap ⇄]                        (trailing) │
├─────────────────────────────────────────────┤
│  card body (colorScheme.surfaceVariant)      │
│  "SET N — ACTIVE" label (labelSmall Bold)   │
│  ┌────────────────┐  ┌────────────────┐     │
│  │  REPS          │  │  KG            │     │
│  │  displaySmall/ │  │  displaySmall/ │     │
│  │  Black         │  │  Black         │     │
│  │  containerColor│  │  = surface or  │     │
│  │  tertiaryCtner │  │  tertiaryCtner │     │
│  │  − pill   +    │  │  − pill   +    │     │
│  └────────────────┘  └────────────────┘     │
│  AMRAP toggle (last set only)               │
│  [gradient CTA pill "Done — Next Set"]      │  ← height 52dp
│  [← Back (tonal)] [Skip Exercise → (tonal)]│
│  completed sets + pending sets chips row    │
└─────────────────────────────────────────────┘
```

Non-active / upcoming cards: `containerColor = colorScheme.surfaceVariant`, `alpha = 0.55f`, `RoundedCornerShape(12.dp)`.
Completed exercise cards: `alpha = 0.35f`.

### StepperCard

| State | `containerColor` | Value color | Label color |
|---|---|---|---|
| Normal | `colorScheme.surface` (`#0D1410` dark) | `colorScheme.onSurface` | `colorScheme.onSurfaceVariant` |
| AMRAP | `colorScheme.tertiaryContainer` (`#60A5FA` @ 14% dark) | `colorScheme.onTertiaryContainer` | `colorScheme.onTertiaryContainer` |

Shape: `RoundedCornerShape(20.dp)`. Elevation: `2.dp`. Stepper value: `typography.displaySmall`, `FontWeight.Black`. Label: `typography.labelSmall`, `FontWeight.Bold`.

### Rest Timer Banner

`containerColor = colorScheme.primaryContainer`. Timer value: `typography.displaySmall`, `FontWeight.Black`, `color = colorScheme.onPrimaryContainer`. Label/milestone text: `typography.labelSmall`, `FontWeight.Bold`, `color = colorScheme.onPrimaryContainer`. Progress bar: `color = colorScheme.primary`, `trackColor = colorScheme.primary.copy(alpha = 0.2f)`.

### List Items

Use `ListItem` composable for Settings rows, Exercises list, Equipment list, and Routines list. Leading icon tinted `colorScheme.onSurfaceVariant`. Trailing: chevron or `Switch` (Settings), delete icon (lists).

Delete icons everywhere use `tint = colorScheme.error`. Confirm actions in delete dialogs use `contentColor = colorScheme.error`.

### Icons

**Primary icon family:** `Icons.Rounded.*` — use this by default in all workout-related and content screens.

**Exceptions:**
- Settings screen uses `Icons.Outlined.*` for category icons (`Palette`, `Timer`, `FitnessCenter`, `Construction`, `ListAlt`)
- Routine selection uses `Icons.Default.CheckCircle` / `Icons.Default.RadioButtonUnchecked`
- Edit actions use `Icons.Default.Edit`
- Add FAB uses `Icons.Default.Add`

Do not mix icon families within a single screen. When adding new icons, default to `Icons.Rounded.*` unless the screen already uses a different family.

### Navigation Bar

`NavigationSuiteScaffold` with two items:

| Tab | Icon | Route |
|---|---|---|
| Home | `Icons.Rounded.Home` | `HomeRoute` |
| History | `Icons.Rounded.History` | `HistoryRoute` |

- Background: `#0D1410` dark / `#FFFFFF` light (renders as `colorScheme.surface`)
- Top border: `BorderStroke(1.dp, DarkOutlineVariant)` (`0x12FFFFFF`)
- Active indicator: M3 default translucent `primaryContainer` pill
- Active label/icon: `colorScheme.primary` (`#27AE60` dark)
- Inactive: `colorScheme.onSurfaceVariant`

Settings reachable via tune icon (`Icons.Rounded.Tune`) in the Home hero — not a tab.

### Active Workout Mini-Bar

Shown in `MainActivity` inner `Scaffold` `bottomBar` when `isActive && !isFullScreen`.

| Property | Value |
|---|---|
| Surface color | `colorScheme.primaryContainer` (`#27AE60` @ 14% dark) |
| Height | `64.dp` |
| Top border | `BorderStroke(1.dp, DarkOutlineVariant)` |
| Leading icon | `Icons.Rounded.FitnessCenter`, tint `onPrimaryContainer` |
| Elapsed time | `typography.labelSmall`, `color = onPrimaryContainer` |
| Workout name | `typography.titleSmall`, `FontWeight.Bold`, `color = onPrimaryContainer` |
| Trailing button | `FilledTonalButton` "Resume", `shape = CircleShape` |
| Horizontal padding | `16.dp` |
| Icon-to-text spacer | `12.dp` |
| Full row | tappable → `ActiveWorkoutRoute` |

### `ExerciseSelectionDialog` (Bottom Sheet)

`ModalBottomSheet` with `skipPartiallyExpanded = true`. Used when adding or swapping exercises during an active workout. Contains search `OutlinedTextField` + `LazyColumn` at `fillMaxHeight(0.75f)`. Each exercise row is a `ListItem` with a `40.dp` circle icon placeholder (`colorScheme.surfaceVariant` background). `WindowInsets.navigationBars` spacer at bottom.

### Duration / Label Chips

Use `SuggestionChip` with `containerColor = colorScheme.secondaryContainer`, `labelColor = colorScheme.onSecondaryContainer`. Leading icon optional (e.g. `Icons.Rounded.Schedule` in History).

---

## 7. Navigation Structure

### Tab Layout

| Tab | Icon | Route |
|---|---|---|
| Home | `Icons.Rounded.Home` | `HomeRoute` |
| History | `Icons.Rounded.History` | `HistoryRoute` |

### Route Inventory

```
Top-level (outside nested graph):
  HomeRoute
  HistoryRoute
  ActiveWorkoutRoute         ← sets isFullScreen = true via DisposableEffect in NavGraph
  WorkoutSummaryRoute

Nested graph (SettingsGraphRoute, startDestination = SettingsRoute):
  SettingsRoute
  TimerSettingsRoute
  RoutinesRoute
  RoutineDetailRoute(routineId: String)
  CreateRoutineRoute(routineId: String? = null)   ← null = create, non-null = edit
  ExercisesRoute
  EquipmentRoute
```

Navigate to `SettingsGraphRoute` (the nested graph key) — not `SettingsRoute` directly. Navigation Compose resolves the start destination automatically.

### Back Stack Behavior

| Trigger | Behavior |
|---|---|
| Minimize workout | Pops back stack; `isFullScreen = false` → mini-bar appears |
| Tap mini-bar / "Resume" | Navigates to `ActiveWorkoutRoute` |
| Last set completed | `requestFinish()` → `LaunchedEffect` navigates to `WorkoutSummaryRoute` |
| Summary → Confirm finish | Persists to DB, `isFinished = true`, pops to Home |
| Summary → Resume | `resumeWorkout()` → `showSummary = false`, returns to workout screen |
| Summary → Back button | `BackHandler` intercepts → `resumeWorkout()` |

### UDF Callback Convention

Composables receive navigation lambdas — never `NavController` references directly.

### ViewModel Creation Rule

```kotlin
// Activity-scoped — shared across all destinations
val activeWorkoutViewModel: ActiveWorkoutViewModel =
    viewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity)

// NavBackStackEntry-scoped — standard for all other ViewModels
val routinesViewModel: RoutinesViewModel = hiltViewModel()
```

Mixing these scoping patterns crashes at runtime. `ActiveWorkoutViewModel` must always be Activity-scoped.

---

## 8. Screen-by-Screen Designs

### Home Screen

No `TopAppBar`. Gradient hero bleeds under status bar (edge-to-edge). Root layout: `LazyColumn` (hero is the first item).

**Hero — routine selected:**
- Gradient brush: `GradientHeroStart → GradientHeroMid → GradientHeroEnd` (diagonal, bottom-left to top-right)
- Hero padding: `top = statusBarPadding + 16.dp, start = 20.dp, end = 20.dp, bottom = 28.dp`
- Title row: app title (`typography.titleMedium`, `FontWeight.Bold`, white) + `Icons.Rounded.Tune` ghost pill button
- Routine label: `"ROUTINE · DAY X OF N"` (`typography.labelSmall`, `FontWeight.Bold`, white 65%)
- Day name: `typography.displaySmall`, `FontWeight.Black`, white
- Exercise count: `typography.bodyMedium`, white 65%
- Exercise chips: `CircleShape`, `color = Color.White.copy(alpha = 0.15f)`, text `typography.labelSmall`, white. Show first 2 + "+N more" if overflow.
- Button row: gradient CTA pill `Button` (see gradient CTA button pattern in Section 2) "▶ Start Workout" (flex 1, height 48dp) + `FilledTonalButton` (`containerColor = Color.White.copy(alpha = 0.2f)`, `contentColor = Color.White`) swap icon (fixed width, `CircleShape`)
- Gap between buttons: `12.dp`

**Hero — no routine:**
- Same gradient and padding. `typography.headlineMedium`, `FontWeight.Black`, white headline "No Active Routine". `FilledButton` (same styling) "Manage Routines".

**Recent Workouts section:**
- Section header: `"RECENT WORKOUTS"` (`typography.labelSmall`, `FontWeight.Bold`, `colorScheme.onSurfaceVariant`)
- `WorkoutSessionCard` composable (defined in `HistoryScreen.kt`, reused here)
- List padding: `horizontal = 16.dp`, `bottom = 8.dp` per item, `contentPadding = PaddingValues(bottom = 16.dp)`

### Workout Screen

`TopAppBar` (compact, no scroll behavior). Trailing actions: "+ Exercise" `FilledTonalButton` (pill), dropdown with "Finish Workout" and "Cancel Workout" (text `color = colorScheme.error`).

- Subtitle: elapsed time, `typography.labelSmall`, `color = colorScheme.primary`
- `RestTimerBanner` shown/hidden via `AnimatedVisibility(expandVertically/shrinkVertically)` below TopAppBar; `padding(horizontal = 16.dp)`
- Progress row: `"EXERCISE X OF Y"` (`typography.labelSmall`, `FontWeight.Bold`, `colorScheme.onSurfaceVariant`) + `"XX%"` (`typography.labelSmall`, `FontWeight.Bold`, `colorScheme.primary`)
- Progress bar: `LinearProgressIndicator`, height `6.dp`, clipped to `CircleShape`, `color = colorScheme.primary`, `trackColor = colorScheme.surfaceVariant`, `padding(horizontal = 16.dp)`
- Exercise list: `LazyColumn`, `content padding = start=16, end=16, bottom=24.dp`, `verticalArrangement = spacedBy(8.dp)`, auto-scrolls to current exercise index
- "Cancel Workout?" confirmation: `AlertDialog` with confirm button text `color = colorScheme.error`

### Workout Summary Screen

`TopAppBar` (compact). `BackHandler` intercepts back press → `resumeWorkout()`. `LaunchedEffect` watching `isFinished` pops back when true.

- Elapsed time subtitle: `typography.labelSmall`, `color = colorScheme.primary`
- Total Volume surface: `RoundedCornerShape(16.dp)`, `color = colorScheme.secondaryContainer`, `padding(horizontal = 16.dp, vertical = 8.dp)`. Inner: `"Total Volume"` (`typography.labelLarge`), value (`typography.titleLarge`, `FontWeight.Bold`), both `colorScheme.onSecondaryContainer`.
- Per-exercise items in `LazyColumn` (`verticalArrangement = spacedBy(0.dp)`), `HorizontalDivider` between items.
- Exercise name: `typography.titleSmall`, `FontWeight.Bold`.
- Set rows: "Set N" (`typography.bodySmall`, `FontWeight.Medium`), reps×weight (`typography.bodySmall`, `FontWeight.Medium`).
- Skipped sets: "Skipped" text `color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)`; `isDone == false` renders as skipped.
- AMRAP badge: `CircleShape` surface, `color = colorScheme.tertiaryContainer`, text `colorScheme.onTertiaryContainer`, `typography.labelSmall`, `FontWeight.Bold`.
- Action buttons: column `padding(horizontal = 16.dp, vertical = 12.dp)`, `verticalArrangement = spacedBy(8.dp)`. "Finish Workout" `Button` (pill, `FontWeight.Bold`); "Resume Workout" `OutlinedButton` (pill).

### History Screen

`LargeTopAppBar` "History" + `exitUntilCollapsedScrollBehavior` + `nestedScroll`.

- Sessions grouped by "THIS WEEK" / "LAST WEEK" / "MONTH YEAR" group headers (`typography.labelSmall`, `FontWeight.Bold`, `colorScheme.onSurfaceVariant`).
- `LazyColumn`: `contentPadding = PaddingValues(start=16, end=16, top=4, bottom=16.dp)`, `verticalArrangement = spacedBy(10.dp)`.
- Session card: `RoundedCornerShape(20.dp)`, `containerColor = colorScheme.surfaceVariant`, expandable via `AnimatedVisibility(spring(DampingRatioMediumBouncy))`.
- Session card header: workout name (`typography.titleMedium`, `FontWeight.ExtraBold`), date (`typography.bodySmall`, `colorScheme.onSurfaceVariant`), duration `SuggestionChip` (`containerColor = colorScheme.secondaryContainer`), expand/collapse `Icons.Rounded.ExpandMore/Less` (`tint = colorScheme.onSurfaceVariant`).
- Expanded detail: inner `Card` `RoundedCornerShape(12.dp)` `containerColor = colorScheme.surface`. Exercise name `typography.bodyMedium`, `FontWeight.SemiBold`. Set summary `typography.bodySmall`, `colorScheme.onSurfaceVariant`. Caps at 3 exercises, shows "+N more exercises" in `colorScheme.primary`.
- Empty state: centered `Icons.Rounded.FitnessCenter` (`tint = colorScheme.onSurfaceVariant`) + text.

### `WorkoutSessionCard` (shared — Home + History)

Defined in `HistoryScreen.kt`. Used on both the Home screen (recent workouts) and potentially inside History expanded views.

- Day name: `typography.titleMedium`, `FontWeight.Bold`, `color = colorScheme.primary`
- Date: `typography.labelMedium`, `colorScheme.onSurfaceVariant`
- Duration badge: `typography.labelLarge`, `colorScheme.secondaryContainer`
- Exercise name: `typography.bodyLarge`, `FontWeight.Medium`
- Muscle group: `color = colorScheme.primary`; equipment: `color = colorScheme.secondary`
- Set row: `typography.bodyMedium`

### Settings Screen

`LargeTopAppBar` "Settings" + `exitUntilCollapsedScrollBehavior` + `nestedScroll`. `LazyColumn` with `HorizontalDivider` between each `ListItem`. Icon family: `Icons.Outlined.*`.

- First row: Theme selector — `SingleChoiceSegmentedButtonRow` with three `SegmentedButton`s: Dark / Light / System. Updates `theme_mode` string preference in DataStore via `RestTimerPreferencesRepository`.
- Remaining rows: "Timer Settings", "Manage Exercises", "Manage Equipment", "Manage Routines" — each navigates to respective route.
- All leading icons `tint = colorScheme.onSurfaceVariant`; trailing chevrons `tint = colorScheme.onSurfaceVariant`.
- List item headlines: `FontWeight.SemiBold`.

### Timer Settings Screen

`LargeTopAppBar` "Timer Settings" + `exitUntilCollapsedScrollBehavior` + `nestedScroll`. Three timer rows: Easy threshold, Hard threshold (between sets), and Between-exercises threshold. Trailing value: `typography.bodyLarge`, `FontWeight.Bold`, `color = colorScheme.primary`, formatted as `"M:SS"` if ≥ 60s else `"Xs"`. Tap opens `AlertDialog` with `OutlinedTextField` (digits only, "s" suffix).

### Routines Screen

`TopAppBar` + `enterAlwaysScrollBehavior` + `nestedScroll`. `ExtendedFloatingActionButton` "New Routine" (+ `Icons.Default.Add`).

- Routine cards: `RoundedCornerShape(16.dp)`, `containerColor = colorScheme.surfaceVariant`.
- Active routine: name `color = colorScheme.primary`, select icon `Icons.Default.CheckCircle` `tint = colorScheme.primary`.
- Inactive routine: name `colorScheme.onSurface`, select icon `Icons.Default.RadioButtonUnchecked` `tint = colorScheme.onSurfaceVariant`.
- Day count: `typography.bodySmall`, `colorScheme.onSurfaceVariant`.
- Delete icon trailing: `tint = colorScheme.error`.
- `LazyColumn`: `padding(horizontal = 16.dp)`, `verticalArrangement = spacedBy(10.dp)`, `contentPadding = PaddingValues(vertical = 8.dp)`.

### Routine Detail Screen

`TopAppBar` + `enterAlwaysScrollBehavior` + `nestedScroll`. Trailing "Edit" action: `Icons.Default.Edit`. Loading state: centered `CircularProgressIndicator`.

- Day cards: `RoundedCornerShape(16.dp)`, `containerColor = colorScheme.surfaceVariant`, `padding(horizontal=16, vertical=6.dp)`, inner `padding(16.dp)`.
- Day name: `typography.titleMedium`, `color = colorScheme.primary`.
- `HorizontalDivider` with `padding(vertical = 8.dp)` between day name and exercise list.
- Exercise name: `typography.bodyLarge`. Sets summary: `typography.bodySmall`. Equipment: `typography.bodySmall`, `color = colorScheme.secondary`.

### Create / Edit Routine Screen

`TopAppBar` + `enterAlwaysScrollBehavior` + `nestedScroll`. Title: "New Routine" (create) or "Edit Routine" (edit). Trailing: `TextButton` "Save" (disabled until name non-blank and days non-empty).

- `LazyColumn` inner `padding(16.dp)`, `verticalArrangement = spacedBy(16.dp)`.
- `OutlinedTextField` for routine name and description.
- `DayCard` per day: `OutlinedCard`, `RoundedCornerShape(16.dp)`, header row with expand/collapse icon + name field + move up/down + delete.
- "Add Day" button: `FilledTonalButton`, `CircleShape`, `fillMaxWidth().height(52.dp)`.
- Exercise picker: `AlertDialog` (`ExercisePicker`) with flat `LazyColumn` list. (Not a bottom sheet.)

### Exercises Screen

`TopAppBar` + `enterAlwaysScrollBehavior` + `nestedScroll`. `ExtendedFloatingActionButton` "Add Exercise".

- `ListItem` per exercise. `HorizontalDivider` between items.
- Trailing delete: `Icons.Rounded.Delete`, `tint = colorScheme.error`.
- Add/Edit: `AlertDialog` with name, muscle group, description, and equipment dropdown (`OutlinedCard` + `DropdownMenu`).
- Delete confirm: `AlertDialog` with confirm text `color = colorScheme.error`.

### Equipment Screen

`TopAppBar` + `enterAlwaysScrollBehavior` + `nestedScroll`. `ExtendedFloatingActionButton` "Add Equipment". Same pattern as Exercises: `ListItem`, delete icon, single-field `AlertDialog`.

---

## 9. Interaction Patterns

### Workout Cursor Model

| Action | Effect on cursor | Effect on `isDone` |
|---|---|---|
| `completeCurrentSet()` | Advances to next set; last set → `requestFinish()` | Sets `isDone = true` |
| `goToPreviousSet()` | setIndex − 1; wraps to previous exercise's last set | None |
| `skipExercise()` | exerciseIndex + 1, setIndex = 0; last exercise → `requestFinish()` | None |

`isDone` is only set by `completeCurrentSet()`. Summary renders `isDone = false` as "Skipped". Do not add a separate skip flag.

### `toggleSetDone` Behavior

- AMRAP sets: `toggleSetDone` does **nothing**. Only `updateSetReps` (via the reps dialog) marks an AMRAP set done.
- Completed non-AMRAP sets: tapping **decrements reps by 1** on each tap; only resets to `originalReps` + `isDone = false` when reps reach zero. It is not a simple toggle.

### Workout Finish Flow

```
completeCurrentSet() on last set
  └── requestFinish()         → stops timer, showSummary = true
        └── LaunchedEffect     → navigates to WorkoutSummaryRoute

  Summary: Resume
    └── resumeWorkout()        → showSummary = false, cursor unchanged

  Summary: Confirm Finish
    └── finishWorkout()        → persists to DB, isFinished = true
          └── LaunchedEffect   → pops back to Home
```

`requestFinish()` and `finishWorkout()` are distinct. `requestFinish()` does not save. `finishWorkout()` saves.

### Rest Timer Events

`restTimerEvents: SharedFlow<RestTimerEvent>` is not part of `uiState`. Collect it in a `LaunchedEffect` in the workout composable for haptics/audio. Events: `EasyMilestone`, `HardMilestone`, `ExerciseMilestone`.

---

## 10. Implementation Notes

- Gradient backgrounds: `Brush.linearGradient(...)` passed to `Modifier.background(brush = brush)`.
- Icons: `Icons.Rounded.*` default; `Icons.Outlined.*` for Settings; `Icons.Default.*` for selection/edit/add.
- `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` is currently required for certain M3 Expressive types but `MotionScheme.expressive()` cannot be called — do not add it to `Theme.kt` until stable.
- Edge-to-edge: `enableEdgeToEdge()` in `MainActivity`. Home hero consumes `WindowInsets.statusBars` padding manually.
- No new dependencies required for any currently specified UI.
- `formatWeight(Double): String` strips `.0`: `80.0 → "80"`, `82.5 → "82.5"`. Use this everywhere weight is displayed.
- `formatElapsedTime(Long): String` produces `HH:MM:SS` when hours > 0, else `MM:SS`. Use everywhere elapsed time is shown.
- `formatRestSeconds(Int): String` (in `TimerSettingsScreen.kt`) produces `"M:SS"` if ≥ 60s, else `"Xs"`. Currently local to that file — extract if reused.
