# AGENTS.md — WorkoutPlanner

Single-module Android app (`de.melobeat.workoutplanner`). Kotlin + Jetpack Compose + Room + Hilt + Navigation Compose.

## Toolchain

| Item | Version |
|---|---|
| AGP | 9.1.1 |
| Kotlin | 2.3.20 |
| KSP | 2.3.2 |
| Navigation Compose | 2.9.7 |
| Gradle wrapper | 9.4.1 |
| JVM toolchain | 21 (via Foojay resolver in `settings.gradle.kts`; source/target compat: 11) |
| compileSdk / targetSdk | 36 |
| minSdk | **33** — no code path for API < 33 |
| Compose BOM | 2026.03.01 (includes M3 Expressive) |
| Room | 2.8.4 |
| Hilt | 2.59.2 |

## Developer Commands

```bash
./gradlew assembleDebug
./gradlew assembleRelease              # R8 full mode active
./gradlew :app:testDebugUnitTest       # unit tests (JVM, no device)
./gradlew connectedAndroidTest         # instrumented (device/emulator required)
./gradlew :app:lintDebug
./gradlew clean
```

No Makefile, no CI, no formatter config (Ktlint/Spotless not configured).

Git hooks are active (stored in `.git/hooks/`, not committed):
- **commit-msg**: enforces Conventional Commits format (`feat|fix|docs|style|refactor|test|chore|perf|ci|build|revert`)
- **pre-push**: runs `:app:testDebugUnitTest` before every push; bypass with `SKIP_TESTS=1 git push` (emergency only)

## Git Workflow (Git Flow)

- **`main`** — production-ready code only. Tagged releases.
- **`develop`** — integration branch for the next release. Feature branches merge here.
- Feature branches: `feat/<slug>` off `develop`, merge back to `develop`
- Release branches: `release/<version>` off `develop` for stabilization, merge to `main` + `develop`
- Hotfix branches: `hotfix/<slug>` off `main` for urgent fixes, merge to `main` + `develop`
- Branch naming: `feat/<slug>`, `fix/<slug>`, `release/<version>`, `hotfix/<slug>`
- Commit style: Conventional Commits — `feat(scope): description` (≤72 chars subject)
- Never commit directly to `main` or `develop`

**Initial setup:**
```bash
# develop branch already created locally
git push origin develop
git branch --set-upstream-to=origin/develop develop
```

Gradle has `org.gradle.configuration-cache=true` and `org.gradle.caching=true` active. After schema changes that require `clean`, run `./gradlew clean` normally — the configuration cache is compatible.

**JAVA_HOME**: OpenJDK is installed on the system `PATH` (Java 26). Gradle will auto-detect it via Foojay resolver, which provisions Java 21 for the build. No manual `JAVA_HOME` setup required.

**Gradle heap**: `-Xmx4g` in `gradle.properties`. On machines with < 6 GB free RAM, builds may OOM.

## Module Structure

```
app/src/main/java/de/melobeat/workoutplanner/
  WorkoutApplication.kt        @HiltAndroidApp
  MainActivity.kt              @AndroidEntryPoint; NavigationSuiteScaffold host; hosts mini-bar
  data/                        Room entities (Entities.kt), DAO, database, repository, mappers,
                               InitialData.kt (seed JSON parsing), DataStore wrapper
  di/                          DatabaseModule — provides DB, DAO, repo, DataStore, @IoDispatcher
  domain/
    model/                     Domain models (Exercise, Routine, RoutineSet, WorkoutDay,
                               ExerciseHistory, Equipment, UserProfile, SideType, SampleData)
    util/                      Pure utility functions (filterExercises)
  ui/
    navigation/                NavRoutes.kt (type-safe routes), WorkoutNavGraph.kt
    theme/                     Theme.kt, Color.kt, Type.kt
    common/                    Shared composables (ExerciseCard, RestTimerBanner, etc.)
    feature/                   Feature-scoped screens + ViewModels
      workout/                 WorkoutScreen, WorkoutSummaryScreen, ActiveWorkoutViewModel, WorkoutUiState
      history/                 HistoryScreen, HistoryViewModel
      home/                    HomeScreen, HomeViewModel
      routines/                RoutinesScreen, RoutineDetailScreen, CreateRoutineScreen, RoutinesViewModel
      exercises/               ExercisesScreen, ExerciseLibraryViewModel
      equipment/               EquipmentScreen
      settings/                SettingsScreen, TimerSettingsScreen, TimerSettingsViewModel
      profile/                 ProfileScreen, UserProfileViewModel
    FormatElapsedTime.kt       Standalone util; has its own unit test
app/src/main/assets/           equipment.json, exercises.json (DB seed — loaded only on onCreate)
                               equipment_schema.json, exercises_schema.json (not loaded at runtime)
docs/design-guidelines.md      Authoritative UI spec — **read before any UI change**. Supersedes older spec files in `docs/superpowers/specs/`.
```

## Critical Hilt / ViewModel Scoping

`ActiveWorkoutViewModel` is **Activity-scoped**, not NavBackStackEntry-scoped:

```kotlin
viewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity)
```

Every other `@HiltViewModel` inside `NavHost` destinations uses `hiltViewModel()`. Mixing these causes a runtime crash.

## Room — No Migrations

- DB version: **8**; entities: `ExerciseEntity`, `RoutineEntity`, `WorkoutDayEntity`, `WorkoutDayExerciseEntity`, `WorkoutHistoryEntity`, `ExerciseHistoryEntity`, `EquipmentEntity`.
- `fallbackToDestructiveMigrationFrom(1,2,3,4,5,6,7)` — schema changes **destroy all data**. No migration objects exist.
- `exportSchema = false` — no schema export files.
- When adding an entity/column: bump version integer + update `@Database` entities list.
- Seed data in `assets/` reloads only on destructive migration (via `RoomDatabase.Callback` in `WorkoutDatabase.getDatabase()`), not on app update.

## TypeConverter — RoutineSet Is a JSON Blob

`WorkoutDayExerciseEntity.routineSets` stores `List<RoutineSet>` as a JSON string. `RoutineSet` must remain `@Serializable`. Never add non-serializable fields to it.

## Navigation — Type-Safe Routes

Routes are `@Serializable` objects/data classes in `NavRoutes.kt`, not string constants. Arguments extracted via `backStackEntry.toRoute<T>()`.

`SettingsGraphRoute` is the **nested graph key** with `SettingsRoute` as its `startDestination`. The following routes live **inside** this nested graph:
- `SettingsRoute`, `TimerSettingsRoute`
- `RoutinesRoute`
- `RoutineDetailRoute(routineId: String)` — required arg, no default
- `CreateRoutineRoute(routineId: String? = null)` — `null` = create new, non-null = edit existing
- `ExercisesRoute`, `EquipmentRoute`
- `ProfileRoute`

Navigate to `SettingsGraphRoute` (not `SettingsRoute`) — Navigation Compose resolves the start destination automatically.

Top-level destinations (outside the nested graph): `HomeRoute`, `HistoryRoute`, `ActiveWorkoutRoute`, `WorkoutSummaryRoute`.

Composables receive navigation lambdas — never `NavController` directly.

## Workout State Conventions

- `isDone` on a set is set only by `completeCurrentSet()`. Skipped sets stay `isDone = false`; the Summary screen renders them as "Skipped". Do not add a separate skip flag.
- `WEIGHT_STEP = 2.5` (kg), hardcoded.
- `formatWeight()` strips `.0`: `80.0 → "80"`, `82.5 → "82.5"`.
- `SetUiState` has `originalReps: String` — used to reset reps when tapping a completed set back to zero.
- `ExerciseUiState.lastSets: List<Pair<Double, Int>>` — populated from last session history for pre-filling weight.
- `ExerciseUiState.isExpanded: Boolean = true` — exercises start expanded; advancing to next exercise auto-collapses the previous one.

### showSummary vs isFinished

These are distinct flags with different lifecycles — do not conflate:
- `requestFinish()` → sets `showSummary = true`, stops timer, captures `summaryDurationMs`. Does **not** save data.
- `finishWorkout()` → persists to DB, sets `isFinished = true`. This is the actual save.
- `resumeWorkout()` → clears `showSummary`, restarts timer offset by `summaryDurationMs`.

### RestTimerContext / RestTimerEvent

- `RestTimerContext`: `BetweenSets` (two thresholds: easy + hard) vs. `BetweenExercises` (single threshold). Timer behavior differs.
- `RestTimerEvent`: `EasyMilestone`, `HardMilestone`, `ExerciseMilestone` — emitted from `restTimerEvents: SharedFlow`. UI must collect this in a `LaunchedEffect` for haptics/audio. It is **not** part of `uiState`.

### ExerciseHistory

`ExerciseHistory` is defined in `domain/model/ExerciseHistory.kt`. Required when calling `repository.finishWorkout(...)`:
```kotlin
data class ExerciseHistory(
    val exerciseId: String,
    val reps: Int,
    val weight: Double,
    val setIndex: Int = 1,
    val isAmrap: Boolean = false
)
```
`finishWorkout` silently skips sets where `reps`/`weight` cannot be parsed (logs a warning, does not abort).

## ActiveWorkoutViewModel — Non-Obvious Behaviors

- **AMRAP sets**: `toggleSetDone` does **nothing** on AMRAP sets. Only `updateSetReps` (called from the reps dialog) marks an AMRAP set done.
- **Tapping a done set**: `toggleSetDone` on a completed non-AMRAP set **decrements reps by 1** on each tap; only resets to `originalReps` + `isDone = false` when reps reach zero. It is not a simple toggle.
- **`setRepsValue` vs `updateSetReps`**: `setRepsValue` (used by steppers) does NOT flip `isDone`. `updateSetReps` (used by AMRAP dialog) DOES flip `isDone = true`. Choose correctly when adding new reps-edit flows.
- **`addSet`**: always adds weight="0", reps="0", isAmrap=false. No defaults from history.
- **`removeSet`**: guards minimum 1 set per exercise (`if (sets.size <= 1) return`).
- **`swapExercise(exerciseIndex, newExercise)`**: replaces the exercise, preserving set count from the replaced exercise.
- **`reorderExercise(from, to)`**: reorders exercises via mutable list remove+insert.
- **`timerSettings`**: eagerly initialized via `stateIn(SharingStarted.Eagerly)` — loads from DataStore immediately on ViewModel creation.

## upsertRoutine Behavior

`upsertRoutine` in `WorkoutDao` deletes all days/exercises for a routine then re-inserts. Day objects with temp IDs (`""` or `"temp_*"`) get fresh UUIDs on save; real persisted IDs pass through. When updating an existing routine, `isSelected` and `lastCompletedDayIndex` are preserved from the existing DB row — never overwritten.

## Active Workout Mini-Bar

Lives in `MainActivity` inner `Scaffold` `bottomBar` when `isActive && !isFullScreen`. Implementation diverges from `docs/design-guidelines.md` in two ways — do not "fix" without intent:
- Surface color: `primaryContainer` (spec says `surfaceVariant`)
- Text styles: elapsed time uses `labelSmall`, workout name uses `titleSmall` + `FontWeight.Bold` (spec says `bodySmall` and `titleMedium` weight 700)
- Border: `outlineVariant` (nearly transparent 5% white) — intentional subtle separation

## R8 Full Mode

`android.enableR8.fullMode=true` in `gradle.properties`. `@Serializable` classes survive via the Kotlin serialization plugin (`kotlin-serialization` applied at app level). `proguard-rules.pro` is essentially empty — do not add manual keeps for serialization.

## Theme

`WorkoutPlannerTheme(themeMode: String = "dark")` — accepts `"dark"`, `"light"`, or `"system"`. The preference is stored as a `stringPreferencesKey("theme_mode")` in DataStore, surfaced via `RestTimerPreferencesRepository.themeMode: Flow<String>` (defaults to `"dark"`).

`TimerSettingsViewModel.themeMode` uses `SharingStarted.Eagerly` (not `WhileSubscribed`) to avoid first-frame flicker. Every other StateFlow in the app uses `WhileSubscribed(5000)`.

**Color role assignments — strictly enforced:**
- `primary`/`primaryContainer` — emerald green (`#27AE60` dark / `#16A34A` light). Active/selected states, CTA buttons.
- `secondary`/`secondaryContainer` — violet (`#A78BFA` dark / `#7C3AED` light). **Rest timer and UP NEXT badge only.** Do not use for text labels or generic UI.
- `tertiary`/`tertiaryContainer` — steel blue (`#60A5FA` dark / `#2563EB` light). **History chart bars and PR/stat values only.**
- `surfaceVariant` — card containers (`#111A12` dark). Use for all non-gradient card backgrounds.
- `outlineVariant` — `0x0DFFFFFF` (5 % white in dark) — nearly transparent. Used for subtle borders, mini-bar border. Do not use as a progress track.
- `outline` — `0x14FFFFFF` (8 % white in dark) — slightly more visible.
- `error` — "End workout" button **only**.

**Gradient constants** (in `Color.kt`, use these — no inline hex):
- `GradientHeroStart/Mid/End` — hero section backgrounds
- `GradientCardStart/End` — active exercise card header strip
- `GradientCtaStart/End` — CTA buttons (`#1E8449 → #27AE60`)
- Light variants: `GradientHeroStartLight/…`, `GradientCtaStartLight/…`

**Gradient CTA button pattern** — used in HomeScreen, ExerciseCard, WorkoutSummaryScreen:
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

`Color.White` is permitted inside gradient hero surfaces (white-on-green is intentional per design spec).

`motionScheme = MotionScheme.expressive()` is **not wired** — API is `internal` in M3 1.4.0. Wire when promoted to stable.

M3 Expressive components require `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`. BOM already includes them.

## UI Conventions (from `docs/design-guidelines.md`)

- All buttons: pill-shaped (`CircleShape`).
- All cards: `surfaceVariant` container color. Never custom background.
- Colors: `MaterialTheme.colorScheme.*` only. Raw hex only in gradients and translucent overlays. Use named gradient constants from `Color.kt`.
- Icons: `Icons.Rounded.*` from `material-icons-extended`. `Icons.Default.*` and `Icons.Outlined.*` are banned. Exception: `Icons.AutoMirrored.*` keeps its family (e.g., `Icons.AutoMirrored.Outlined.ListAlt`).
- `LargeTopAppBar` always pairs with `exitUntilCollapsedScrollBehavior` + `nestedScroll`. Screens using it: History, Settings, TimerSettings, Routines, RoutineDetail, Exercises, Equipment, Profile. **`CreateRoutineScreen` uses plain `TopAppBar` + `enterAlwaysScrollBehavior` instead.**
- No custom font (M3 defaults / Roboto only).
- Home screen has **no TopAppBar** — gradient hero replaces it.
- Acid green (`#C8FF00`) is launcher icon only; never in-app.
- Settings screen theme selector uses `SingleChoiceSegmentedButtonRow` + `SegmentedButton` (not `FilterChip`s).
- Progress bar track in `WorkoutScreen`: uses `surfaceVariant` (not `outlineVariant` — `outlineVariant` is near-transparent at 5% and would be invisible as a track).

## Workflow Skills

Use the `test-driven-development` skill when implementing features or bug fixes, unless already operating inside a TDD workflow.

## Test Patterns

6 unit test files (all JVM, no instrumented tests — `androidTest/` directory does not exist):

- `ActiveWorkoutViewModelTest` — full ViewModel coverage with `UnconfinedTestDispatcher`
- `RoutinesViewModelTest` — ViewModel with mocked repository
- `RestTimerPreferencesRepositoryTest` — real DataStore + JUnit `TemporaryFolder`
- `UserProfileRepositoryTest` — real DataStore + JUnit `TemporaryFolder`
- `ExerciseFilterTest` — pure function test, no dispatcher setup (root package, not `ui/` or `data/`)
- `FormatElapsedTimeTest` — pure function test, no dispatcher setup

Patterns:
- ViewModel tests: `UnconfinedTestDispatcher`, `Dispatchers.setMain/resetMain` in `@Before`/`@After`.
- Repository mocked with `mockk(relaxed = true)`.
- Flow assertions via Turbine (`app.cash.turbine`).
- Test names use backtick strings: `` `cancelWorkout resets state to defaults`() ``.

**No custom Hilt test runner is configured.** `testInstrumentationRunner` is the plain `AndroidJUnitRunner`. Any future instrumented Hilt tests will need `HiltTestRunner` added to `build.gradle.kts`.

## @IoDispatcher and DataStore Qualifiers

`DatabaseModule.kt` defines:
- `@IoDispatcher` — injects `Dispatchers.IO`. Any new repository using it must be provided in the module.
- `@RestTimerDataStore` — DataStore instance for `rest_timer_prefs` (used by `RestTimerPreferencesRepository`).
- `@UserProfileDataStore` — DataStore instance for `user_profile_prefs` (used by `UserProfileRepository`).

Every DataStore instance **must** have its own `@Qualifier` to avoid Hilt injection ambiguity. When adding a third DataStore, add a new qualifier and provide it in `DatabaseModule`.

## Localization

All user-visible strings live in Android resource files. **Never hardcode display text in Kotlin/Compose source.**

### Resource files

- `app/src/main/res/values/strings.xml` — English (default)
- `app/src/main/res/values-de/strings.xml` — German

### Key naming conventions

Keys follow a `<screen>_<element>` pattern:

| Prefix | Scope |
|---|---|
| `app_*` | App-level (app name, generic labels) |
| `action_*` | Generic actions shared across screens (`action_cancel`, `action_save`, `action_back_cd`) |
| `label_*` | Short standalone labels (`label_amrap`, `label_kg`) |
| `nav_*` | Bottom nav / navigation bar labels |
| `home_*` | HomeScreen |
| `workout_*` | WorkoutScreen, ExerciseCard, WorkoutSummaryScreen |
| `history_*` | HistoryScreen |
| `settings_*` | SettingsScreen, TimerSettingsScreen |
| `profile_*` | ProfileScreen |
| `exercises_*` | ExercisesScreen |
| `equipment_*` | EquipmentScreen |
| `routines_*` | RoutinesScreen, RoutineDetailScreen |
| `routine_create_*` | CreateRoutineScreen, RoutineDayCard, RoutineExerciseEditItem |
| `rest_timer_*` | RestTimerBanner |
| `exercise_dialog_*` | ExerciseSelectionDialog |

### Usage in composables

```kotlin
import androidx.compose.ui.res.stringResource
import de.melobeat.workoutplanner.R

Text(text = stringResource(R.string.some_key))
Text(text = stringResource(R.string.some_key_with_arg, argValue))
```

### Special cases — do not change without reading this

**HistoryScreen — `getDateGroupLabel()`**: This function returns English strings (`"Today"`, `"Yesterday"`, `"This Week"`, etc.) that are used as **sort keys** for grouping. Do **not** localize inside `getDateGroupLabel()`. Translate only at the display site using a `when(groupLabel)` mapping to `stringResource(...)`. The English string acts as a stable internal identifier.

**RestTimerBanner**: The `when` expression inside the composable checks `restTimerContext` type and selects a label. Pre-resolve the three localized strings at the top of the composable into local `val` variables, then use those variables inside the `when`:
```kotlin
val betweenSetsLabel = stringResource(R.string.rest_timer_between_sets)
val betweenExercisesLabel = stringResource(R.string.rest_timer_between_exercises)
// ... then use in when expression
```
This is required because `stringResource` cannot be called inside a non-composable lambda or `when` branch that is not itself inline composable.

**`WorkoutStepperCard`**: The `label` parameter is passed in from call sites — no changes needed inside the component itself. Localize at the call site.

**`ExerciseSelectionDialog`**: Only the `placeholder` text (search field hint) needed localization. The `title` parameter is provided by call sites.

**`repsSummary` in `RoutineDetailScreen`'s `WorkoutDayItem`**: This is a data-derived string built from set counts and rep ranges. It is deliberately constructed as Kotlin code (not a string resource) because its structure is dynamic. Leave as-is.

