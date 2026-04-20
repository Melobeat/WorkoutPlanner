# Design Guidelines Overhaul — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current light/dynamic-color M3 theme with a deliberate Dark & Deep fixed theme — near-black surfaces with subtle purple tint, gradient hero as the primary color moment, and a premium active exercise card with a gradient header strip.

**Architecture:** Work bottom-up: (1) establish the color palette and theme, (2) wire `motionScheme`, (3) add `useDynamicColor` DataStore preference + Settings toggle, (4) update `ExerciseCard` with the new gradient header strip, (5) update `MainActivity` mini-bar, (6) sweep remaining screens for any hardcoded light-mode references, (7) rewrite `docs/design-guidelines.md`.

**Tech Stack:** Kotlin, Jetpack Compose, Material3 Expressive, Room, Hilt, DataStore Preferences, Navigation Compose.

---

## File Map

| File | Action | Change |
|---|---|---|
| `ui/theme/Color.kt` | Modify | Replace static seed palette with Dark & Deep hex values |
| `ui/theme/Theme.kt` | Modify | Hard-code dark theme, add `useDynamicColor` param, wire `motionScheme` |
| `data/RestTimerPreferencesRepository.kt` | Modify | Add `useDynamicColor` boolean preference key and read/write support |
| `ui/TimerSettingsScreen.kt` | Modify | Expose `useDynamicColor` from `TimerSettingsViewModel`; add Theme toggle row to `SettingsScreen.kt` |
| `ui/SettingsScreen.kt` | Modify | Add Theme `ListItem` row with `Switch` |
| `MainActivity.kt` | Modify | Pass `useDynamicColor` into `WorkoutPlannerTheme`; update mini-bar colors |
| `ui/ExerciseCard.kt` | Modify | Replace active-set left-border approach with gradient header strip |
| `docs/design-guidelines.md` | Rewrite | Replace content with the new authoritative spec |

---

## Task 1: Replace the color palette

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/theme/Color.kt`

- [ ] **Step 1: Replace Color.kt**

Replace the entire file content:

```kotlin
package de.melobeat.workoutplanner.ui.theme

import androidx.compose.ui.graphics.Color

// ── Dark & Deep fixed palette ─────────────────────────────────────────────────

// Surfaces
val DarkBackground      = Color(0xFF0D0D14)  // screen background
val DarkSurface         = Color(0xFF0D0D14)  // same as background
val DarkSurfaceContainer     = Color(0xFF1A1A28)  // cards, elevated surfaces
val DarkSurfaceContainerLow  = Color(0xFF141422)  // nav bar
val DarkSurfaceContainerLowest = Color(0xFF12102A) // stepper inner cards

// Borders / dividers
val DarkOutlineVariant  = Color(0x12FFFFFF)  // rgba(255,255,255,0.07) ≈ 0x12

// Text
val DarkOnBackground    = Color(0xFFEEE8FF)  // primary text (lavender-white)
val DarkOnSurface       = Color(0xFFEEE8FF)
val DarkOnSurfaceVariant = Color(0xFF7A7590) // secondary / supporting text

// Accent
val DarkPrimary         = Color(0xFFD0BCFF)  // active indicators, borders
val DarkPrimaryContainer = Color(0xFF3B2F6B) // tonal button containers, nav indicator pill
val DarkOnPrimary       = Color(0xFF1C1040)
val DarkOnPrimaryContainer = Color(0xFFEEE8FF)

val DarkSecondary       = Color(0xFFFFB0C8)
val DarkSecondaryContainer = Color(0xFF5E2750)
val DarkOnSecondary     = Color(0xFF3E0030)
val DarkOnSecondaryContainer = Color(0xFFFFD8E4)

val DarkTertiary        = Color(0xFFEADDFF)
val DarkTertiaryContainer = Color(0xFF4A3278)
val DarkOnTertiary      = Color(0xFF21005D)
val DarkOnTertiaryContainer = Color(0xFFEADDFF)

val DarkError           = Color(0xFFFFB4AB)
val DarkErrorContainer  = Color(0xFF93000A)
val DarkOnError         = Color(0xFF690005)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkSurfaceTint     = DarkPrimary

// ── Gradient stop colors (used by Brush.linearGradient at call sites) ─────────
val GradientHeroStart   = Color(0xFF4A0080)  // hero banner start
val GradientHeroMid     = Color(0xFF6750A4)  // hero banner mid / CTA start
val GradientHeroEnd     = Color(0xFFB5488A)  // hero banner end / CTA end
val GradientCardStart   = Color(0xFF2D1060)  // active exercise card header strip start
val GradientCardEnd     = Color(0xFF4A2280)  // active exercise card header strip end
```

- [ ] **Step 2: Build**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL (Color.kt is not yet wired into the theme — no compile errors).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/theme/Color.kt
git commit -m "refactor(theme): replace seed palette with Dark & Deep color tokens"
```

---

## Task 2: Rewrite Theme.kt — dark-first, motionScheme wired

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/theme/Theme.kt`

- [ ] **Step 1: Replace Theme.kt**

```kotlin
package de.melobeat.workoutplanner.ui.theme

import android.os.Build
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val AppDarkColorScheme = darkColorScheme(
    primary                = DarkPrimary,
    onPrimary              = DarkOnPrimary,
    primaryContainer       = DarkPrimaryContainer,
    onPrimaryContainer     = DarkOnPrimaryContainer,
    secondary              = DarkSecondary,
    onSecondary            = DarkOnSecondary,
    secondaryContainer     = DarkSecondaryContainer,
    onSecondaryContainer   = DarkOnSecondaryContainer,
    tertiary               = DarkTertiary,
    onTertiary             = DarkOnTertiary,
    tertiaryContainer      = DarkTertiaryContainer,
    onTertiaryContainer    = DarkOnTertiaryContainer,
    error                  = DarkError,
    errorContainer         = DarkErrorContainer,
    onError                = DarkOnError,
    onErrorContainer       = DarkOnErrorContainer,
    background             = DarkBackground,
    onBackground           = DarkOnBackground,
    surface                = DarkSurface,
    onSurface              = DarkOnSurface,
    surfaceVariant         = DarkSurfaceContainer,
    onSurfaceVariant       = DarkOnSurfaceVariant,
    outline                = DarkOutlineVariant,
    outlineVariant         = DarkOutlineVariant,
    surfaceTint            = DarkSurfaceTint,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WorkoutPlannerTheme(
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // App is dark-only. No light theme.
    // Dynamic color opt-in always uses the dark variant.
    val colorScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(LocalContext.current)
    } else {
        AppDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}
```

- [ ] **Step 2: Build**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL. The app now renders in Dark & Deep at all call sites that use `MaterialTheme.colorScheme.*`.

- [ ] **Step 3: Run unit tests**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:testDebugUnitTest
```

Expected: All tests pass. Theme change has no effect on pure unit tests.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/theme/Theme.kt
git commit -m "feat(theme): dark-first fixed theme with motionScheme.expressive() wired"
```

---

## Task 3: Add `useDynamicColor` to DataStore

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/data/RestTimerPreferencesRepository.kt`

- [ ] **Step 1: Add the preference key and read/write support**

Open `RestTimerPreferencesRepository.kt`. The file currently has three `intPreferencesKey` entries and a `RestTimerSettings` data class. Add a `booleanPreferencesKey` for `useDynamicColor` and expose it as a separate flow + setter (keeping the existing `settings` flow unchanged):

```kotlin
package de.melobeat.workoutplanner.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val DEFAULT_BETWEEN_SETS_EASY_SECONDS = 90
const val DEFAULT_BETWEEN_SETS_HARD_SECONDS = 180
const val DEFAULT_BETWEEN_EXERCISES_SECONDS = 60

data class RestTimerSettings(
    val betweenSetsEasySeconds: Int = DEFAULT_BETWEEN_SETS_EASY_SECONDS,
    val betweenSetsHardSeconds: Int = DEFAULT_BETWEEN_SETS_HARD_SECONDS,
    val betweenExercisesSeconds: Int = DEFAULT_BETWEEN_EXERCISES_SECONDS
)

class RestTimerPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val BETWEEN_SETS_EASY   = intPreferencesKey("between_sets_easy_seconds")
        val BETWEEN_SETS_HARD   = intPreferencesKey("between_sets_hard_seconds")
        val BETWEEN_EXERCISES   = intPreferencesKey("between_exercises_seconds")
        val USE_DYNAMIC_COLOR   = booleanPreferencesKey("use_dynamic_color")
    }

    val settings: Flow<RestTimerSettings> = dataStore.data.map { prefs ->
        RestTimerSettings(
            betweenSetsEasySeconds = prefs[BETWEEN_SETS_EASY] ?: DEFAULT_BETWEEN_SETS_EASY_SECONDS,
            betweenSetsHardSeconds = prefs[BETWEEN_SETS_HARD] ?: DEFAULT_BETWEEN_SETS_HARD_SECONDS,
            betweenExercisesSeconds = prefs[BETWEEN_EXERCISES] ?: DEFAULT_BETWEEN_EXERCISES_SECONDS
        )
    }

    val useDynamicColor: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[USE_DYNAMIC_COLOR] ?: false
    }

    suspend fun update(settings: RestTimerSettings) {
        dataStore.edit { prefs ->
            prefs[BETWEEN_SETS_EASY] = settings.betweenSetsEasySeconds
            prefs[BETWEEN_SETS_HARD] = settings.betweenSetsHardSeconds
            prefs[BETWEEN_EXERCISES] = settings.betweenExercisesSeconds
        }
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[USE_DYNAMIC_COLOR] = enabled
        }
    }
}
```

- [ ] **Step 2: Write the failing test**

Open `app/src/test/java/de/melobeat/workoutplanner/RestTimerPreferencesRepositoryTest.kt`. Add a new test at the bottom of the class:

```kotlin
@Test
fun `useDynamicColor defaults to false and can be toggled`() = runTest {
    repository.useDynamicColor.test {
        assertThat(awaitItem()).isFalse()
        repository.setUseDynamicColor(true)
        assertThat(awaitItem()).isTrue()
        repository.setUseDynamicColor(false)
        assertThat(awaitItem()).isFalse()
        cancelAndIgnoreRemainingEvents()
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:testDebugUnitTest --tests "de.melobeat.workoutplanner.RestTimerPreferencesRepositoryTest.useDynamicColor defaults to false and can be toggled"
```

Expected: FAILED — `useDynamicColor` property does not exist yet (this step verifies the test is actually exercising the new code).

> Note: If the build already includes Step 1's changes, the test may pass immediately — that's fine, proceed to Step 4.

- [ ] **Step 4: Run all tests to verify nothing is broken**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:testDebugUnitTest
```

Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/data/RestTimerPreferencesRepository.kt
git add app/src/test/java/de/melobeat/workoutplanner/RestTimerPreferencesRepositoryTest.kt
git commit -m "feat(data): add useDynamicColor preference to DataStore"
```

---

## Task 4: Expose `useDynamicColor` from TimerSettingsViewModel

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/TimerSettingsScreen.kt`

`TimerSettingsViewModel` is defined inside `TimerSettingsScreen.kt`. It already injects `RestTimerPreferencesRepository`. Add `useDynamicColor` state and a toggle method.

- [ ] **Step 1: Add useDynamicColor to TimerSettingsViewModel**

Locate `TimerSettingsViewModel` in `TimerSettingsScreen.kt`. It currently has a `timerSettings` `StateFlow` and an `update` method. Add after the existing `timerSettings` stateIn block:

```kotlin
val useDynamicColor: StateFlow<Boolean> = repository.useDynamicColor
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

fun setUseDynamicColor(enabled: Boolean) {
    viewModelScope.launch(ioDispatcher) {
        repository.setUseDynamicColor(enabled)
    }
}
```

- [ ] **Step 2: Build to confirm no errors**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/TimerSettingsScreen.kt
git commit -m "feat(vm): expose useDynamicColor state and toggle in TimerSettingsViewModel"
```

---

## Task 5: Add Theme toggle row to SettingsScreen

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/SettingsScreen.kt`

- [ ] **Step 1: Add SettingsScreen parameter and Theme row**

`SettingsScreen` currently takes no ViewModel — it only has navigation callbacks. Add a `timerSettingsViewModel: TimerSettingsViewModel = hiltViewModel()` parameter (it's in the same Settings nav graph so `hiltViewModel()` is correct here), collect `useDynamicColor`, and insert the Theme row above the Timer Settings row.

Replace the `SettingsScreen` composable:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToExercises: () -> Unit,
    onNavigateToRoutines: () -> Unit,
    onNavigateToEquipment: () -> Unit,
    onNavigateToTimerSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    timerSettingsViewModel: TimerSettingsViewModel = hiltViewModel()
) {
    val useDynamicColor by timerSettingsViewModel.useDynamicColor.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = innerPadding) {
            item {
                ListItem(
                    headlineContent = { Text("Theme", fontWeight = FontWeight.SemiBold) },
                    supportingContent = {
                        Text(if (useDynamicColor) "Dynamic color (wallpaper)" else "Custom dark theme")
                    },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = useDynamicColor,
                            onCheckedChange = { timerSettingsViewModel.setUseDynamicColor(it) }
                        )
                    }
                )
                HorizontalDivider()
            }
            item {
                SettingsListItem(
                    "Timer Settings", "Rest timer durations between sets and exercises",
                    Icons.Outlined.Timer, onNavigateToTimerSettings
                )
                HorizontalDivider()
            }
            item {
                SettingsListItem(
                    "Manage Exercises", "Add, edit or delete exercises",
                    Icons.Outlined.FitnessCenter, onNavigateToExercises
                )
                HorizontalDivider()
            }
            item {
                SettingsListItem(
                    "Manage Equipment", "Dumbbells, barbells, machines, etc.",
                    Icons.Outlined.Construction, onNavigateToEquipment
                )
                HorizontalDivider()
            }
            item {
                SettingsListItem(
                    "Manage Routines", "Create and organize your workout routines",
                    Icons.AutoMirrored.Outlined.ListAlt, onNavigateToRoutines
                )
            }
        }
    }
}
```

Note: `Icons.Outlined.Palette` requires `material-icons-extended` which is already in the dependencies. Also note `TopAppBar` is upgraded to `LargeTopAppBar` + `exitUntilCollapsedScrollBehavior` per the spec.

- [ ] **Step 2: Build**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/SettingsScreen.kt
git commit -m "feat(settings): add Theme toggle row with dynamic color switch"
```

---

## Task 6: Wire `useDynamicColor` into WorkoutPlannerTheme in MainActivity

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/MainActivity.kt`

- [ ] **Step 1: Collect useDynamicColor and pass into theme**

`MainActivity` currently calls `WorkoutPlannerTheme { WorkoutPlannerApp() }` with no arguments. The `TimerSettingsViewModel` is already injectable via Hilt in Activity context. However, to avoid over-scoping, read the preference directly from the repository via a lightweight Activity ViewModel or use `timerSettingsViewModel` at Activity level.

The cleanest approach given the current architecture: inject `TimerSettingsViewModel` at the `WorkoutPlannerApp` level (it's an `@HiltViewModel`, so `hiltViewModel()` works inside any `@AndroidEntryPoint` composable context, which `MainActivity.setContent { WorkoutPlannerTheme { ... } }` is not). Instead, collect the preference inside `WorkoutPlannerApp` using `hiltViewModel<TimerSettingsViewModel>()` since `WorkoutPlannerApp` is called from within `setContent` which provides the Hilt composition locals.

Replace `MainActivity.onCreate` and `WorkoutPlannerApp`:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
        val timerSettingsViewModel: TimerSettingsViewModel = hiltViewModel()
        val useDynamicColor by timerSettingsViewModel.useDynamicColor.collectAsStateWithLifecycle()
        WorkoutPlannerTheme(useDynamicColor = useDynamicColor) {
            WorkoutPlannerApp()
        }
    }
}
```

Add the required import for `TimerSettingsViewModel`:
```kotlin
import de.melobeat.workoutplanner.ui.TimerSettingsViewModel
```

- [ ] **Step 2: Update mini-bar to use dark palette colors**

In `WorkoutPlannerApp`, the mini-bar `Surface` currently uses `color = MaterialTheme.colorScheme.primaryContainer`. Since `primaryContainer` is now `#3B2F6B` (dark purple), this is fine — but the icon and text tints use `onPrimaryContainer`. Verify the colors read correctly from `MaterialTheme.colorScheme` and are not hardcoded. No change needed if they already reference tokens.

Also confirm the mini-bar top border is drawn. Currently it has no border — add a `border` modifier to the `Surface`:

Locate the `Surface` block for the mini-bar in `WorkoutPlannerApp`. Add `border = BorderStroke(1.dp, DarkOutlineVariant)` to the `Surface`:

```kotlin
Surface(
    onClick = { ... },
    color = MaterialTheme.colorScheme.primaryContainer,
    tonalElevation = 0.dp,
    border = BorderStroke(1.dp, DarkOutlineVariant),
    modifier = Modifier
        .fillMaxWidth()
        .height(64.dp)
)
```

Add import:
```kotlin
import androidx.compose.foundation.BorderStroke
import de.melobeat.workoutplanner.ui.theme.DarkOutlineVariant
```

- [ ] **Step 3: Build and run unit tests**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug && JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/MainActivity.kt
git commit -m "feat(main): wire useDynamicColor into WorkoutPlannerTheme; add mini-bar top border"
```

---

## Task 7: Gradient header strip on active ExerciseCard

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseCard.kt`

The current active set uses a 3dp left accent border drawn via `drawBehind` and a `primaryContainer.copy(alpha = 0.25f)` background tint. The spec replaces the active card's header zone with a gradient strip and moves the exercise name + progress label into it.

The `ExerciseCard` composable currently renders:
- A single `Card` wrapping everything
- A header `Row` at top (exercise name + state labels + swap button)
- An `AnimatedVisibility` body with per-set rows

The new structure splits the active card into a `Column` with two zones:
1. **Gradient header `Box`** — `topStart = 16.dp, topEnd = 16.dp` rounded, gradient background, contains progress label + exercise name
2. **Card body `Column`** — `#1A1A28` background, `bottomStart = 16.dp, bottomEnd = 16.dp` rounded, contains set rows (unchanged)

For non-active cards, the existing header row structure is preserved unchanged.

- [ ] **Step 1: Add gradient header composable**

At the top of the `ExerciseCard.kt` file, after the existing imports, add a private composable for the gradient strip:

```kotlin
@Composable
private fun ActiveCardHeader(
    exercise: ExerciseUiState,
    exerciseIndex: Int,
    totalExercises: Int,
    onSwapExercise: (Exercise) -> Unit,
) {
    val cardGradient = Brush.linearGradient(
        colors = listOf(GradientCardStart, GradientCardEnd),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = cardGradient,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Text(
                text = "EXERCISE ${exerciseIndex + 1} OF $totalExercises",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f),
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = { /* swap dialog trigger handled by parent */ },
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.18f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Rounded.SwapHoriz, contentDescription = "Swap exercise", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Swap", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
```

Add required imports if not already present:
```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.rounded.SwapHoriz
import de.melobeat.workoutplanner.ui.theme.GradientCardStart
import de.melobeat.workoutplanner.ui.theme.GradientCardEnd
```

- [ ] **Step 2: Integrate ActiveCardHeader into ExerciseCard**

In `ExerciseCard`, locate where `isActive` is true. The card is currently a single `Card(...)` composable. Change the outer container for the active state to a `Column` with two rounded zones instead:

Find the outer `Card(` call in `ExerciseCard`. It currently has a `borderColor`/`cardAlpha` computed above. Change so that when `isActive`, the card is replaced with:

```kotlin
if (isActive) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        ActiveCardHeader(
            exercise = exercise,
            exerciseIndex = exerciseIndex,
            totalExercises = totalExercises,
            onSwapExercise = onSwapExercise
        )
        // Card body — the set rows
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // set rows go here — move the AnimatedVisibility body content
            }
        }
    }
} else {
    // existing Card(...) for non-active states
}
```

> **Implementation note:** The exact restructuring depends on how the existing `Card` wraps both the header row and `AnimatedVisibility` body. Read the existing code carefully before editing. The goal is:
> - Active: `Column { ActiveCardHeader(...); Surface(surfaceVariant) { existing set rows } }`
> - Non-active: existing `Card(...)` unchanged
>
> The swap button is now inside `ActiveCardHeader`. Remove the swap button from the existing header row for the active state only (it was previously `if (isActive) SwapButton(...)`).

- [ ] **Step 3: Remove the left-border drawBehind from the active set row**

Inside the active set `Row` (within the set iteration), there is a `Modifier.drawBehind { ... }` that draws the 3dp left accent bar. Remove it — the gradient header strip replaces this visual indicator.

Also remove the `primaryContainer.copy(alpha = 0.25f)` background tint on the active set `Box` if present (the set row now lives inside the `surfaceVariant` body, no extra tint needed).

- [ ] **Step 4: Build**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Run tests**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:testDebugUnitTest
```

Expected: All tests pass (ExerciseCard has no unit tests; ActiveWorkoutViewModelTest is unaffected by UI changes).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseCard.kt
git commit -m "feat(workout): gradient header strip on active exercise card"
```

---

## Task 8: Sweep remaining screens for stale light-mode patterns

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/HomeScreen.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/HistoryScreen.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutScreen.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/RestTimerBanner.kt`

The dark theme token changes in Task 2 handle `MaterialTheme.colorScheme.*` usages automatically. This task targets usages that will not self-update:

1. **`HomeScreen.kt`:** The hero `Brush.linearGradient` uses `listOf(Purple10, Purple40, Pink40)`. Update to use the new gradient constants:
   ```kotlin
   // Before
   val heroBrush = Brush.linearGradient(listOf(Purple10, Purple40, Pink40))
   // After
   val heroBrush = Brush.linearGradient(
       colors = listOf(GradientHeroStart, GradientHeroMid, GradientHeroEnd),
       start = Offset(0f, Float.POSITIVE_INFINITY),
       end = Offset(Float.POSITIVE_INFINITY, 0f)
   )
   ```
   Add import: `import de.melobeat.workoutplanner.ui.theme.GradientHeroStart` etc.
   Remove unused import of `Purple10`, `Purple40`, `Pink40`.

2. **`HistoryScreen.kt`:** Uses `TopAppBarDefaults.enterAlwaysScrollBehavior()`. Per spec, `LargeTopAppBar` + `exitUntilCollapsedScrollBehavior` is required. Update:
   ```kotlin
   // Before
   val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
   // After
   val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
   ```
   Also change `TopAppBar` to `LargeTopAppBar` if the History screen still uses the standard variant.

3. **`WorkoutScreen.kt`:** Check for any `surfaceVariant` references used as card container colors in the file. With the new theme, `MaterialTheme.colorScheme.surfaceVariant` now maps to `DarkSurfaceContainer` (`#1A1A28`) — no change needed if using tokens.

4. **`RestTimerBanner.kt`:** Verify the banner uses `MaterialTheme.colorScheme.primaryContainer` / `onPrimaryContainer`. With the new token values, `primaryContainer = #3B2F6B` and `onPrimaryContainer = #EEE8FF` — the dark purple banner with lavender-white text is correct per spec. No hex changes needed if using tokens.

- [ ] **Step 1: Update HomeScreen.kt hero gradient**

Find `val heroBrush = Brush.linearGradient(listOf(Purple10, Purple40, Pink40))` in `HomeScreen.kt` and replace as shown above.

- [ ] **Step 2: Update HistoryScreen.kt scroll behavior and AppBar**

Find `enterAlwaysScrollBehavior` in `HistoryScreen.kt` and change to `exitUntilCollapsedScrollBehavior`. Change `TopAppBar` to `LargeTopAppBar` in the same scaffold.

- [ ] **Step 3: Build**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run all tests**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:testDebugUnitTest
```

Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/HomeScreen.kt
git add app/src/main/java/de/melobeat/workoutplanner/ui/HistoryScreen.kt
git commit -m "fix(ui): update hero gradient constants and History scroll behavior"
```

---

## Task 9: Rewrite docs/design-guidelines.md

**Files:**
- Rewrite: `docs/design-guidelines.md`

- [ ] **Step 1: Replace docs/design-guidelines.md**

Replace the entire file with the following authoritative content:

```markdown
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

- `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` required for `MotionScheme.expressive()`
- Gradient backgrounds: `Brush.linearGradient(...)` passed to `Modifier.background(brush)`
- Icons: `Icons.Rounded.*` from `material-icons-extended`
- No new dependencies required
- Edge-to-edge enabled via `enableEdgeToEdge()` in `MainActivity`
```

- [ ] **Step 2: Commit the updated design guidelines**

```bash
git add docs/design-guidelines.md
git commit -m "docs: overhaul design-guidelines.md — Dark & Deep theme, gradient header strip, dynamic color opt-in"
```

---

## Task 10: Final verification

- [ ] **Step 1: Run full build and tests**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew clean && JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Expected: BUILD SUCCESSFUL, all tests pass, lint clean (or only pre-existing warnings).

- [ ] **Step 2: Final commit if lint auto-fixed anything**

```bash
git status
# If any files changed:
git add -A
git commit -m "chore: lint fixes after design overhaul"
```
