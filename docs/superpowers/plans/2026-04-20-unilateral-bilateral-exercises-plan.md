# Unilateral/Bilateral Exercise Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add support for unilateral (one-side) and bilateral (both-sides) exercises, with separate L/R rep tracking during workouts and combined volume calculations.

**Architecture:** Extend existing data models (ExerciseEntity, RoutineSet, SetUiState, ExerciseHistory) with sideType, leftReps, rightReps fields. Add SideType enum. Update ViewModel logic for unilateral-aware set completion, rep decrementing, and history saving. Update UI for dual steppers and side type selection.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Hilt, kotlinx.serialization

---

### Task 1: Add SideType enum and extend data models

**Files:**
- Create: `app/src/main/java/de/melobeat/workoutplanner/model/SideType.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/model/Exercise.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/data/Entities.kt`

- [ ] **Step 1: Create SideType enum**

Create `app/src/main/java/de/melobeat/workoutplanner/model/SideType.kt`:

```kotlin
package de.melobeat.workoutplanner.model

enum class SideType { Bilateral, Unilateral }
```

- [ ] **Step 2: Extend Exercise domain model**

Modify `app/src/main/java/de/melobeat/workoutplanner/model/Exercise.kt` — add `sideType` field:

```kotlin
package de.melobeat.workoutplanner.model

data class Exercise(
    val id: String,
    val name: String,
    val description: String = "",
    val muscleGroup: String = "",
    val equipmentId: String? = null,
    val equipmentName: String? = null,
    val isBodyweight: Boolean = false,
    val sideType: SideType = SideType.Bilateral,
    val routineSets: List<RoutineSet> = emptyList()
)
```

- [ ] **Step 3: Extend RoutineSet**

In `app/src/main/java/de/melobeat/workoutplanner/model/Exercise.kt`, update `RoutineSet`:

```kotlin
@kotlinx.serialization.Serializable
data class RoutineSet(
    val reps: Int,
    val weight: Double,
    val isAmrap: Boolean = false,
    val sideType: String = "Bilateral",
    val leftReps: Int? = null,
    val rightReps: Int? = null
)
```

- [ ] **Step 4: Extend ExerciseEntity**

Modify `app/src/main/java/de/melobeat/workoutplanner/data/Entities.kt` — add `sideType` to `ExerciseEntity`:

```kotlin
data class ExerciseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val muscleGroup: String,
    val description: String,
    val equipmentId: String? = null,
    val isBodyweight: Boolean = false,
    val sideType: String = "Bilateral"
)
```

- [ ] **Step 5: Extend WorkoutDayExerciseEntity**

In the same file, add `sideType` to `WorkoutDayExerciseEntity`:

```kotlin
@TypeConverters(Converters::class)
data class WorkoutDayExerciseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workoutDayId: String,
    val exerciseId: String,
    val routineSets: List<RoutineSet> = emptyList(),
    val order: Int,
    val sideType: String? = null
)
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/model/SideType.kt app/src/main/java/de/melobeat/workoutplanner/model/Exercise.kt app/src/main/java/de/melobeat/workoutplanner/data/Entities.kt
git commit -m "feat: add SideType enum and extend data models for unilateral support"
```

---

### Task 2: Extend SetUiState and ExerciseHistory

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutUiState.kt`

- [ ] **Step 1: Extend SetUiState**

Modify `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutUiState.kt` — add fields to `SetUiState`:

```kotlin
data class SetUiState(
    val index: Int,
    val weight: String,
    val reps: String,
    val isAmrap: Boolean,
    val isDone: Boolean = false,
    val originalReps: String,
    val sideType: String = "Bilateral",
    val leftReps: Int? = null,
    val rightReps: Int? = null,
    val leftOriginalReps: Int? = null,
    val rightOriginalReps: Int? = null,
)
```

- [ ] **Step 2: Extend ExerciseHistory**

In the same file, add fields to `ExerciseHistory`:

```kotlin
data class ExerciseHistory(
    val exerciseId: String,
    val reps: Int,
    val weight: Double,
    val setIndex: Int = 1,
    val isAmrap: Boolean = false,
    val sideType: String = "Bilateral",
    val leftReps: Int? = null,
    val rightReps: Int? = null
)
```

- [ ] **Step 3: Add ExerciseUiState sideType field**

Add `sideType` to `ExerciseUiState`:

```kotlin
data class ExerciseUiState(
    val exerciseId: String,
    val name: String,
    val sets: List<SetUiState>,
    val isExpanded: Boolean = true,
    val lastSets: List<Pair<Double, Int>> = emptyList(),
    val weightStep: Double = 2.5,
    val sideType: String = "Bilateral"
)
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutUiState.kt
git commit -m "feat: extend SetUiState, ExerciseHistory, ExerciseUiState with sideType and L/R reps"
```

---

### Task 3: Update mappers and repository

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/data/Mappers.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/data/WorkoutRepository.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/data/InitialData.kt`
- Modify: `app/src/main/assets/exercises.json`

- [ ] **Step 1: Update ExerciseWithEquipment.toDomain()**

Modify `app/src/main/java/de/melobeat/workoutplanner/data/Mappers.kt`:

```kotlin
fun ExerciseWithEquipment.toDomain() = Exercise(
    id = exercise.id,
    name = exercise.name,
    description = exercise.description,
    muscleGroup = exercise.muscleGroup,
    equipmentId = exercise.equipmentId,
    equipmentName = equipment?.name,
    isBodyweight = exercise.isBodyweight,
    sideType = de.melobeat.workoutplanner.model.SideType.valueOf(exercise.sideType)
)
```

- [ ] **Step 2: Update WorkoutDayExerciseWithDetails.toDomain()**

In the same file, resolve effective sideType (override > exercise default):

```kotlin
fun WorkoutDayExerciseWithDetails.toDomain() = Exercise(
    id = exercise.exercise.id,
    name = exercise.exercise.name,
    description = exercise.exercise.description,
    muscleGroup = exercise.exercise.muscleGroup,
    equipmentId = exercise.exercise.equipmentId,
    equipmentName = exercise.exercise?.name,
    isBodyweight = exercise.exercise.isBodyweight,
    sideType = dayExercise.sideType?.let { de.melobeat.workoutplanner.model.SideType.valueOf(it) }
        ?: de.melobeat.workoutplanner.model.SideType.valueOf(exercise.exercise.sideType),
    routineSets = dayExercise.routineSets
)
```

- [ ] **Step 3: Update WorkoutRepository.saveExercise**

Modify `app/src/main/java/de/melobeat/workoutplanner/data/WorkoutRepository.kt` — add `sideType` parameter:

```kotlin
suspend fun saveExercise(
    name: String,
    muscleGroup: String,
    description: String,
    equipmentId: String?,
    existingId: String?,
    isBodyweight: Boolean = false,
    sideType: String = "Bilateral"
) = withContext(dispatcher) {
    dao.insertExercise(
        ExerciseEntity(
            id = existingId ?: UUID.randomUUID().toString(),
            name = name,
            muscleGroup = muscleGroup,
            description = description,
            equipmentId = equipmentId,
            isBodyweight = isBodyweight,
            sideType = sideType
        )
    )
}
```

- [ ] **Step 4: Update InitialExercise**

Modify `app/src/main/java/de/melobeat/workoutplanner/data/InitialData.kt`:

```kotlin
@Serializable
data class InitialExercise(
    val name: String,
    val muscleGroup: String,
    val description: String,
    val equipmentId: String?,
    val sideType: String = "Bilateral"
)
```

- [ ] **Step 5: Update exercises.json**

Modify `app/src/main/assets/exercises.json` — add `"sideType": "Bilateral"` to each entry. For bicep curls, lateral raises, single-leg exercises, set `"sideType": "Unilateral"`. Current entries:

```json
[
  {"name": "Bench Press", "muscleGroup": "Chest", "description": "Lay on bench and press barbell up", "equipmentId": "equip_barbell", "sideType": "Bilateral"},
  {"name": "Incline Bench Press", "muscleGroup": "Chest", "description": "Press barbell on incline bench", "equipmentId": "equip_barbell", "sideType": "Bilateral"},
  {"name": "Overhead Press", "muscleGroup": "Shoulders", "description": "Press barbell overhead while standing", "equipmentId": "equip_barbell", "sideType": "Bilateral"},
  {"name": "Barbell Row", "muscleGroup": "Back", "description": "Bend over and row barbell to chest", "equipmentId": "equip_barbell", "sideType": "Bilateral"},
  {"name": "Pull Up", "muscleGroup": "Back", "description": "Pull body up to bar", "equipmentId": "equip_pullup_bar", "sideType": "Bilateral"},
  {"name": "Dip", "muscleGroup": "Chest", "description": "Lower and raise body on parallel bars", "equipmentId": "equip_dip_station", "sideType": "Bilateral"},
  {"name": "Squat", "muscleGroup": "Legs", "description": "Squat with barbell on back", "equipmentId": "equip_barbell", "sideType": "Bilateral"},
  {"name": "Deadlift", "muscleGroup": "Back", "description": "Lift barbell from ground to standing", "equipmentId": "equip_barbell", "sideType": "Bilateral"},
  {"name": "Lateral Raise", "muscleGroup": "Shoulders", "description": "Raise dumbbells to the side", "equipmentId": "equip_dumbbell", "sideType": "Unilateral"},
  {"name": "Bicep Curl", "muscleGroup": "Arms", "description": "Curl dumbbell to shoulder", "equipmentId": "equip_dumbbell", "sideType": "Unilateral"},
  {"name": "Tricep Extension", "muscleGroup": "Arms", "description": "Extend dumbbell overhead", "equipmentId": "equip_dumbbell", "sideType": "Unilateral"},
  {"name": "Leg Press", "muscleGroup": "Legs", "description": "Press weight with legs on machine", "equipmentId": "equip_leg_press", "sideType": "Bilateral"},
  {"name": "Calf Raise", "muscleGroup": "Legs", "description": "Raise up on toes", "equipmentId": "equip_calf_raise_machine", "sideType": "Bilateral"}
]
```

- [ ] **Step 6: Update WorkoutDatabase seed loading**

Modify `app/src/main/java/de/melobeat/workoutplanner/data/WorkoutDatabase.kt` — in the `onCreate` callback, add `sideType` to the `ExerciseEntity` construction:

Change this block (lines 66-75):
```kotlin
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
```

To:
```kotlin
exercisesList.forEach { ex ->
    dao.insertExercise(
        ExerciseEntity(
            name = ex.name,
            muscleGroup = ex.muscleGroup,
            description = ex.description,
            equipmentId = ex.equipmentId,
            sideType = ex.sideType
        )
    )
}
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/data/Mappers.kt app/src/main/java/de/melobeat/workoutplanner/data/WorkoutRepository.kt app/src/main/java/de/melobeat/workoutplanner/data/InitialData.kt app/src/main/assets/exercises.json
git commit -m "feat: update mappers, repository, and seed data for sideType"
```

---

### Task 4: Add volume calculation utilities

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutUiState.kt`

- [ ] **Step 1: Add effectiveReps and volume functions**

Add to `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutUiState.kt` (after `SetUiState`):

```kotlin
fun SetUiState.effectiveReps(): Int = when (sideType) {
    "Bilateral" -> reps.toIntOrNull() ?: 0
    "Unilateral" -> (leftReps ?: 0) + (rightReps ?: 0)
    else -> reps.toIntOrNull() ?: 0
}

fun SetUiState.displayReps(): String = when (sideType) {
    "Bilateral" -> reps
    "Unilateral" -> "L:${leftReps ?: 0} / R:${rightReps ?: 0}"
    else -> reps
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutUiState.kt
git commit -m "feat: add effectiveReps and displayReps utilities for SetUiState"
```

---

### Task 5: Update ActiveWorkoutViewModel for unilateral logic

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModel.kt`
- Test: `app/src/test/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModelTest.kt`

- [ ] **Step 1: Update buildExerciseUiState**

Modify `buildExerciseUiState` in `ActiveWorkoutViewModel.kt` to populate sideType and L/R reps:

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
        val routineSetSideType = predefined.getOrNull(i)?.sideType ?: exercise.sideType.name
        val isUnilateral = routineSetSideType == "Unilateral"
        SetUiState(
            index = i,
            weight = weight,
            reps = reps,
            isAmrap = isAmrap,
            originalReps = reps,
            sideType = routineSetSideType,
            leftReps = if (isUnilateral) predefined.getOrNull(i)?.leftReps ?: 0 else null,
            rightReps = if (isUnilateral) predefined.getOrNull(i)?.rightReps ?: 0 else null,
            leftOriginalReps = if (isUnilateral) predefined.getOrNull(i)?.leftReps else null,
            rightOriginalReps = if (isUnilateral) predefined.getOrNull(i)?.rightReps else null,
        )
    }
    return ExerciseUiState(
        exerciseId = exercise.id,
        name = exercise.name,
        sets = sets,
        lastSets = lastSets,
        weightStep = resolveWeightStep(exercise),
        sideType = exercise.sideType.name
    )
}
```

- [ ] **Step 2: Add setLeftReps and setRightReps methods**

Add to `ActiveWorkoutViewModel.kt`:

```kotlin
fun setLeftReps(exerciseIndex: Int, setIndex: Int, reps: Int) {
    _uiState.update { state ->
        state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
            if (ei != exerciseIndex) return@mapIndexed ex
            ex.copy(sets = ex.sets.mapIndexed { si, set ->
                if (si == setIndex) set.copy(leftReps = reps) else set
            })
        })
    }
}

fun setRightReps(exerciseIndex: Int, setIndex: Int, reps: Int) {
    _uiState.update { state ->
        state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
            if (ei != exerciseIndex) return@mapIndexed ex
            ex.copy(sets = ex.sets.mapIndexed { si, set ->
                if (si == setIndex) set.copy(rightReps = reps) else set
            })
        })
    }
}
```

- [ ] **Step 3: Update toggleSetDone for unilateral**

Replace the existing `toggleSetDone` method:

```kotlin
fun toggleSetDone(exerciseIndex: Int, setIndex: Int) {
    _uiState.update { state ->
        state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
            if (ei != exerciseIndex) return@mapIndexed ex
            ex.copy(sets = ex.sets.mapIndexed { si, set ->
                if (si != setIndex) return@mapIndexed set
                if (set.isAmrap) {
                    set // AMRAP handled by RepsDialog
                } else {
                    if (set.sideType == "Unilateral") {
                        // Unilateral: mark done only if both sides have reps > 0
                        val left = set.leftReps ?: 0
                        val right = set.rightReps ?: 0
                        if (!set.isDone) {
                            if (left > 0 && right > 0) set.copy(isDone = true) else set
                        } else {
                            // Decrement both sides
                            val newLeft = maxOf(0, left - 1)
                            val newRight = maxOf(0, right - 1)
                            if (newLeft == 0 && newRight == 0) {
                                set.copy(
                                    leftReps = set.leftOriginalReps,
                                    rightReps = set.rightOriginalReps,
                                    isDone = false
                                )
                            } else {
                                set.copy(leftReps = newLeft, rightReps = newRight)
                            }
                        }
                    } else {
                        // Bilateral: unchanged behavior
                        if (!set.isDone) {
                            set.copy(isDone = true)
                        } else {
                            val currentReps = set.reps.toIntOrNull() ?: 0
                            if (currentReps > 0) {
                                set.copy(reps = (currentReps - 1).toString())
                            } else {
                                set.copy(reps = set.originalReps, isDone = false)
                            }
                        }
                    }
                }
            })
        })
    }
}
```

- [ ] **Step 4: Update completeCurrentSet for unilateral**

Replace the `isDone = true` line in `completeCurrentSet`:

```kotlin
fun completeCurrentSet() {
    val state = _uiState.value
    val ei = state.currentExerciseIndex
    val si = state.currentSetIndex
    cancelRestTimer()
    val exercise = state.exercises.getOrNull(ei) ?: return
    val set = exercise.sets.getOrNull(si) ?: return

    _uiState.update { s ->
        s.copy(exercises = s.exercises.mapIndexed { eIdx, ex ->
            if (eIdx != ei) ex
            else ex.copy(sets = ex.sets.mapIndexed { sIdx, s ->
                if (sIdx != si) s
                else {
                    if (s.sideType == "Unilateral") {
                        val left = s.leftReps ?: 0
                        val right = s.rightReps ?: 0
                        if (left > 0 && right > 0) s.copy(isDone = true) else s
                    } else {
                        s.copy(isDone = true)
                    }
                }
            })
        })
    }
    // ... rest of the method unchanged (navigation logic)
}
```

- [ ] **Step 5: Update addSet for unilateral**

Modify `addSet`:

```kotlin
fun addSet(exerciseIndex: Int) {
    _uiState.update { state ->
        state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
            if (ei != exerciseIndex) return@mapIndexed ex
            val newIndex = ex.sets.size
            val isUnilateral = ex.sideType == "Unilateral"
            ex.copy(sets = ex.sets + SetUiState(
                index = newIndex, weight = "0", reps = "0",
                isAmrap = false, originalReps = "0",
                sideType = ex.sideType,
                leftReps = if (isUnilateral) 0 else null,
                rightReps = if (isUnilateral) 0 else null,
                leftOriginalReps = if (isUnilateral) 0 else null,
                rightOriginalReps = if (isUnilateral) 0 else null,
            ))
        })
    }
}
```

- [ ] **Step 6: Update finishWorkout**

Modify the `finishWorkout` method to include sideType, leftReps, rightReps in ExerciseHistory:

```kotlin
fun finishWorkout() {
    val day = currentWorkoutDay ?: return
    currentWorkoutDay = null
    val duration = _uiState.value.summaryDurationMs
    timerJob?.cancel()
    timerJob = null

    var hasInvalidSets = false
    val history = _uiState.value.exercises.flatMap { exercise ->
        exercise.sets
            .filter { set ->
                if (set.sideType == "Unilateral") {
                    (set.leftReps ?: 0) > 0 || (set.rightReps ?: 0) > 0
                } else {
                    set.reps.isNotEmpty() && set.weight.isNotEmpty()
                }
            }
            .mapIndexedNotNull { setIndex, set ->
                val weight = set.weight.toDoubleOrNull()
                if (weight == null) {
                    hasInvalidSets = true
                    android.util.Log.w("ActiveWorkoutViewModel", "Skipping set with invalid weight='${set.weight}'")
                    null
                } else {
                    if (set.sideType == "Unilateral") {
                        val leftReps = set.leftReps ?: 0
                        val rightReps = set.rightReps ?: 0
                        ExerciseHistory(
                            exerciseId = exercise.exerciseId,
                            reps = leftReps + rightReps,
                            weight = weight,
                            setIndex = setIndex + 1,
                            isAmrap = set.isAmrap,
                            sideType = "Unilateral",
                            leftReps = leftReps,
                            rightReps = rightReps
                        )
                    } else {
                        val reps = set.reps.toIntOrNull()
                        if (reps == null) {
                            hasInvalidSets = true
                            android.util.Log.w("ActiveWorkoutViewModel", "Skipping set with invalid reps='${set.reps}'")
                            null
                        } else {
                            ExerciseHistory(
                                exerciseId = exercise.exerciseId,
                                reps = reps,
                                weight = weight,
                                setIndex = setIndex + 1,
                                isAmrap = set.isAmrap
                            )
                        }
                    }
                }
            }
    }
    // ... rest unchanged
}
```

- [ ] **Step 7: Update swapExercise**

Modify `swapExercise` to preserve sideType:

```kotlin
fun swapExercise(exerciseIndex: Int, newExercise: Exercise) {
    val currentSets = _uiState.value.exercises.getOrNull(exerciseIndex)?.sets?.size ?: 3
    _uiState.update { state ->
        state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
            if (ei != exerciseIndex) return@mapIndexed ex
            buildExerciseUiState(
                newExercise.copy(
                    routineSets = if (newExercise.routineSets.isNotEmpty())
                        newExercise.routineSets
                    else {
                        val isUnilateral = newExercise.sideType == de.melobeat.workoutplanner.model.SideType.Unilateral
                        List(currentSets) {
                            RoutineSet(
                                reps = 0, weight = 0.0,
                                sideType = newExercise.sideType.name,
                                leftReps = if (isUnilateral) 0 else null,
                                rightReps = if (isUnilateral) 0 else null
                            )
                        }
                    }
                ),
                emptyList()
            )
        })
    }
}
```

- [ ] **Step 8: Add increment/decrement L/R reps helper methods**

Add to `ActiveWorkoutViewModel.kt` (after `setRightReps`):

```kotlin
fun incrementLeftReps(exerciseIndex: Int, setIndex: Int) {
    val current = _uiState.value.exercises.getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex)?.leftReps ?: 0
    setLeftReps(exerciseIndex, setIndex, current + 1)
}

fun decrementLeftReps(exerciseIndex: Int, setIndex: Int) {
    val current = _uiState.value.exercises.getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex)?.leftReps ?: 0
    if (current > 0) setLeftReps(exerciseIndex, setIndex, current - 1)
}

fun incrementRightReps(exerciseIndex: Int, setIndex: Int) {
    val current = _uiState.value.exercises.getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex)?.rightReps ?: 0
    setRightReps(exerciseIndex, setIndex, current + 1)
}

fun decrementRightReps(exerciseIndex: Int, setIndex: Int) {
    val current = _uiState.value.exercises.getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex)?.rightReps ?: 0
    if (current > 0) setRightReps(exerciseIndex, setIndex, current - 1)
}
```

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModel.kt
git commit -m "feat: update ActiveWorkoutViewModel for unilateral set logic"
```

---

### Task 6: Write unilateral tests for ActiveWorkoutViewModel

**Files:**
- Modify: `app/src/test/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModelTest.kt`

- [ ] **Step 1: Add unilateral test helper**

Add to the helpers region in `ActiveWorkoutViewModelTest.kt`:

```kotlin
private fun makeUnilateralExercise() = Exercise(
    id = "e_uni",
    name = "Bicep Curl",
    muscleGroup = "Arms",
    sideType = de.melobeat.workoutplanner.model.SideType.Unilateral,
    routineSets = listOf(
        RoutineSet(reps = 10, weight = 15.0, sideType = "Unilateral", leftReps = 10, rightReps = 10),
        RoutineSet(reps = 8, weight = 15.0, sideType = "Unilateral", leftReps = 8, rightReps = 8)
    )
)
```

- [ ] **Step 2: Test unilateral set requires both sides before marking done**

```kotlin
@Test
fun `toggleSetDone on unilateral set does nothing when only one side has reps`() = runTest {
    val day = WorkoutDay(
        id = "d1", name = "Day",
        exercises = listOf(makeUnilateralExercise())
    )
    viewModel.startWorkout(day, 0, "R", null)

    // Only set left reps, right is 0
    viewModel.setLeftReps(0, 0, 10)
    viewModel.toggleSetDone(0, 0)

    assertFalse(viewModel.uiState.value.exercises[0].sets[0].isDone)
}
```

- [ ] **Step 3: Test unilateral set marks done when both sides have reps**

```kotlin
@Test
fun `toggleSetDone on unilateral set marks done when both sides have reps`() = runTest {
    val day = WorkoutDay(
        id = "d1", name = "Day",
        exercises = listOf(makeUnilateralExercise())
    )
    viewModel.startWorkout(day, 0, "R", null)

    viewModel.setLeftReps(0, 0, 10)
    viewModel.setRightReps(0, 0, 10)
    viewModel.toggleSetDone(0, 0)

    assertTrue(viewModel.uiState.value.exercises[0].sets[0].isDone)
}
```

- [ ] **Step 4: Test tapping done unilateral set decrements both sides**

```kotlin
@Test
fun `toggleSetDone on done unilateral set decrements both sides`() = runTest {
    val day = WorkoutDay(
        id = "d1", name = "Day",
        exercises = listOf(makeUnilateralExercise())
    )
    viewModel.startWorkout(day, 0, "R", null)

    viewModel.setLeftReps(0, 0, 10)
    viewModel.setRightReps(0, 0, 10)
    viewModel.toggleSetDone(0, 0) // mark done

    viewModel.toggleSetDone(0, 0) // tap again - should decrement

    val set = viewModel.uiState.value.exercises[0].sets[0]
    assertEquals(9, set.leftReps)
    assertEquals(9, set.rightReps)
    assertTrue(set.isDone)
}
```

- [ ] **Step 5: Test unilateral set resets when both sides reach zero**

```kotlin
@Test
fun `toggleSetDone on unilateral set resets when both sides reach zero`() = runTest {
    val day = WorkoutDay(
        id = "d1", name = "Day",
        exercises = listOf(
            Exercise(
                id = "e1", name = "Ex",
                sideType = de.melobeat.workoutplanner.model.SideType.Unilateral,
                routineSets = listOf(
                    RoutineSet(reps = 1, weight = 0.0, sideType = "Unilateral", leftReps = 1, rightReps = 1)
                )
            )
        )
    )
    viewModel.startWorkout(day, 0, "R", null)

    viewModel.setLeftReps(0, 0, 1)
    viewModel.setRightReps(0, 0, 1)
    viewModel.toggleSetDone(0, 0) // done
    viewModel.toggleSetDone(0, 0) // L:0, R:0
    viewModel.toggleSetDone(0, 0) // should reset

    val set = viewModel.uiState.value.exercises[0].sets[0]
    assertFalse(set.isDone)
    assertEquals(1, set.leftReps)
    assertEquals(1, set.rightReps)
}
```

- [ ] **Step 6: Test addSet on unilateral initializes L/R reps**

```kotlin
@Test
fun `addSet on unilateral exercise initializes left and right reps to 0`() = runTest {
    val day = WorkoutDay(
        id = "d1", name = "Day",
        exercises = listOf(makeUnilateralExercise())
    )
    viewModel.startWorkout(day, 0, "R", null)
    val before = viewModel.uiState.value.exercises[0].sets.size

    viewModel.addSet(0)

    val sets = viewModel.uiState.value.exercises[0].sets
    assertEquals(before + 1, sets.size)
    val newSet = sets.last()
    assertEquals("Unilateral", newSet.sideType)
    assertEquals(0, newSet.leftReps)
    assertEquals(0, newSet.rightReps)
}
```

- [ ] **Step 7: Test setLeftReps and setRightReps update independently**

```kotlin
@Test
fun `setLeftReps and setRightReps update independently`() = runTest {
    val day = WorkoutDay(
        id = "d1", name = "Day",
        exercises = listOf(makeUnilateralExercise())
    )
    viewModel.startWorkout(day, 0, "R", null)

    viewModel.setLeftReps(0, 0, 12)
    assertEquals(12, viewModel.uiState.value.exercises[0].sets[0].leftReps)
    assertEquals(10, viewModel.uiState.value.exercises[0].sets[0].rightReps) // unchanged

    viewModel.setRightReps(0, 0, 14)
    assertEquals(12, viewModel.uiState.value.exercises[0].sets[0].leftReps) // unchanged
    assertEquals(14, viewModel.uiState.value.exercises[0].sets[0].rightReps)
}
```

- [ ] **Step 8: Test bilateral exercises behave unchanged**

```kotlin
@Test
fun `bilateral exercises behave unchanged with new sideType field`() = runTest {
    viewModel.startWorkout(makeWorkoutDay(), 0, "R", null)

    val set = viewModel.uiState.value.exercises[0].sets[0]
    assertEquals("Bilateral", set.sideType)
    assertNull(set.leftReps)
    assertNull(set.rightReps)

    viewModel.toggleSetDone(0, 0)
    assertTrue(set.isDone) // works same as before
}
```

- [ ] **Step 9: Run tests**

```bash
export JAVA_HOME=/opt/android-studio/jbr && ./gradlew :app:testDebugUnitTest --tests "de.melobeat.workoutplanner.ui.ActiveWorkoutViewModelTest"
```

Expected: All tests pass.

- [ ] **Step 10: Commit**

```bash
git add app/src/test/java/de/melobeat/workoutplanner/ui/ActiveWorkoutViewModelTest.kt
git commit -m "test: add unilateral/bilateral tests for ActiveWorkoutViewModel"
```

---

### Task 7: Add localization strings

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-de/strings.xml`

- [ ] **Step 1: Add English strings**

Add to `app/src/main/res/values/strings.xml`:

```xml
<string name="label_bilateral">Bilateral</string>
<string name="label_unilateral">Unilateral</string>
<string name="label_left">L</string>
<string name="label_right">R</string>
<string name="workout_side_override">Side type</string>
<string name="workout_left_reps">Left reps</string>
<string name="workout_right_reps">Right reps</string>
```

- [ ] **Step 2: Add German strings**

Add to `app/src/main/res/values-de/strings.xml`:

```xml
<string name="label_bilateral">Beidseitig</string>
<string name="label_unilateral">Einseitig</string>
<string name="label_left">L</string>
<string name="label_right">R</string>
<string name="workout_side_override">Seitentyp</string>
<string name="workout_left_reps">Links Wdh.</string>
<string name="workout_right_reps">Rechts Wdh.</string>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git commit -m "feat: add localization strings for unilateral/bilateral feature"
```

---

### Task 8: Update Exercise Library UI

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/ExercisesScreen.kt`

- [ ] **Step 1: Add sideType to AddExerciseDialog**

Modify `AddExerciseDialog` in `ExercisesScreen.kt`:
- Add `var sideType by remember { mutableStateOf(initialExercise?.sideType?.name ?: "Bilateral") }` state
- Add `SingleChoiceSegmentedButtonRow` with Bilateral/Unilateral options after the bodyweight switch
- Update `onConfirm` callback to include sideType

Add this import:
```kotlin
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
```

Add the side type selector after the bodyweight switch row:

```kotlin
Spacer(modifier = Modifier.height(8.dp))

Text(stringResource(R.string.workout_side_override), style = MaterialTheme.typography.labelMedium)
SingleChoiceSegmentedButtonRow {
    SegmentedButton(
        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        onClick = { sideType = "Bilateral" },
        selected = sideType == "Bilateral"
    ) {
        Text(stringResource(R.string.label_bilateral))
    }
    SegmentedButton(
        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        onClick = { sideType = "Unilateral" },
        selected = sideType == "Unilateral"
    ) {
        Text(stringResource(R.string.label_unilateral))
    }
}
```

Update the `onConfirm` call to pass sideType:

```kotlin
TextButton(
    onClick = { onConfirm(name, muscleGroup, description, selectedEquipmentId, isBodyweight, sideType) },
    enabled = name.isNotBlank() && muscleGroup.isNotBlank()
)
```

- [ ] **Step 2: Update ExercisesScreenContent callback signature**

Change the `onSaveExercise` parameter:

```kotlin
onSaveExercise: (name: String, muscle: String, desc: String, equipId: String?, id: String?, isBodyweight: Boolean, sideType: String) -> Unit,
```

Update the call site in `ExercisesScreen`:

```kotlin
onSaveExercise = { name, muscle, desc, equipId, id, isBodyweight, sideType ->
    viewModel.saveExercise(name, muscle, desc, equipId, id, isBodyweight, sideType)
},
```

- [ ] **Step 3: Add Unilateral badge to ExerciseLibraryItem**

In `ExerciseLibraryItem`, show a badge when `sideType != Bilateral`:

```kotlin
Row {
    Text(
        text = exercise.muscleGroup,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary
    )
    if (exercise.sideType == de.melobeat.workoutplanner.model.SideType.Unilateral) {
        Text(
            text = " • ${stringResource(R.string.label_unilateral)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
    exercise.equipmentName?.let {
        Text(
            text = " • $it",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

- [ ] **Step 4: Update previews**

Update `AddExerciseDialogPreview` and `ExerciseLibraryItemPreview` to include `sideType` in their test data.

- [ ] **Step 5: Update ExerciseLibraryViewModel.saveExercise**

Modify `app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseLibraryViewModel.kt` — add `sideType` parameter:

```kotlin
fun saveExercise(
    name: String,
    muscleGroup: String,
    description: String,
    equipmentId: String?,
    existingId: String?,
    isBodyweight: Boolean = false,
    sideType: String = "Bilateral"
) {
    viewModelScope.launch {
        try {
            repository.saveExercise(name, muscleGroup, description, equipmentId, existingId, isBodyweight, sideType)
        } catch (e: Exception) {
            _state.update { it.copy(error = "Failed to save exercise") }
        }
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/ExercisesScreen.kt app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseLibraryViewModel.kt
git commit -m "feat: add side type selector and badge to exercise library UI"
```

---

### Task 9: Update active workout UI for dual steppers

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseCard.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutStepperCard.kt`

- [ ] **Step 1: Add side label to StepperCard**

Modify `StepperCard` in `WorkoutStepperCard.kt` to accept an optional `sideLabel` parameter:

```kotlin
@Composable
fun StepperCard(
    label: String,
    value: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    isAmrap: Boolean = false,
    sideLabel: String? = null
) {
    // ... existing code ...
    val displayLabel = when {
        isAmrap -> "AMRAP"
        sideLabel != null -> "$label ($sideLabel)"
        else -> label.uppercase()
    }
    // ... rest unchanged
}
```

- [ ] **Step 2: Update ExerciseCard active set steppers for unilateral**

In `ExerciseCard.kt`, replace the stepper Row in the active set section:

```kotlin
// Stepper cards
if (set.sideType == "Unilateral") {
    // Weight stepper centered above
    StepperCard(
        label = "kg",
        value = set.weight,
        onIncrement = { onIncrementWeight(si) },
        onDecrement = { onDecrementWeight(si) },
        modifier = Modifier.fillMaxWidth(0.6f).align(Alignment.CenterHorizontally)
    )
    Spacer(Modifier.height(8.dp))
    // L/R steppers side by side
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepperCard(
            label = stringResource(R.string.label_left),
            value = (set.leftReps ?: 0).toString(),
            onIncrement = { onIncrementLeftReps(si) },
            onDecrement = { onDecrementLeftReps(si) },
            sideLabel = "L",
            modifier = Modifier.weight(1f)
        )
        StepperCard(
            label = stringResource(R.string.label_right),
            value = (set.rightReps ?: 0).toString(),
            onIncrement = { onIncrementRightReps(si) },
            onDecrement = { onDecrementRightReps(si) },
            sideLabel = "R",
            modifier = Modifier.weight(1f)
        )
    }
} else {
    // Bilateral: current single-stepper behavior
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepperCard(
            label = "Reps",
            value = set.reps,
            onIncrement = { onIncrementReps(si) },
            onDecrement = { onDecrementReps(si) },
            isAmrap = set.isAmrap,
            modifier = Modifier.weight(1f)
        )
        StepperCard(
            label = "kg",
            value = set.weight,
            onIncrement = { onIncrementWeight(si) },
            onDecrement = { onDecrementWeight(si) },
            modifier = Modifier.weight(1f)
        )
    }
}
```

- [ ] **Step 3: Add unilateral callback parameters to ExerciseCard**

Add to the `ExerciseCard` function signature:

```kotlin
onIncrementLeftReps: (setIndex: Int) -> Unit,
onDecrementLeftReps: (setIndex: Int) -> Unit,
onIncrementRightReps: (setIndex: Int) -> Unit,
onDecrementRightReps: (setIndex: Int) -> Unit,
```

- [ ] **Step 4: Update completed set display for unilateral**

In the completed set row, change the reps display:

```kotlin
Text(
    stringResource(
        if (set.sideType == "Unilateral") R.string.workout_set_reps_weight_unilateral
        else R.string.workout_set_reps_weight,
        // For bilateral: set.reps, set.weight
        // For unilateral: "L:${set.leftReps} / R:${set.rightReps}", set.weight
    ),
    // ...
)
```

Add new string resource `workout_set_reps_weight_unilateral` with format `"%1$s / %2$s kg"`.

- [ ] **Step 5: Update WorkoutScreen to pass new callbacks**

Modify `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutScreen.kt`:

Add new callback parameters to `WorkoutScreenContent`:

```kotlin
onIncrementLeftReps: (exerciseIndex: Int, setIndex: Int) -> Unit,
onDecrementLeftReps: (exerciseIndex: Int, setIndex: Int) -> Unit,
onIncrementRightReps: (exerciseIndex: Int, setIndex: Int) -> Unit,
onDecrementRightReps: (exerciseIndex: Int, setIndex: Int) -> Unit,
```

Wire them in `WorkoutScreen`:

```kotlin
WorkoutScreenContent(
    // ... existing params ...
    onIncrementLeftReps = { ei, si -> viewModel.incrementLeftReps(ei, si) },
    onDecrementLeftReps = { ei, si -> viewModel.decrementLeftReps(ei, si) },
    onIncrementRightReps = { ei, si -> viewModel.incrementRightReps(ei, si) },
    onDecrementRightReps = { ei, si -> viewModel.decrementRightReps(ei, si) },
)
```

Pass them to `ExerciseCard` in the LazyColumn:

```kotlin
ExerciseCard(
    // ... existing params ...
    onIncrementLeftReps = { setIndex -> onIncrementLeftReps(exerciseIndex, setIndex) },
    onDecrementLeftReps = { setIndex -> onDecrementLeftReps(exerciseIndex, setIndex) },
    onIncrementRightReps = { setIndex -> onIncrementRightReps(exerciseIndex, setIndex) },
    onDecrementRightReps = { setIndex -> onDecrementRightReps(exerciseIndex, setIndex) },
)
```

Update the preview `WorkoutScreenContentPreview` to include the new callback parameters (all no-ops).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseCard.kt app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutStepperCard.kt
git commit -m "feat: add dual steppers for unilateral exercises in active workout"
```

---

### Task 10: Update Workout Summary screen

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutSummaryScreen.kt`

- [ ] **Step 1: Read WorkoutSummaryScreen to understand current structure**

Read the file to find where exercise set data is displayed (likely iterating over `ExerciseHistoryEntity` or similar).

- [ ] **Step 2: Display L/R split for unilateral sets**

Wherever reps are displayed for a set, change to:

```kotlin
val repsDisplay = if (entry.sideType == "Unilateral") {
    "L:${entry.leftReps ?: 0} / R:${entry.rightReps ?: 0}"
} else {
    entry.reps.toString()
}
Text(repsDisplay, style = MaterialTheme.typography.bodyMedium)
```

Note: `ExerciseHistoryEntity` in the DB does NOT have sideType/leftReps/rightReps columns. The summary screen reads from `ExerciseHistoryEntity` which only has `reps`, `weight`, `sets`, `isAmrap`. Since `finishWorkout` stores combined reps (`leftReps + rightReps`), the summary will show the total. For a proper L/R split display, we would need to either:
- Add columns to `ExerciseHistoryEntity` (requires DB migration), OR
- Show only the combined total in summary (simpler, acceptable for now)

For this task, just ensure the combined reps value displays correctly. The L/R split can be added later when history schema is updated.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutSummaryScreen.kt
git commit -m "feat: update workout summary for unilateral combined reps"
```

---

### Task 11: Update Routine Editor for side type display

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/RoutineExerciseEditItem.kt`

- [ ] **Step 1: Show side type label in ExerciseEditItem**

In `ExerciseEditItem` in `RoutineExerciseEditItem.kt`, add a side type label next to the exercise name:

Add import:
```kotlin
import de.melobeat.workoutplanner.model.SideType
```

After the exercise name `Text`, add:

```kotlin
if (exercise.sideType == SideType.Unilateral) {
    Text(
        text = stringResource(R.string.label_unilateral),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 4.dp)
    )
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/RoutineExerciseEditItem.kt
git commit -m "feat: show unilateral badge in routine editor"
```

---

### Task 12: Final verification and clean build

- [ ] **Step 1: Run full test suite**

```bash
export JAVA_HOME=/opt/android-studio/jbr && ./gradlew :app:testDebugUnitTest
```

- [ ] **Step 2: Run lint**

```bash
export JAVA_HOME=/opt/android-studio/jbr && ./gradlew :app:lintDebug
```

- [ ] **Step 3: Assemble debug build**

```bash
export JAVA_HOME=/opt/android-studio/jbr && ./gradlew assembleDebug
```

- [ ] **Step 4: Commit all remaining changes**

```bash
git add -A
git commit -m "feat: complete unilateral/bilateral exercise support"
```
