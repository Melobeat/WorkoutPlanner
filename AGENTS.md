# AGENTS.md — WorkoutPlanner

Single-module Android app (`de.melobeat.workoutplanner`). Kotlin + Jetpack Compose + Room + Hilt + Navigation Compose.

## Documentation Index

| Doc | Purpose |
|---|---|
| [`docs/architecture.md`](docs/architecture.md) | Layer structure, DI graph, Room, navigation, ViewModel scoping, workout state, ActiveWorkoutViewModel behaviors |
| [`docs/testing.md`](docs/testing.md) | Test inventory, patterns, what to test, how to add tests |
| [`docs/release.md`](docs/release.md) | Build commands, R8, version bumping, Git Flow release/hotfix process |
| [`docs/design-guidelines.md`](docs/design-guidelines.md) | **Authoritative UI spec** — read before any UI change |

## Toolchain

| Item | Version |
|---|---|
| AGP | 9.1.1 |
| Kotlin | 2.3.20 |
| KSP | 2.3.2 |
| Navigation Compose | 2.9.7 |
| Gradle wrapper | 9.4.1 |
| JVM toolchain | 21 (via Foojay resolver; source/target compat: 11) |
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
- **commit-msg**: enforces Conventional Commits (`feat|fix|docs|style|refactor|test|chore|perf|ci|build|revert`)
- **pre-push**: runs `:app:testDebugUnitTest` before every push; bypass with `SKIP_TESTS=1 git push`

## Git Workflow (Git Flow)

- **`main`** — production-ready code only. Tagged releases.
- **`develop`** — integration branch for the next release.
- Feature branches: `feat/<slug>` off `develop`, merge back to `develop`
- Release branches: `release/<version>` off `develop` for stabilization, merge to `main` + `develop`
- Hotfix branches: `hotfix/<slug>` off `main` for urgent fixes, merge to `main` + `develop`
- Never commit directly to `main` or `develop`
- Full release/hotfix process: see [`docs/release.md`](docs/release.md)

Gradle has `org.gradle.configuration-cache=true` and `org.gradle.caching=true` active.

**JAVA_HOME**: OpenJDK on system `PATH` (Java 26). Gradle provisions Java 21 via Foojay resolver.

**Gradle heap**: `-Xmx4g` in `gradle.properties`. On machines with < 6 GB free RAM, builds may OOM.

## R8 Full Mode

`android.enableR8.fullMode=true` in `gradle.properties`. `proguard-rules.pro` is essentially empty. See [`docs/release.md`](docs/release.md) for details.

## Theme

`WorkoutPlannerTheme(themeMode: String = "dark")` — accepts `"dark"`, `"light"`, or `"system"`. See [`docs/architecture.md`](docs/architecture.md) for `SharingStarted.Eagerly` vs `WhileSubscribed` convention.

Full color tokens, gradients, typography, shapes, and component patterns: see [`docs/design-guidelines.md`](docs/design-guidelines.md).

## UI Conventions

- All buttons: pill-shaped (`CircleShape`).
- All cards: `surfaceVariant` container color. Never custom background.
- Icons: `Icons.Rounded.*` from `material-icons-extended`. `Icons.Default.*` and `Icons.Outlined.*` are banned. Exception: `Icons.AutoMirrored.*` keeps its family.
- `LargeTopAppBar` always pairs with `exitUntilCollapsedScrollBehavior` + `nestedScroll`. **`CreateRoutineScreen` uses plain `TopAppBar` + `enterAlwaysScrollBehavior` instead.**
- No custom font (M3 defaults / Roboto only).
- Home screen has **no TopAppBar** — gradient hero replaces it.
- Acid green (`#C8FF00`) is launcher icon only; never in-app.
- Full spec: [`docs/design-guidelines.md`](docs/design-guidelines.md)

## Workflow Skills

Use the `test-driven-development` skill when implementing features or bug fixes, unless already operating inside a TDD workflow.

## Localization

All user-visible strings live in Android resource files. **Never hardcode display text in Kotlin/Compose source.**

- `app/src/main/res/values/strings.xml` — English (default)
- `app/src/main/res/values-de/strings.xml` — German

Key naming: `<screen>_<element>` pattern (e.g., `home_*`, `workout_*`, `settings_*`).

### Special cases

**HistoryScreen — `getDateGroupLabel()`**: Returns English strings used as **sort keys**. Do **not** localize inside the function. Translate only at the display site via `when(groupLabel)` mapping to `stringResource(...)`.

**RestTimerBanner**: Pre-resolve localized strings into local `val` variables before the `when` expression — `stringResource` cannot be called inside a non-composable lambda.

**`WorkoutStepperCard`**: The `label` parameter is passed from call sites. Localize at the call site.

**`ExerciseSelectionDialog`**: Only the `placeholder` text needed localization. The `title` parameter is provided by call sites.

**`repsSummary` in `RoutineDetailScreen`'s `WorkoutDayItem`**: Data-derived string built from set counts and rep ranges. Deliberately Kotlin code (not a string resource) because its structure is dynamic. Leave as-is.
