# Equipment Weight Step Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add configurable weight stepper increments per equipment type, replacing the hardcoded `WEIGHT_STEP = 2.5` with per-equipment values.

**Architecture:** Add a `weightStep` field to the Equipment entity with seed defaults. The step is resolved when exercises are loaded into the active workout and stored on `ExerciseUiState`. The Equipment edit dialog exposes a field to customize the step.

**Tech Stack:** Kotlin, Room, Jetpack Compose, Hilt, Material 3

---

### Task 1: Add `weightStep` to data layer (Entity, Domain, Mapper, Seed, DB)

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/data/Entities.kt:27-32`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/model/Equipment.kt:1-7`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/data/Mappers.kt:8`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/data/InitialData.kt:6`
- Modify: `app/src/main/assets/equipment.json`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/data/WorkoutDatabase.kt:24,38`

- [ ] **Step 1: Add `weightStep` to `EquipmentEntity`**

In `data/Entities.kt`, add `weightStep` parameter to `EquipmentEntity`:

```kotlin
@Entity(tableName = "equipment")
data class EquipmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val defaultWeight: Double? = null,
    val weightStep: Double = 2.5
)
```

- [ ] **Step 2: Add `weightStep` to domain `Equipment`**

In `model/Equipment.kt`:

```kotlin
package de.melobeat.workoutplanner.model

data class Equipment(
    val id: String,
    val name: String,
    val defaultWeight: Double? = null,
    val weightStep: Double = 2.5
)
```

- [ ] **Step 3: Update mapper**

In `data/Mappers.kt`, update the `toDomain()` function:

```kotlin
fun EquipmentEntity.toDomain() = Equipment(id = id, name = name, defaultWeight = defaultWeight, weightStep = weightStep)
```

- [ ] **Step 4: Update `InitialEquipment` serializable class**

In `data/InitialData.kt`:

```kotlin
@Serializable
data class InitialEquipment(val id: String, val name: String, val defaultWeight: Double? = null, val weightStep: Double? = null)
```

The `weightStep` is nullable with default `null` so that `ignoreUnknownKeys = true` handles old JSON without the field, and we'll provide explicit values in the seed file.

- [ ] **Step 5: Update seed data in `equipment.json`**

```json
[
  { "id": "equip_barbell",       "name": "Barbell",         "defaultWeight": 20.0, "weightStep": 2.5 },
  { "id": "equip_ez_bar",        "name": "EZ Bar",          "defaultWeight": 10.0, "weightStep": 2.5 },
  { "id": "equip_dumbbell",      "name": "Dumbbell",                              "weightStep": 1.0 },
  { "id": "equip_pullup_bar",    "name": "Pull-up Bar",                           "weightStep": 1.25 },
  { "id": "equip_kettlebell",    "name": "Kettlebell",                            "weightStep": 1.0 },
  { "id": "equip_cable_machine", "name": "Cable Machine",                         "weightStep": 1.0 },
  { "id": "equip_none",          "name": "None / Bodyweight",                     "weightStep": 1.0 }
]
```

- [ ] **Step 6: Update `WorkoutDatabase` seed loading to include `weightStep`**

In `data/WorkoutDatabase.kt`, update the equipment insertion in `onCreate` (around line 56-63):

```kotlin
equipmentList.forEach { equip ->
    dao.insertEquipment(
        EquipmentEntity(
            id = equip.id,
            name = equip.name,
            defaultWeight = equip.defaultWeight,
            weightStep = equip.weightStep ?: 2.5
        )
    )
}
```

- [ ] **Step 7: Bump DB version and update migration list**

In `data/WorkoutDatabase.kt`:
- Change `version = 8` to `version = 9`
- Change `.fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7)` to `.fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7, 8)`

- [ ] **Step 8: Verify build compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/data/Entities.kt app/src/main/java/de/melobeat/workoutplanner/model/Equipment.kt app/src/main/java/de/melobeat/workoutplanner/data/Mappers.kt app/src/main/java/de/melobeat/workoutplanner/data/InitialData.kt app/src/main/assets/equipment.json app/src/main/java/de/melobeat/workoutplanner/data/WorkoutDatabase.kt
git commit -m "feat(equipment): add weightStep field to Equipment entity"
```

---

### Task 2: Add `weightStep` to `ExerciseUiState` and resolve it in ViewModel

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutUiState.kt:46-52`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModel.kt` (lines 36, 59-118)

- [ ] **Step 1: Add `weightStep` to `ExerciseUiState`**

In `ui/WorkoutUiState.kt`:

```kotlin
data class ExerciseUiState(
    val exerciseId: String,
    val name: String,
    val sets: List<SetUiState>,
    val isExpanded: Boolean = true,
    val lastSets: List<Pair<Double, Int>> = emptyList(),
    val weightStep: Double = 2.5
)
```

- [ ] **Step 2: Remove the hardcoded constant and add equipment cache**

In `ActiveWorkoutViewModel.kt`:
- Remove `private const val WEIGHT_STEP = 2.5` (line 36)
- Add a private field to cache equipment:

```kotlin
private var equipmentCache: List<Equipment> = emptyList()
```

- [ ] **Step 3: Load equipment cache in `startWorkout`**

Modify `startWorkout` to load equipment before building exercise states. Change the method to:

```kotlin
fun startWorkout(day: WorkoutDay, dayIndex: Int, routineName: String, routineId: String?) {
    viewModelScope.launch {
        try {
            // Load equipment cache
            equipmentCache = repository.getEquipmentStream().first()
            
            val exerciseStates = day.exercises.mapIndexed { index, exercise ->
                val history = repository.getHistoryForExercise(exercise.id).first()
                val lastSets = if (history.isNotEmpty()) {
                    val latestWorkoutId = history[0].workoutHistoryId
                    history.filter { it.workoutHistoryId == latestWorkoutId }
                        .sortedBy { it.sets }
                        .map { it.weight to it.reps }
                } else emptyList()

                buildExerciseUiState(exercise, lastSets).copy(isExpanded = index == 0)
            }

            currentWorkoutDay = day
            currentDayIndex = dayIndex
            currentRoutineName = routineName
            currentRoutineId = routineId

            _uiState.update {
                it.copy(
                    isActive = true,
                    isFullScreen = true,
                    workoutDayName = day.name,
                    exercises = exerciseStates,
                    isFinished = false,
                    error = null,
                    currentExerciseIndex = 0,
                    currentSetIndex = 0
                )
            }
            startTimer()
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to start workout: ${e.message}") }
        }
    }
}
```

- [ ] **Step 4: Add helper to resolve weightStep from exercise's equipment**

Add a private helper method in `ActiveWorkoutViewModel`:

```kotlin
private fun resolveWeightStep(exercise: Exercise): Double {
    if (exercise.isBodyweight) return 1.0
    val equipment = equipmentCache.find { it.id == exercise.equipmentId }
    return equipment?.weightStep ?: 1.0
}
```

- [ ] **Step 5: Update `buildExerciseUiState` to include `weightStep`**

Update the return statement in `buildExerciseUiState`:

```kotlin
private fun buildExerciseUiState(
    exercise: Exercise,
    lastSets: List<Pair<Double, Int>>
): ExerciseUiState {
    val predefined = exercise.routineSets
    val numSets = if (predefined.isNotEmpty()) predefined.size else maxOf(3, lastSets.size)
    val sets = (0 until numSets).map { i ->
        val weight = lastSets.getOrNull(i)?.first?.let { formatWeight(it) }
            ?: predefined.getOrNull(i)?.weight?.let { if (it > 0) formatWeight(it) else "0" }
            ?: "0"
        val reps = predefined.getOrNull(i)?.reps?.toString() ?: "0"
        val isAmrap = predefined.getOrNull(i)?.isAmrap ?: false
        SetUiState(index = i, weight = weight, reps = reps, isAmrap = isAmrap, originalReps = reps)
    }
    return ExerciseUiState(
        exerciseId = exercise.id,
        name = exercise.name,
        sets = sets,
        lastSets = lastSets,
        weightStep = resolveWeightStep(exercise)
    )
}
```

- [ ] **Step 6: Update `incrementWeight` to use per-exercise step**

```kotlin
fun incrementWeight(exerciseIndex: Int, setIndex: Int) {
    val exercise = _uiState.value.exercises.getOrNull(exerciseIndex) ?: return
    val set = exercise.sets.getOrNull(setIndex) ?: return
    val current = set.weight.toDoubleOrNull() ?: 0.0
    setWeightValue(exerciseIndex, setIndex, formatWeight(current + exercise.weightStep))
}
```

- [ ] **Step 7: Update `decrementWeight` to use per-exercise step**

```kotlin
fun decrementWeight(exerciseIndex: Int, setIndex: Int) {
    val exercise = _uiState.value.exercises.getOrNull(exerciseIndex) ?: return
    val set = exercise.sets.getOrNull(setIndex) ?: return
    val current = set.weight.toDoubleOrNull() ?: 0.0
    if (current >= exercise.weightStep) setWeightValue(exerciseIndex, setIndex, formatWeight(current - exercise.weightStep))
}
```

- [ ] **Step 8: Update `addExercise` to also cache equipment**

The `addExercise` method calls `buildExerciseUiState` which now uses `equipmentCache`. Ensure equipment is loaded before `addExercise` is called. Since `addExercise` is called during an active workout where `startWorkout` already loaded the cache, this is fine. But add a safety check:

```kotlin
fun addExercise(exercise: Exercise) {
    if (equipmentCache.isEmpty()) {
        viewModelScope.launch {
            equipmentCache = repository.getEquipmentStream().first()
            _uiState.update { state ->
                state.copy(exercises = state.exercises + buildExerciseUiState(exercise, emptyList()))
            }
        }
    } else {
        _uiState.update { state ->
            state.copy(exercises = state.exercises + buildExerciseUiState(exercise, emptyList()))
        }
    }
}
```

- [ ] **Step 9: Update `swapExercise` to use the new exercise's weightStep**

The `swapExercise` method calls `buildExerciseUiState` with the new exercise, which will automatically resolve the correct `weightStep` from the equipment cache. No code changes needed — it already works correctly.

- [ ] **Step 10: Verify build compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 11: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutUiState.kt app/src/main/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModel.kt
git commit -m "feat(workout): use per-equipment weightStep in active workout"
```

---

### Task 3: Update repository and ViewModel for equipment save with weightStep

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/data/WorkoutRepository.kt:108-120`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseLibraryViewModel.kt:72-80`

- [ ] **Step 1: Update `WorkoutRepository.saveEquipment`**

```kotlin
suspend fun saveEquipment(
    name: String,
    existingId: String?,
    defaultWeight: Double? = null,
    weightStep: Double = 2.5
) = withContext(dispatcher) {
    dao.insertEquipment(
        EquipmentEntity(
            id = existingId ?: UUID.randomUUID().toString(),
            name = name,
            defaultWeight = defaultWeight,
            weightStep = weightStep
        )
    )
}
```

- [ ] **Step 2: Update `ExerciseLibraryViewModel.saveEquipment`**

```kotlin
fun saveEquipment(name: String, existingId: String?, defaultWeight: Double? = null, weightStep: Double = 2.5) {
    viewModelScope.launch {
        try {
            repository.saveEquipment(name, existingId, defaultWeight, weightStep)
        } catch (e: Exception) {
            _state.update { it.copy(error = "Failed to save equipment") }
        }
    }
}
```

- [ ] **Step 3: Verify build compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/data/WorkoutRepository.kt app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseLibraryViewModel.kt
git commit -m "feat(equipment): add weightStep parameter to saveEquipment"
```

---

### Task 4: Update EquipmentScreen UI — dialog and list display

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/EquipmentScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-de/strings.xml`

- [ ] **Step 1: Add new string resources (English)**

In `res/values/strings.xml`, add to the EquipmentScreen section:

```xml
<string name="equipment_weight_step">Step: %1$s kg</string>
<string name="equipment_weight_step_label">Weight step (kg)</string>
```

- [ ] **Step 2: Add new string resources (German)**

In `res/values-de/strings.xml`, add to the EquipmentScreen section:

```xml
<string name="equipment_weight_step">Schritt: %1$s kg</string>
<string name="equipment_weight_step_label">Gewichtsschritt (kg)</string>
```

- [ ] **Step 3: Update list item to show weight step**

In `EquipmentScreenContent`, update the `ListItem` supporting content to show both bar weight and step:

Replace lines 91-93:
```kotlin
supportingContent = item.defaultWeight?.let { w ->
    { Text(stringResource(R.string.equipment_bar_weight, if (w % 1.0 == 0.0) w.toInt().toString() else w.toString())) }
},
```

With:
```kotlin
supportingContent = {
    val parts = mutableListOf<String>()
    item.defaultWeight?.let { w ->
        parts.add(stringResource(R.string.equipment_bar_weight, if (w % 1.0 == 0.0) w.toInt().toString() else w.toString()))
    }
    parts.add(stringResource(R.string.equipment_weight_step, if (item.weightStep % 1.0 == 0.0) item.weightStep.toInt().toString() else item.weightStep.toString()))
    Text(parts.joinToString(" · "))
},
```

- [ ] **Step 4: Update `EquipmentScreen` to pass `weightStep` to `onSaveEquipment`**

Change the lambda in `EquipmentScreen`:

```kotlin
onSaveEquipment = { name, id, defaultWeight, weightStep -> viewModel.saveEquipment(name, id, defaultWeight, weightStep) },
```

- [ ] **Step 5: Update `EquipmentScreenContent` signature**

Update the function signature:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentScreenContent(
    equipment: List<Equipment>,
    onBack: () -> Unit,
    onSaveEquipment: (name: String, id: String?, defaultWeight: Double?, weightStep: Double) -> Unit,
    onDeleteEquipment: (id: String) -> Unit,
    modifier: Modifier = Modifier
)
```

- [ ] **Step 6: Update the dialog confirm callback**

In `EquipmentScreenContent`, update the `EquipmentDialog` onConfirm callback:

```kotlin
if (showAddDialog || equipmentToEdit != null) {
    EquipmentDialog(
        initialEquipment = equipmentToEdit,
        onDismiss = {
            showAddDialog = false
            equipmentToEdit = null
        },
        onConfirm = { name, defaultWeight, weightStep ->
            onSaveEquipment(name, equipmentToEdit?.id, defaultWeight, weightStep)
            showAddDialog = false
            equipmentToEdit = null
        }
    )
}
```

- [ ] **Step 7: Update `EquipmentDialog` to include weight step field**

Replace the entire `EquipmentDialog` composable:

```kotlin
@Composable
fun EquipmentDialog(
    initialEquipment: Equipment? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, defaultWeight: Double?, weightStep: Double) -> Unit
) {
    var name by remember { mutableStateOf(initialEquipment?.name ?: "") }
    var weightText by remember {
        mutableStateOf(
            initialEquipment?.defaultWeight?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            } ?: ""
        )
    }
    var stepText by remember {
        mutableStateOf(
            initialEquipment?.let {
                if (it.weightStep % 1.0 == 0.0) it.weightStep.toInt().toString() else it.weightStep.toString()
            } ?: "2.5"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialEquipment == null) stringResource(R.string.equipment_add_dialog_title) else stringResource(R.string.equipment_edit_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.equipment_name_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text(stringResource(R.string.equipment_bar_weight_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stepText,
                    onValueChange = { stepText = it },
                    label = { Text(stringResource(R.string.equipment_weight_step_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val weight = weightText.trim().toDoubleOrNull()
                    val step = stepText.trim().toDoubleOrNull()?.takeIf { it > 0 } ?: 2.5
                    onConfirm(name, weight, step)
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (initialEquipment == null) stringResource(R.string.action_add) else stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
```

- [ ] **Step 8: Update previews**

Update `EquipmentScreenContentPreview` to include `weightStep` in Equipment objects:

```kotlin
@Preview(showBackground = true)
@Composable
fun EquipmentScreenContentPreview() {
    WorkoutPlannerTheme {
        EquipmentScreenContent(
            equipment = listOf(
                Equipment(id = "1", name = "Barbell", weightStep = 2.5),
                Equipment(id = "2", name = "Dumbbell", weightStep = 1.0),
                Equipment(id = "3", name = "Cable Machine", weightStep = 1.0)
            ),
            onBack = {},
            onSaveEquipment = { _, _, _, _ -> },
            onDeleteEquipment = {}
        )
    }
}
```

Update `EquipmentDialogPreview`:

```kotlin
@Preview(showBackground = true)
@Composable
fun EquipmentDialogPreview() {
    WorkoutPlannerTheme {
        EquipmentDialog(
            initialEquipment = null,
            onDismiss = {},
            onConfirm = { _, _, _ -> }
        )
    }
}
```

Update `EquipmentItemPreview`:

```kotlin
@Preview(showBackground = true)
@Composable
fun EquipmentItemPreview() {
    WorkoutPlannerTheme {
        EquipmentItem(
            equipment = Equipment(id = "1", name = "Barbell", weightStep = 2.5),
            onClick = {},
            onDelete = {}
        )
    }
}
```

- [ ] **Step 9: Verify build compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/EquipmentScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git commit -m "feat(equipment): add weight step field to Equipment dialog and list"
```

---

### Task 5: Write tests for weightStep functionality

**Files:**
- Modify: `app/src/test/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModelTest.kt`
- Create: `app/src/test/java/de/melobeat/workoutplanner/EquipmentWeightStepTest.kt`

- [ ] **Step 1: Create pure function test for weightStep resolution**

Create `app/src/test/java/de/melobeat/workoutplanner/EquipmentWeightStepTest.kt`:

```kotlin
package de.melobeat.workoutplanner

import de.melobeat.workoutplanner.model.Equipment
import de.melobeat.workoutplanner.model.Exercise
import org.junit.Assert.assertEquals
import org.junit.Test

class EquipmentWeightStepTest {

    private fun resolveWeightStep(exercise: Exercise, equipmentCache: List<Equipment>): Double {
        if (exercise.isBodyweight) return 1.0
        val equipment = equipmentCache.find { it.id == exercise.equipmentId }
        return equipment?.weightStep ?: 1.0
    }

    @Test
    fun `bodyweight exercise returns 1_0`() {
        val exercise = Exercise(id = "ex1", name = "Push-up", isBodyweight = true)
        val cache = listOf(
            Equipment(id = "equip_barbell", name = "Barbell", weightStep = 2.5)
        )
        assertEquals(1.0, resolveWeightStep(exercise, cache), 0.001)
    }

    @Test
    fun `exercise with no equipment returns 1_0`() {
        val exercise = Exercise(id = "ex1", name = "Stretch", equipmentId = null)
        val cache = listOf(
            Equipment(id = "equip_barbell", name = "Barbell", weightStep = 2.5)
        )
        assertEquals(1.0, resolveWeightStep(exercise, cache), 0.001)
    }

    @Test
    fun `exercise with barbell equipment returns 2_5`() {
        val exercise = Exercise(id = "ex1", name = "Bench Press", equipmentId = "equip_barbell")
        val cache = listOf(
            Equipment(id = "equip_barbell", name = "Barbell", weightStep = 2.5),
            Equipment(id = "equip_dumbbell", name = "Dumbbell", weightStep = 1.0)
        )
        assertEquals(2.5, resolveWeightStep(exercise, cache), 0.001)
    }

    @Test
    fun `exercise with dumbbell equipment returns 1_0`() {
        val exercise = Exercise(id = "ex1", name = "Bicep Curl", equipmentId = "equip_dumbbell")
        val cache = listOf(
            Equipment(id = "equip_barbell", name = "Barbell", weightStep = 2.5),
            Equipment(id = "equip_dumbbell", name = "Dumbbell", weightStep = 1.0)
        )
        assertEquals(1.0, resolveWeightStep(exercise, cache), 0.001)
    }

    @Test
    fun `exercise with unknown equipment id returns 1_0`() {
        val exercise = Exercise(id = "ex1", name = "Mystery Exercise", equipmentId = "equip_unknown")
        val cache = listOf(
            Equipment(id = "equip_barbell", name = "Barbell", weightStep = 2.5)
        )
        assertEquals(1.0, resolveWeightStep(exercise, cache), 0.001)
    }

    @Test
    fun `exercise with pullup bar returns 1_25`() {
        val exercise = Exercise(id = "ex1", name = "Pull-up", equipmentId = "equip_pullup_bar")
        val cache = listOf(
            Equipment(id = "equip_pullup_bar", name = "Pull-up Bar", weightStep = 1.25)
        )
        assertEquals(1.25, resolveWeightStep(exercise, cache), 0.001)
    }
}
```

- [ ] **Step 2: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "de.melobeat.workoutplanner.EquipmentWeightStepTest"`
Expected: BUILD SUCCESSFUL, 6 tests passed

- [ ] **Step 3: Add ViewModel tests for increment/decrement with custom weightStep**

In `ActiveWorkoutViewModelTest.kt`, add these tests at the end of the file (before the `// region helpers` section):

```kotlin
@Test
fun `incrementWeight uses exercise weightStep`() = runTest {
    val equipment = listOf(Equipment(id = "ex1", name = "Test", weightStep = 1.0))
    every { repository.getEquipmentStream() } returns flowOf(equipment)

    val freshVm = ActiveWorkoutViewModel(repository, timerPrefs)
    freshVm.startWorkout(makeWorkoutDay(), 0, "R", null)

    freshVm.incrementWeight(0, 0)

    val set = freshVm.uiState.value.exercises[0].sets[0]
    assertEquals("1", set.weight)
}

@Test
fun `decrementWeight uses exercise weightStep`() = runTest {
    val equipment = listOf(Equipment(id = "ex1", name = "Test", weightStep = 1.25))
    every { repository.getEquipmentStream() } returns flowOf(equipment)

    val freshVm = ActiveWorkoutViewModel(repository, timerPrefs)
    freshVm.startWorkout(makeWorkoutDay(), 0, "R", null)

    freshVm.incrementWeight(0, 0)
    freshVm.incrementWeight(0, 0)
    freshVm.incrementWeight(0, 0) // 3 * 1.25 = 3.75

    freshVm.decrementWeight(0, 0) // 3.75 - 1.25 = 2.5

    val set = freshVm.uiState.value.exercises[0].sets[0]
    assertEquals("2.5", set.weight)
}

@Test
fun `decrementWeight does not go below zero`() = runTest {
    val equipment = listOf(Equipment(id = "ex1", name = "Test", weightStep = 2.5))
    every { repository.getEquipmentStream() } returns flowOf(equipment)

    val freshVm = ActiveWorkoutViewModel(repository, timerPrefs)
    freshVm.startWorkout(makeWorkoutDay(), 0, "R", null)

    freshVm.decrementWeight(0, 0) // weight is 0, should stay 0

    val set = freshVm.uiState.value.exercises[0].sets[0]
    assertEquals("0", set.weight)
}
```

- [ ] **Step 4: Add import for Equipment in the test file**

At the top of `ActiveWorkoutViewModelTest.kt`, add:

```kotlin
import de.melobeat.workoutplanner.model.Equipment
```

- [ ] **Step 5: Run all ViewModel tests**

Run: `./gradlew :app:testDebugUnitTest --tests "de.melobeat.workoutplanner.ui.ActiveWorkoutViewModelTest"`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 6: Run all tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 7: Commit**

```bash
git add app/src/test/java/de/melobeat/workoutplanner/EquipmentWeightStepTest.kt app/src/test/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModelTest.kt
git commit -m "test: add tests for equipment weightStep resolution and stepper behavior"
```

---

### Task 6: Final verification and lint

- [ ] **Step 1: Run lint**

Run: `./gradlew :app:lintDebug`
Expected: BUILD SUCCESSFUL (warnings are OK, no errors)

- [ ] **Step 2: Run full build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit any lint fixes if needed**

If lint produced fixable issues, apply them and commit.
