# Seed Data Loading Fix + FAB Color Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the recursive `getDatabase()` call in `onCreate` that prevents seed data from loading, make ExtendedFloatingActionButtons visible with the correct solid green color, and align `docs/design-guidelines.md` with the `Icons.Rounded.*` convention already used in code.

**Architecture:** Three independent surgical edits — one to `WorkoutDatabase.kt` (replace recursive self-call with `Instance!!`), one to three screen files (add explicit FAB colors), and one to the design-guidelines doc (replace five `Icons.Default.*` references with `Icons.Rounded.*`). No new files. No DB version bump required.

**Tech Stack:** Kotlin, Room, Jetpack Compose M3, `ExtendedFloatingActionButton`

---

## Files Modified

| File | Change |
|---|---|
| `app/src/main/java/de/melobeat/workoutplanner/data/WorkoutDatabase.kt` | Fix recursive `getDatabase()` call; remove duplicate `8` from migration list |
| `app/src/main/java/de/melobeat/workoutplanner/ui/RoutinesScreen.kt` | Add `containerColor`/`contentColor` to FAB |
| `app/src/main/java/de/melobeat/workoutplanner/ui/ExercisesScreen.kt` | Add `containerColor`/`contentColor` to FAB |
| `app/src/main/java/de/melobeat/workoutplanner/ui/EquipmentScreen.kt` | Add `containerColor`/`contentColor` to FAB |
| `docs/design-guidelines.md` | Replace 5 `Icons.Default.*` with `Icons.Rounded.*` |

---

### Task 1: Fix the seed data recursive call in WorkoutDatabase.kt

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/data/WorkoutDatabase.kt:38,45`

**Background:** Inside the `onCreate` callback, line 45 calls `getDatabase(databaseContext)` again before `Instance` is assigned (`.also { Instance = it }` only runs after `.build()` returns). This creates a second orphaned `WorkoutDatabase` instance. The seed data is written to that second instance, not the one the app uses. The fix is to reference `Instance!!` directly — by the time the coroutine block runs on `Dispatchers.IO`, `.build()` has returned and `Instance` is guaranteed to be set.

Also, `8` appears in `fallbackToDestructiveMigrationFrom(1,2,3,4,5,6,7,8)` — the current DB version is `8`, so including it is meaningless. Remove it to avoid confusion.

- [ ] **Step 1: Edit `WorkoutDatabase.kt` line 38 — remove `8` from migration list**

  Change line 38 from:
  ```kotlin
  .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7, 8)
  ```
  To:
  ```kotlin
  .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7)
  ```

- [ ] **Step 2: Edit `WorkoutDatabase.kt` line 45 — fix the recursive call**

  Change line 45 from:
  ```kotlin
  val dao = getDatabase(databaseContext).workoutDao()
  ```
  To:
  ```kotlin
  val dao = Instance!!.workoutDao()
  ```

  The variable `databaseContext` on line 42 is no longer needed inside the coroutine (it's still used to open asset files on lines 48 and 52), so leave it. Only the DAO acquisition changes.

- [ ] **Step 3: Verify the file looks correct after the edit**

  The `onCreate` callback block (lines 39–79) should now read:
  ```kotlin
  .addCallback(object : Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
          super.onCreate(db)
          val databaseContext = context.applicationContext
          CoroutineScope(Dispatchers.IO).launch {
              try {
                  val dao = Instance!!.workoutDao()
                  val json = Json { ignoreUnknownKeys = true }

                  val equipmentJson = databaseContext.assets.open("equipment.json")
                      .bufferedReader().use { it.readText() }
                  val equipmentList = json.decodeFromString<List<InitialEquipment>>(equipmentJson)

                  val exercisesJson = databaseContext.assets.open("exercises.json")
                      .bufferedReader().use { it.readText() }
                  val exercisesList = json.decodeFromString<List<InitialExercise>>(exercisesJson)

                  equipmentList.forEach { equip ->
                      dao.insertEquipment(
                          EquipmentEntity(
                              id = equip.id,
                              name = equip.name,
                              defaultWeight = equip.defaultWeight
                          )
                      )
                  }
                  exercisesList.forEach { ex ->
                      dao.insertExercise(
                          ExerciseEntity(
                              name = ex.name,
                              muscleGroup = ex.muscleGroup,
                              description = ex.description,
                              equipmentId = ex.equipmentId
                          )
                      )
                  }
              } catch (e: Exception) {
                  android.util.Log.e("WorkoutDatabase", "Failed to seed initial data", e)
              }
          }
      }
  })
  ```

- [ ] **Step 4: Build to confirm no compile errors**

  ```bash
  JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

  ```bash
  git add app/src/main/java/de/melobeat/workoutplanner/data/WorkoutDatabase.kt
  git commit -m "fix(data): fix recursive getDatabase call preventing seed data from loading"
  ```

---

### Task 2: Fix FAB colors in RoutinesScreen, ExercisesScreen, EquipmentScreen

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/RoutinesScreen.kt:73-79`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/ExercisesScreen.kt:78-84`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/EquipmentScreen.kt:70-75`

**Background:** M3 `ExtendedFloatingActionButton` defaults to `containerColor = colorScheme.primaryContainer`. In this app's dark theme, `primaryContainer` is `Color(0x2427AE60)` — only 14% alpha, making the FAB nearly invisible on the dark `#0A0E0B` background. The fix adds explicit `containerColor = MaterialTheme.colorScheme.primary` (solid `#27AE60` emerald) and `contentColor = MaterialTheme.colorScheme.onPrimary` (white).

No import changes are needed — `MaterialTheme` is already imported via `androidx.compose.material3.*` in all three files.

- [ ] **Step 1: Fix FAB in `RoutinesScreen.kt`**

  Change lines 73-79 from:
  ```kotlin
  floatingActionButton = {
      ExtendedFloatingActionButton(
          onClick = onCreateRoutineClick,
          icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
          text = { Text("New Routine") }
      )
  },
  ```
  To:
  ```kotlin
  floatingActionButton = {
      ExtendedFloatingActionButton(
          onClick = onCreateRoutineClick,
          icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
          text = { Text("New Routine") },
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
      )
  },
  ```

- [ ] **Step 2: Fix FAB in `ExercisesScreen.kt`**

  Change lines 78-84 from:
  ```kotlin
  floatingActionButton = {
      ExtendedFloatingActionButton(
          onClick = { showAddDialog = true },
          icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
          text = { Text("Add Exercise") }
      )
  },
  ```
  To:
  ```kotlin
  floatingActionButton = {
      ExtendedFloatingActionButton(
          onClick = { showAddDialog = true },
          icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
          text = { Text("Add Exercise") },
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
      )
  },
  ```

- [ ] **Step 3: Fix FAB in `EquipmentScreen.kt`**

  Change lines 70-75 from:
  ```kotlin
  floatingActionButton = {
      ExtendedFloatingActionButton(
          onClick = { showAddDialog = true },
          icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
          text = { Text("Add Equipment") }
      )
  },
  ```
  To:
  ```kotlin
  floatingActionButton = {
      ExtendedFloatingActionButton(
          onClick = { showAddDialog = true },
          icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
          text = { Text("Add Equipment") },
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
      )
  },
  ```

- [ ] **Step 4: Build to confirm no compile errors**

  ```bash
  JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

  ```bash
  git add app/src/main/java/de/melobeat/workoutplanner/ui/RoutinesScreen.kt \
          app/src/main/java/de/melobeat/workoutplanner/ui/ExercisesScreen.kt \
          app/src/main/java/de/melobeat/workoutplanner/ui/EquipmentScreen.kt
  git commit -m "fix(ui): make FABs visible with solid primary color on list screens"
  ```

---

### Task 3: Update docs/design-guidelines.md to use Icons.Rounded.*

**Files:**
- Modify: `docs/design-guidelines.md:365-367,561,564-565,572`

**Background:** The design guidelines contain five references to `Icons.Default.*` that contradict both the stated primary icon family (`Icons.Rounded.*`) and the actual code. Correcting these keeps the doc as the authoritative reference.

- [ ] **Step 1: Fix icons section (lines 365–367)**

  Change:
  ```markdown
  - Routine selection uses `Icons.Default.CheckCircle` / `Icons.Default.RadioButtonUnchecked`
  - Edit actions use `Icons.Default.Edit`
  - Add FAB uses `Icons.Default.Add`
  ```
  To:
  ```markdown
  - Routine selection uses `Icons.Rounded.CheckCircle` / `Icons.Rounded.RadioButtonUnchecked`
  - Edit actions use `Icons.Rounded.Edit`
  - Add FAB uses `Icons.Rounded.Add`
  ```

- [ ] **Step 2: Fix Routines Screen section (line 561)**

  Change:
  ```markdown
  `TopAppBar` + `enterAlwaysScrollBehavior` + `nestedScroll`. `ExtendedFloatingActionButton` "New Routine" (+ `Icons.Default.Add`).
  ```
  To:
  ```markdown
  `TopAppBar` + `enterAlwaysScrollBehavior` + `nestedScroll`. `ExtendedFloatingActionButton` "New Routine" (+ `Icons.Rounded.Add`).
  ```

- [ ] **Step 3: Fix Routines Screen section (lines 564–565)**

  Change:
  ```markdown
  - Active routine: name `color = colorScheme.primary`, select icon `Icons.Default.CheckCircle` `tint = colorScheme.primary`.
  - Inactive routine: name `colorScheme.onSurface`, select icon `Icons.Default.RadioButtonUnchecked` `tint = colorScheme.onSurfaceVariant`.
  ```
  To:
  ```markdown
  - Active routine: name `color = colorScheme.primary`, select icon `Icons.Rounded.CheckCircle` `tint = colorScheme.primary`.
  - Inactive routine: name `colorScheme.onSurface`, select icon `Icons.Rounded.RadioButtonUnchecked` `tint = colorScheme.onSurfaceVariant`.
  ```

- [ ] **Step 4: Fix Routine Detail Screen section (line 572)**

  Change:
  ```markdown
  `TopAppBar` + `enterAlwaysScrollBehavior` + `nestedScroll`. Trailing "Edit" action: `Icons.Default.Edit`. Loading state: centered `CircularProgressIndicator`.
  ```
  To:
  ```markdown
  `TopAppBar` + `enterAlwaysScrollBehavior` + `nestedScroll`. Trailing "Edit" action: `Icons.Rounded.Edit`. Loading state: centered `CircularProgressIndicator`.
  ```

- [ ] **Step 5: Commit**

  ```bash
  git add docs/design-guidelines.md
  git commit -m "docs: align design-guidelines icon references to Icons.Rounded.*"
  ```

---

### Task 4: Final build verification

- [ ] **Step 1: Run unit tests**

  ```bash
  JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:testDebugUnitTest
  ```
  Expected: `BUILD SUCCESSFUL` with all tests passing. (No new tests are needed — these are mechanical edits with no logic changes. The existing `ActiveWorkoutViewModelTest`, `RoutinesViewModelTest`, and other tests already cover the affected code paths.)

- [ ] **Step 2: Clean build**

  ```bash
  JAVA_HOME=/opt/android-studio/jbr ./gradlew clean assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL`
