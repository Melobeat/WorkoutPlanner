# Equipment Default Weight & User Profile — Design Spec

**Date:** 2026-04-10  
**Status:** Approved

---

## Overview

Two related features:

1. **Equipment default weight** — each equipment item optionally stores a bar/implement weight in kg. Used by a future plate calculator to know how much weight is already on the bar before plates are added.
2. **User profile** — stores age, height, and body weight. Body weight is used by bodyweight-base exercises to calculate total load (body weight + extra weight).

A supporting flag, **`isBodyweight`**, is added to exercises to mark exercises where the athlete's own body weight forms the base load (e.g. pull-ups, push-ups, dips). The workout weight field for these exercises represents *extra* weight only.

---

## Data Layer

### 1. `EquipmentEntity` — new field

```kotlin
val defaultWeight: Double?  // kg; null = no bar weight (dumbbell, cable, etc.)
```

- `null` means the equipment has no fixed implement weight. The user enters the full working weight per set.
- Non-null means the equipment has a known bar/implement weight (e.g. barbell = 20 kg). Stored so a future plate calculator can subtract bar weight from total load.
- User-editable: the Equipment screen allows setting or clearing this value.

**`Equipment` domain model** (`model/Equipment.kt`) gains `defaultWeight: Double?`.

**`EquipmentMapper`** passes the new field through in both directions.

---

### 2. Updated seed data (`assets/equipment.json`)

Remove `equip_bench`. Add `equip_ez_bar`.

| ID | Name | defaultWeight |
|---|---|---|
| `equip_barbell` | Barbell | 20.0 |
| `equip_ez_bar` | EZ Bar | 10.0 |
| `equip_dumbbell` | Dumbbell | null |
| `equip_pullup_bar` | Pull-up Bar | null |
| `equip_kettlebell` | Kettlebell | null |
| `equip_cable_machine` | Cable Machine | null |
| `equip_none` | None / Bodyweight | null |

The `InitialEquipment` data class in `InitialData.kt` gains `val defaultWeight: Double? = null` to parse the new field.

Exercises previously linked to `equip_bench` will have their `equipmentId` set to null via the existing `SET_NULL` foreign key policy on destructive migration — acceptable, since bench is a surface, not a weighted implement.

---

### 3. `ExerciseEntity` — new field

```kotlin
val isBodyweight: Boolean = false
```

Marks exercises where the athlete's own body weight forms the base load (pull-ups, push-ups, dips, bodyweight squats, etc.).

- In the workout view, the weight field represents *extra* weight only (e.g. a dip belt plate). Default extra weight = 0.
- A future plate calculator uses `isBodyweight = true` + profile `bodyWeightKg` to compute total load.
- `Exercise` domain model gains `isBodyweight: Boolean = false`.
- `ExerciseMapper` passes the field through.

---

### 4. User Profile — new `UserProfileRepository`

Backed by a new DataStore file `"user_profile_prefs"` (separate from `"rest_timer_prefs"`).

**DataStore keys:**
```kotlin
val AGE_KEY           = intPreferencesKey("age")               // years; absent = not set
val HEIGHT_KEY        = intPreferencesKey("height_cm")         // centimetres; absent = not set
val BODY_WEIGHT_KEY   = floatPreferencesKey("body_weight_kg")  // kg; absent = not set
```

**Domain model** (`model/UserProfile.kt`):
```kotlin
data class UserProfile(
    val age: Int? = null,
    val heightCm: Int? = null,
    val bodyWeightKg: Float? = null
)
```

All fields optional. A missing value is represented by the key being absent from DataStore (not by a sentinel like 0 or -1).

**`UserProfileRepository`** (`data/UserProfileRepository.kt`):
- `val userProfile: Flow<UserProfile>` — emits the current profile, defaulting to `UserProfile()` (all nulls).
- `suspend fun updateAge(age: Int?)`
- `suspend fun updateHeight(heightCm: Int?)`
- `suspend fun updateBodyWeight(bodyWeightKg: Float?)`

Provided in `DatabaseModule` via `@Provides @Singleton`, injected with `@IoDispatcher`.

---

### 5. Database version bump

- Version: **7 → 8**
- `fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7)` — unchanged (covers all prior versions up to the previous DB version 7).
- No migration objects — destructive only, consistent with existing policy.

---

## Navigation

New route: **`ProfileRoute`** (a `@Serializable object`) added to `NavRoutes.kt`.

`ProfileRoute` lives **inside** the `SettingsGraphRoute` nested graph, alongside `SettingsRoute`, `TimerSettingsRoute`, etc.

---

## UI

### Equipment Screen (`EquipmentScreen`)

**List item:** When `defaultWeight != null`, show it as a secondary line (e.g. "20 kg") beneath the equipment name. When null, show nothing.

**Add / Edit `AlertDialog`** — gains one new field:

- Label: "Bar weight (kg)" — `OutlinedTextField`, numeric keyboard (`KeyboardType.Decimal`), optional.
- Placeholder: "e.g. 20"
- Pre-filled with current `defaultWeight` when editing; empty when adding.
- Blank input → `null` (no default weight). Non-blank → parsed as `Double`.
- Positioned below the equipment name field.

No other changes to the Equipment screen layout.

**`ExerciseLibraryViewModel`** updated to accept `defaultWeight: Double?` in add/edit equipment operations.

---

### Exercise Add/Edit Dialog (`ExercisesScreen`)

The existing dialog (name, muscle group, description, equipment dropdown) gains one new row:

- **"Uses body weight as base load"** — a `Switch` row with label and sublabel "Extra weight only is tracked in the workout (e.g. pull-ups, dips)".
- Positioned below the equipment dropdown.
- Default: off (`false`).
- Pre-filled with current `isBodyweight` when editing.

**`ExerciseLibraryViewModel`** updated to pass `isBodyweight` through add/edit paths.

---

### Profile Screen (`ProfileScreen`) — new screen

Follows the same layout pattern as `TimerSettingsScreen`:

- `Scaffold` with `LargeTopAppBar` + `exitUntilCollapsedScrollBehavior` + `nestedScroll`.
- Top app bar title: "Profile". Back navigation lambda.
- Content: `LazyColumn` with three input rows.

**Input rows** (each a `ListItem`-style row or `OutlinedTextField` in a `Column`):

| Field | Input type | Keyboard | Unit suffix |
|---|---|---|---|
| Age | Integer | `KeyboardType.Number` | "years" |
| Height | Integer | `KeyboardType.Number` | "cm" |
| Body weight | Decimal | `KeyboardType.Decimal` | "kg" |

- All fields optional — blank = not set (stored as null in DataStore).
- **Auto-save on focus-leave** (`onFocusChanged` → save when `!isFocused`). No explicit Save button.
- Values loaded from `UserProfileViewModel.userProfile: StateFlow<UserProfile>`.

**`UserProfileViewModel`** (`ui/UserProfileViewModel.kt`):
- `@HiltViewModel`; uses `hiltViewModel()` inside `NavHost` (not Activity-scoped).
- Exposes `userProfile: StateFlow<UserProfile>` via `WhileSubscribed(5000)`.
- Methods: `updateAge(String)`, `updateHeight(String)`, `updateBodyWeight(String)` — each parses the string to the appropriate nullable type before calling the repository.

---

### Settings Screen (`SettingsScreen`)

Adds a **"Profile"** list item below the existing "Timer" entry:

- Leading icon: `Icons.Rounded.Person`
- Title: "Profile"
- Subtitle: "Age, height and body weight"
- Trailing: chevron icon
- On click: `onNavigateToProfile()`

`SettingsScreen` composable gains an `onNavigateToProfile: () -> Unit` lambda parameter.

---

### Navigation wiring (`WorkoutNavGraph.kt`)

1. Add `ProfileRoute` composable destination inside `SettingsGraphRoute`.
2. Wire `SettingsScreen` → `ProfileRoute` navigation.
3. `ProfileScreen` back-navigation: `popBackStack()`.

---

## Future considerations (out of scope for this spec)

- **Plate calculator**: uses `EquipmentEntity.defaultWeight` (bar weight) + `RoutineSet.weight` (total load) to compute plate configuration. Uses `ExerciseEntity.isBodyweight` + `UserProfile.bodyWeightKg` for bodyweight-base exercises.
- **Total load display in workout**: `isBodyweight = true` → display "Body weight + X kg" in set row. Not implemented here.
- **Unit system (kg / lbs)**: not in scope. All weights stored in kg.

---

## Constraints

- `RoutineSet` (`weight: Double`) is unchanged — it already stores total working weight per set.
- `SetUiState.weight: String` and the existing weight stepper logic are unchanged.
- Equipment weight is purely a metadata field for future plate calculation — it does not pre-fill `RoutineSet.weight` in this feature iteration.
- `@Serializable` constraint on `RoutineSet` is unaffected.
- No Hilt test runner changes required; new ViewModels follow existing `@HiltViewModel` + `hiltViewModel()` pattern.
