# Equipment Default Weight & User Profile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `defaultWeight` to equipment, `isBodyweight` flag to exercises, and a new User Profile screen with age/height/body-weight stored in DataStore.

**Architecture:** Three independent layers of change — (1) data layer: entities, domain models, repository methods, seed data, DB version bump; (2) user profile infrastructure: new DataStore file, new repository, new ViewModel, Hilt wiring; (3) UI layer: update Equipment and Exercise dialogs/list items, add Profile screen, wire navigation. Each task produces a buildable, testable state.

**Tech Stack:** Kotlin, Jetpack Compose, Room 2.8.4, Hilt 2.59.2, DataStore Preferences, Navigation Compose 2.9.7, KSP 2.2.20-2.0.3

---

## File Map

| Action | File |
|--------|------|
| Modify | `app/src/main/java/de/melobeat/workoutplanner/data/Entities.kt` |
| Modify | `app/src/main/java/de/melobeat/workoutplanner/model/Equipment.kt` |
| Modify | `app/src/main/java/de/melobeat/workoutplanner/model/Exercise.kt` |
| Modify | `app/src/main/java/de/melobeat/workoutplanner/data/Mappers.kt` |
| Modify | `app/src/main/java/de/melobeat/workoutplanner/data/InitialData.kt` |
| Modify | `app/src/main/java/de/melobeat/workoutplanner/data/WorkoutDatabase.kt` |
| Modify | `app/src/main/java/de/melobeat/workoutplanner/data/WorkoutRepository.kt` |
| Modify | `app/src/main/assets/equipment.json` |
| Create | `app/src/main/java/de/melobeat/workoutplanner/data/UserProfileRepository.kt` |
| Modify | `app/src/main/java/de/melobeat/workoutplanner/di/DatabaseModule.kt` |
| Create | `app/src/main/java/de/melobeat/workoutplanner/ui/UserProfileViewModel.kt` |
| Create | `app/src/main/java/de/melobeat/workoutplanner/ui/ProfileScreen.kt` |
| Modify | `app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseLibraryViewModel.kt` |
| Modify | `app/src/main/java/de/melobeat/workoutplanner/ui/EquipmentScreen.kt` |
| Modify | `app/src/main/java/de/melobeat/workoutplanner/ui/ExercisesScreen.kt` |
| Modify | `app/src/main/java/de/melobeat/workoutplanner/ui/SettingsScreen.kt` |
| Modify | `app/src/main/java/de/melobeat/workoutplanner/ui/navigation/NavRoutes.kt` |
| Modify | `app/src/main/java/de/melobeat/workoutplanner/ui/navigation/WorkoutNavGraph.kt` |
| Create | `app/src/test/java/de/melobeat/workoutplanner/data/UserProfileRepositoryTest.kt` |

---

## Task 1: Data Layer — Equipment & Exercise Entities

Add `defaultWeight: Double?` to `EquipmentEntity`, `isBodyweight: Boolean` to `ExerciseEntity`, bump DB version to 8, update seed DTO classes and the equipment JSON.

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/data/Entities.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/data/InitialData.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/data/WorkoutDatabase.kt`
- Modify: `app/src/main/assets/equipment.json`

- [ ] **Step 1: Add `defaultWeight` to `EquipmentEntity`**

In `data/Entities.kt` replace the `EquipmentEntity` class (lines 27–31):

```kotlin
@Entity(tableName = "equipment")
data class EquipmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val defaultWeight: Double? = null
)
```

- [ ] **Step 2: Add `isBodyweight` to `ExerciseEntity`**

In `data/Entities.kt` replace the `ExerciseEntity` class (lines 45–51):

```kotlin
@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = EquipmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipmentId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("equipmentId")]
)
data class ExerciseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val muscleGroup: String,
    val description: String,
    val equipmentId: String? = null,
    val isBodyweight: Boolean = false
)
```

- [ ] **Step 3: Bump DB version to 8 and add destructive migration for 8**

In `data/WorkoutDatabase.kt` change:

```kotlin
// line 24 — change version 7 → 8
    version = 8,
```

```kotlin
// line 38 — add 8 to the migration list
                    .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7, 8)
```

> Note: Adding 8 to the list is defensive — if a DB at version 8 with a schema different from what we're creating here ever exists (e.g. from a reverted deploy), Room will wipe it rather than crash.

- [ ] **Step 4: Update `InitialEquipment` DTO to carry `defaultWeight`**

In `data/InitialData.kt` replace the entire file:

```kotlin
package de.melobeat.workoutplanner.data

import kotlinx.serialization.Serializable

@Serializable
data class InitialEquipment(val id: String, val name: String, val defaultWeight: Double? = null)

@Serializable
data class InitialExercise(
    val name: String,
    val muscleGroup: String,
    val description: String,
    val equipmentId: String?
)
```

- [ ] **Step 5: Update the equipment seed callback to pass `defaultWeight`**

In `data/WorkoutDatabase.kt` replace the `equipmentList.forEach` block (lines 56–58):

```kotlin
                                    equipmentList.forEach { equip ->
                                        dao.insertEquipment(
                                            EquipmentEntity(
                                                id = equip.id,
                                                name = equip.name,
                                                defaultWeight = equip.defaultWeight
                                            )
                                        )
                                    }
```

- [ ] **Step 6: Replace `assets/equipment.json`**

Replace the entire file with:

```json
[
  { "id": "equip_barbell",       "name": "Barbell",         "defaultWeight": 20.0 },
  { "id": "equip_ez_bar",        "name": "EZ Bar",          "defaultWeight": 10.0 },
  { "id": "equip_dumbbell",      "name": "Dumbbell"                               },
  { "id": "equip_pullup_bar",    "name": "Pull-up Bar"                            },
  { "id": "equip_kettlebell",    "name": "Kettlebell"                             },
  { "id": "equip_cable_machine", "name": "Cable Machine"                          },
  { "id": "equip_none",          "name": "None / Bodyweight"                      }
]
```

> `equip_bench` is removed and replaced by `equip_ez_bar`. `Json { ignoreUnknownKeys = true }` in the seed means adding `defaultWeight` does not break existing DB reads.

- [ ] **Step 7: Verify the build compiles**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL (Room will generate new schema for version 8)

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/data/Entities.kt \
        app/src/main/java/de/melobeat/workoutplanner/data/InitialData.kt \
        app/src/main/java/de/melobeat/workoutplanner/data/WorkoutDatabase.kt \
        app/src/main/assets/equipment.json
git commit -m "feat(data): add defaultWeight to EquipmentEntity, isBodyweight to ExerciseEntity, bump DB to v8"
```

---

## Task 2: Domain Models & Mappers

Update the `Equipment` and `Exercise` domain models and their mappers to surface the new fields.

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/model/Equipment.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/model/Exercise.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/data/Mappers.kt`

- [ ] **Step 1: Update `Equipment` domain model**

Replace `model/Equipment.kt`:

```kotlin
package de.melobeat.workoutplanner.model

data class Equipment(
    val id: String,
    val name: String,
    val defaultWeight: Double? = null
)
```

- [ ] **Step 2: Update `Exercise` domain model**

Replace `model/Exercise.kt`:

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
    val routineSets: List<RoutineSet> = emptyList()
)
```

- [ ] **Step 3: Update mappers**

In `data/Mappers.kt` replace:

```kotlin
fun EquipmentEntity.toDomain() = Equipment(id, name)
```

with:

```kotlin
fun EquipmentEntity.toDomain() = Equipment(id, name, defaultWeight)
```

Replace the `ExerciseWithEquipment.toDomain()` function (lines 10–17):

```kotlin
fun ExerciseWithEquipment.toDomain() = Exercise(
    id = exercise.id,
    name = exercise.name,
    description = exercise.description,
    muscleGroup = exercise.muscleGroup,
    equipmentId = exercise.equipmentId,
    equipmentName = equipment?.name,
    isBodyweight = exercise.isBodyweight
)
```

Replace the `WorkoutDayExerciseWithDetails.toDomain()` function (lines 34–42):

```kotlin
fun WorkoutDayExerciseWithDetails.toDomain() = Exercise(
    id = exercise.exercise.id,
    name = exercise.exercise.name,
    description = exercise.exercise.description,
    muscleGroup = exercise.exercise.muscleGroup,
    equipmentId = exercise.exercise.equipmentId,
    equipmentName = exercise.equipment?.name,
    isBodyweight = exercise.exercise.isBodyweight,
    routineSets = dayExercise.routineSets
)
```

- [ ] **Step 4: Build to verify no mapper type errors**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/model/Equipment.kt \
        app/src/main/java/de/melobeat/workoutplanner/model/Exercise.kt \
        app/src/main/java/de/melobeat/workoutplanner/data/Mappers.kt
git commit -m "feat(model): propagate defaultWeight and isBodyweight through domain models and mappers"
```

---

## Task 3: Repository Methods — Equipment & Exercise

Update `WorkoutRepository.saveEquipment` and `saveExercise` to accept and persist the new fields. Update `ExerciseLibraryViewModel` to match.

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/data/WorkoutRepository.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseLibraryViewModel.kt`

- [ ] **Step 1: Update `saveEquipment` in `WorkoutRepository`**

Replace lines 106–110 in `data/WorkoutRepository.kt`:

```kotlin
    suspend fun saveEquipment(name: String, defaultWeight: Double?, existingId: String?) = withContext(dispatcher) {
        dao.insertEquipment(
            EquipmentEntity(id = existingId ?: UUID.randomUUID().toString(), name = name, defaultWeight = defaultWeight)
        )
    }
```

- [ ] **Step 2: Update `saveExercise` in `WorkoutRepository`**

Replace lines 84–100 in `data/WorkoutRepository.kt`:

```kotlin
    suspend fun saveExercise(
        name: String,
        muscleGroup: String,
        description: String,
        equipmentId: String?,
        isBodyweight: Boolean,
        existingId: String?
    ) = withContext(dispatcher) {
        dao.insertExercise(
            ExerciseEntity(
                id = existingId ?: UUID.randomUUID().toString(),
                name = name,
                muscleGroup = muscleGroup,
                description = description,
                equipmentId = equipmentId,
                isBodyweight = isBodyweight
            )
        )
    }
```

- [ ] **Step 3: Update `ExerciseLibraryViewModel.saveEquipment`**

Replace lines 71–79 in `ui/ExerciseLibraryViewModel.kt`:

```kotlin
    fun saveEquipment(name: String, defaultWeight: Double?, existingId: String?) {
        viewModelScope.launch {
            try {
                repository.saveEquipment(name, defaultWeight, existingId)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to save equipment") }
            }
        }
    }
```

- [ ] **Step 4: Update `ExerciseLibraryViewModel.saveExercise`**

Replace lines 45–59 in `ui/ExerciseLibraryViewModel.kt`:

```kotlin
    fun saveExercise(
        name: String,
        muscleGroup: String,
        description: String,
        equipmentId: String?,
        isBodyweight: Boolean,
        existingId: String?
    ) {
        viewModelScope.launch {
            try {
                repository.saveExercise(name, muscleGroup, description, equipmentId, isBodyweight, existingId)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to save exercise") }
            }
        }
    }
```

- [ ] **Step 5: Build to verify signatures compile**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL (EquipmentScreen and ExercisesScreen will have compile errors — they will be fixed in Task 5 and 6)

> If compile errors appear only in `EquipmentScreen.kt` and `ExercisesScreen.kt`, that is expected and acceptable at this step. Proceed to Task 4.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/data/WorkoutRepository.kt \
        app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseLibraryViewModel.kt
git commit -m "feat(data): update saveEquipment/saveExercise signatures to include new fields"
```

---

## Task 4: User Profile — Repository & DataStore

Create the `UserProfile` domain model and `UserProfileRepository` backed by a new DataStore file, and wire it through Hilt.

**Files:**
- Create: `app/src/main/java/de/melobeat/workoutplanner/data/UserProfileRepository.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/di/DatabaseModule.kt`
- Create: `app/src/test/java/de/melobeat/workoutplanner/data/UserProfileRepositoryTest.kt`

- [ ] **Step 1: Write the failing tests first**

Create `app/src/test/java/de/melobeat/workoutplanner/data/UserProfileRepositoryTest.kt`:

```kotlin
package de.melobeat.workoutplanner.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UserProfileRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun makeRepo(): UserProfileRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tmpFolder.newFile("test_user_profile.preferences_pb") }
        )
        return UserProfileRepository(dataStore)
    }

    @Test
    fun `returns all nulls when nothing written`() = runTest {
        val repo = makeRepo()
        val profile = repo.userProfile.first()
        assertNull(profile.age)
        assertNull(profile.heightCm)
        assertNull(profile.bodyWeightKg)
    }

    @Test
    fun `updateAge persists and is readable`() = runTest {
        val repo = makeRepo()
        repo.updateAge(30)
        assertEquals(30, repo.userProfile.first().age)
    }

    @Test
    fun `updateHeight persists and is readable`() = runTest {
        val repo = makeRepo()
        repo.updateHeight(180)
        assertEquals(180, repo.userProfile.first().heightCm)
    }

    @Test
    fun `updateBodyWeight persists and is readable`() = runTest {
        val repo = makeRepo()
        repo.updateBodyWeight(85.5f)
        assertEquals(85.5f, repo.userProfile.first().bodyWeightKg)
    }

    @Test
    fun `updateAge null clears the value`() = runTest {
        val repo = makeRepo()
        repo.updateAge(25)
        repo.updateAge(null)
        assertNull(repo.userProfile.first().age)
    }

    @Test
    fun `updateBodyWeight null clears the value`() = runTest {
        val repo = makeRepo()
        repo.updateBodyWeight(75f)
        repo.updateBodyWeight(null)
        assertNull(repo.userProfile.first().bodyWeightKg)
    }

    @Test
    fun `fields are independent — updating one does not affect others`() = runTest {
        val repo = makeRepo()
        repo.updateAge(28)
        repo.updateBodyWeight(90f)
        val profile = repo.userProfile.first()
        assertEquals(28, profile.age)
        assertEquals(90f, profile.bodyWeightKg)
        assertNull(profile.heightCm)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew :app:testDebugUnitTest --tests "de.melobeat.workoutplanner.data.UserProfileRepositoryTest"
```

Expected: FAILED — `UserProfileRepository` does not exist yet.

- [ ] **Step 3: Create `UserProfileRepository`**

Create `app/src/main/java/de/melobeat/workoutplanner/data/UserProfileRepository.kt`:

```kotlin
package de.melobeat.workoutplanner.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Domain model defined here (not in model/ — small, tightly coupled to this repository)
data class UserProfile(
    val age: Int? = null,
    val heightCm: Int? = null,
    val bodyWeightKg: Float? = null
)

class UserProfileRepository(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val AGE            = intPreferencesKey("age")
        val HEIGHT_CM      = intPreferencesKey("height_cm")
        val BODY_WEIGHT_KG = floatPreferencesKey("body_weight_kg")
    }

    val userProfile: Flow<UserProfile> = dataStore.data.map { prefs ->
        UserProfile(
            age          = prefs[AGE],
            heightCm     = prefs[HEIGHT_CM],
            bodyWeightKg = prefs[BODY_WEIGHT_KG]
        )
    }

    suspend fun updateAge(age: Int?) {
        dataStore.edit { prefs ->
            if (age != null) prefs[AGE] = age else prefs.remove(AGE)
        }
    }

    suspend fun updateHeight(heightCm: Int?) {
        dataStore.edit { prefs ->
            if (heightCm != null) prefs[HEIGHT_CM] = heightCm else prefs.remove(HEIGHT_CM)
        }
    }

    suspend fun updateBodyWeight(bodyWeightKg: Float?) {
        dataStore.edit { prefs ->
            if (bodyWeightKg != null) prefs[BODY_WEIGHT_KG] = bodyWeightKg else prefs.remove(BODY_WEIGHT_KG)
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew :app:testDebugUnitTest --tests "de.melobeat.workoutplanner.data.UserProfileRepositoryTest"
```

Expected: 7 tests PASSED.

- [ ] **Step 5: Wire `UserProfileRepository` through Hilt**

In `di/DatabaseModule.kt`, add after the `provideRestTimerPreferencesRepository` provider (after line 63). Also add the import for `UserProfileRepository` at the top of the file.

Add to imports at top:

```kotlin
import de.melobeat.workoutplanner.data.UserProfileRepository
```

Add a new DataStore and repository provider. The user profile needs its **own** DataStore file (separate from `"rest_timer_prefs"`):

```kotlin
    @Provides
    @Singleton
    @UserProfileDataStore
    fun provideUserProfileDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("user_profile_prefs") }
        )

    @Provides
    @Singleton
    fun provideUserProfileRepository(
        @UserProfileDataStore dataStore: DataStore<Preferences>
    ): UserProfileRepository = UserProfileRepository(dataStore)
```

Also add the `@UserProfileDataStore` qualifier annotation. Add it near the `@IoDispatcher` qualifier at lines 22–24:

```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserProfileDataStore
```

The full set of imports to add to `DatabaseModule.kt`:

```kotlin
import de.melobeat.workoutplanner.data.UserProfileRepository
```

> The existing `DataStore<Preferences>` provider (line 54) is unqualified and used only by `RestTimerPreferencesRepository`. The new provider is qualified with `@UserProfileDataStore` to avoid a Hilt duplicate-binding error.

- [ ] **Step 6: Build to verify Hilt wiring compiles**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/data/UserProfileRepository.kt \
        app/src/main/java/de/melobeat/workoutplanner/di/DatabaseModule.kt \
        app/src/test/java/de/melobeat/workoutplanner/data/UserProfileRepositoryTest.kt
git commit -m "feat(data): add UserProfileRepository with DataStore-backed age/height/bodyweight"
```

---

## Task 5: EquipmentScreen — Dialog & List Item

Update the Equipment dialog to include a bar weight field and show secondary text in the list when weight is set.

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/EquipmentScreen.kt`

- [ ] **Step 1: Update `EquipmentDialog` to accept and return `defaultWeight`**

Replace the `EquipmentDialog` composable (lines 137–170) in `ui/EquipmentScreen.kt`:

```kotlin
@Composable
fun EquipmentDialog(
    initialEquipment: Equipment? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, defaultWeight: Double?) -> Unit
) {
    var name by remember { mutableStateOf(initialEquipment?.name ?: "") }
    var weightText by remember {
        mutableStateOf(initialEquipment?.defaultWeight?.let {
            if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
        } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialEquipment == null) "Add Equipment" else "Edit Equipment") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Equipment Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { input ->
                        // allow digits, one decimal point, optional leading minus not needed
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        // prevent multiple decimal points
                        val dotCount = filtered.count { it == '.' }
                        weightText = if (dotCount <= 1) filtered else weightText
                    },
                    label = { Text("Bar weight (kg)") },
                    placeholder = { Text("e.g. 20") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val weight = weightText.toDoubleOrNull()
                    onConfirm(name, weight)
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (initialEquipment == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

Also add the missing import at the top of the file (if not already present):

```kotlin
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
```

> Check the existing wildcard imports (`import androidx.compose.foundation.layout.*`) — `Column`, `Spacer`, and `height` are covered by `layout.*` so no new imports may be needed.

- [ ] **Step 2: Update the `EquipmentScreenContent` to pass `defaultWeight` through**

In `EquipmentScreenContent`, the `onSaveEquipment` lambda signature must change to include `defaultWeight`. Replace lines 41–49:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentScreenContent(
    equipment: List<Equipment>,
    onBack: () -> Unit,
    onSaveEquipment: (name: String, defaultWeight: Double?, id: String?) -> Unit,
    onDeleteEquipment: (id: String) -> Unit,
    modifier: Modifier = Modifier
)
```

Update the `onConfirm` call-site in the dialog invocation (line 103–107) to pass `defaultWeight`:

```kotlin
                onConfirm = { name, defaultWeight ->
                    onSaveEquipment(name, defaultWeight, equipmentToEdit?.id)
                    showAddDialog = false
                    equipmentToEdit = null
                }
```

- [ ] **Step 3: Show `defaultWeight` as secondary text in the list item**

Replace the `ListItem` inside `items(equipment)` (lines 83–92):

```kotlin
                ListItem(
                    headlineContent = { Text(item.name, fontWeight = FontWeight.SemiBold) },
                    supportingContent = item.defaultWeight?.let { weight ->
                        {
                            val display = if (weight == weight.toLong().toDouble())
                                "${weight.toLong()} kg bar" else "$weight kg bar"
                            Text(display)
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { equipmentToDelete = item }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete Equipment", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.clickable { equipmentToEdit = item }
                )
```

- [ ] **Step 4: Update `EquipmentScreen` (the Hilt-connected wrapper) to use new signature**

Replace lines 35–36 in `EquipmentScreen`:

```kotlin
        onSaveEquipment = { name, defaultWeight, id -> viewModel.saveEquipment(name, defaultWeight, id) },
```

- [ ] **Step 5: Update preview to use new signature**

Replace the `EquipmentScreenContentPreview` call at line 207 (update `onSaveEquipment` lambda):

```kotlin
            onSaveEquipment = { _, _, _ -> },
```

- [ ] **Step 6: Build to verify no errors**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL (ExercisesScreen may still have compile errors from Task 3 — fix those in Task 6)

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/EquipmentScreen.kt
git commit -m "feat(ui): update EquipmentDialog to accept bar weight, show weight in list item"
```

---

## Task 6: ExercisesScreen — Bodyweight Switch in Dialog

Add a "Uses body weight as base load" toggle to the exercise dialog and thread `isBodyweight` through the call chain.

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/ExercisesScreen.kt`

- [ ] **Step 1: Update `AddExerciseDialog` signature and add `Switch` row**

Replace `AddExerciseDialog` (lines 149–244) in `ui/ExercisesScreen.kt`:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialog(
    initialExercise: Exercise? = null,
    equipmentList: List<Equipment>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, muscleGroup: String, description: String, equipmentId: String?, isBodyweight: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialExercise?.name ?: "") }
    var muscleGroup by remember { mutableStateOf(initialExercise?.muscleGroup ?: "") }
    var description by remember { mutableStateOf(initialExercise?.description ?: "") }
    var selectedEquipmentId by remember { mutableStateOf(initialExercise?.equipmentId) }
    var isBodyweight by remember { mutableStateOf(initialExercise?.isBodyweight ?: false) }

    var expanded by remember { mutableStateOf(false) }
    val selectedEquipmentName = equipmentList.find { it.id == selectedEquipmentId }?.name ?: "No Equipment"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialExercise == null) "Add New Exercise" else "Edit Exercise") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = muscleGroup,
                    onValueChange = { muscleGroup = it },
                    label = { Text("Muscle Group") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Equipment", style = MaterialTheme.typography.labelMedium)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = selectedEquipmentName, modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("No Equipment") },
                            onClick = {
                                selectedEquipmentId = null
                                expanded = false
                            }
                        )
                        equipmentList.forEach { equipment ->
                            DropdownMenuItem(
                                text = { Text(equipment.name) },
                                onClick = {
                                    selectedEquipmentId = equipment.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Uses body weight as base load",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Extra weight only is tracked in the workout (e.g. pull-ups, dips)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isBodyweight,
                        onCheckedChange = { isBodyweight = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, muscleGroup, description, selectedEquipmentId, isBodyweight) },
                enabled = name.isNotBlank() && muscleGroup.isNotBlank()
            ) {
                Text(if (initialExercise == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

Also add `Switch` to imports — it is part of `material3.*` so already covered.

- [ ] **Step 2: Update `ExercisesScreenContent` signature and call-sites**

Replace lines 50–58 (the `ExercisesScreenContent` composable signature and `onSaveExercise` type):

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreenContent(
    exercises: List<Exercise>,
    equipment: List<Equipment>,
    onBack: () -> Unit,
    onSaveExercise: (name: String, muscle: String, desc: String, equipId: String?, isBodyweight: Boolean, id: String?) -> Unit,
    onDeleteExercise: (id: String) -> Unit,
    modifier: Modifier = Modifier
)
```

Update the `onConfirm` lambda in the dialog invocation (lines 115–119):

```kotlin
                onConfirm = { name, muscle, desc, equipId, isBodyweight ->
                    onSaveExercise(name, muscle, desc, equipId, isBodyweight, exerciseToEdit?.id)
                    showAddDialog = false
                    exerciseToEdit = null
                }
```

- [ ] **Step 3: Update `ExercisesScreen` (Hilt wrapper) to forward `isBodyweight`**

Replace lines 42–44:

```kotlin
        onSaveExercise = { name, muscle, desc, equipId, isBodyweight, id ->
            viewModel.saveExercise(name, muscle, desc, equipId, isBodyweight, id)
        },
```

- [ ] **Step 4: Update the preview lambdas**

In `ExercisesScreenContentPreview` (line 306) update the lambda:

```kotlin
            onSaveExercise = { _, _, _, _, _, _ -> },
```

In `AddExerciseDialogPreview` (line 323) update:

```kotlin
            onConfirm = { _, _, _, _, _ -> }
```

- [ ] **Step 5: Build to verify**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/ExercisesScreen.kt
git commit -m "feat(ui): add isBodyweight toggle to AddExerciseDialog"
```

---

## Task 7: UserProfileViewModel

Create the ViewModel for the Profile screen.

**Files:**
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/UserProfileViewModel.kt`

- [ ] **Step 1: Create `UserProfileViewModel`**

Create `app/src/main/java/de/melobeat/workoutplanner/ui/UserProfileViewModel.kt`:

```kotlin
package de.melobeat.workoutplanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.melobeat.workoutplanner.data.UserProfile
import de.melobeat.workoutplanner.data.UserProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val repository: UserProfileRepository
) : ViewModel() {

    val profile: StateFlow<UserProfile> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    fun updateAge(input: String) {
        viewModelScope.launch {
            repository.updateAge(input.trim().toIntOrNull())
        }
    }

    fun updateHeight(input: String) {
        viewModelScope.launch {
            repository.updateHeight(input.trim().toIntOrNull())
        }
    }

    fun updateBodyWeight(input: String) {
        viewModelScope.launch {
            repository.updateBodyWeight(input.trim().toFloatOrNull())
        }
    }
}
```

- [ ] **Step 2: Build to verify Hilt can resolve the dependency**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/UserProfileViewModel.kt
git commit -m "feat(ui): add UserProfileViewModel backed by UserProfileRepository"
```

---

## Task 8: ProfileScreen

Create the Profile screen composable with `LargeTopAppBar` + `exitUntilCollapsedScrollBehavior`, three `OutlinedTextField`s that auto-save on focus loss.

**Files:**
- Create: `app/src/main/java/de/melobeat/workoutplanner/ui/ProfileScreen.kt`

- [ ] **Step 1: Create `ProfileScreen.kt`**

Create `app/src/main/java/de/melobeat/workoutplanner/ui/ProfileScreen.kt`:

```kotlin
package de.melobeat.workoutplanner.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.melobeat.workoutplanner.data.UserProfile
import de.melobeat.workoutplanner.ui.theme.WorkoutPlannerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    ProfileScreenContent(
        profile = profile,
        onBack = onBack,
        onAgeChange = { viewModel.updateAge(it) },
        onHeightChange = { viewModel.updateHeight(it) },
        onBodyWeightChange = { viewModel.updateBodyWeight(it) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    profile: UserProfile,
    onBack: () -> Unit,
    onAgeChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onBodyWeightChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Black) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = innerPadding
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                AutoSaveTextField(
                    label = "Age",
                    suffix = "years",
                    initialValue = profile.age?.toString() ?: "",
                    onSave = onAgeChange,
                    keyboardType = KeyboardType.Number
                )
                Spacer(modifier = Modifier.height(12.dp))
                AutoSaveTextField(
                    label = "Height",
                    suffix = "cm",
                    initialValue = profile.heightCm?.toString() ?: "",
                    onSave = onHeightChange,
                    keyboardType = KeyboardType.Number
                )
                Spacer(modifier = Modifier.height(12.dp))
                AutoSaveTextField(
                    label = "Body weight",
                    suffix = "kg",
                    initialValue = profile.bodyWeightKg?.let {
                        if (it == it.toLong().toFloat()) it.toLong().toString() else it.toString()
                    } ?: "",
                    onSave = onBodyWeightChange,
                    keyboardType = KeyboardType.Decimal
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * A text field that saves its value when it loses focus.
 * The local [text] state mirrors user edits; on focus-lost it calls [onSave].
 */
@Composable
private fun AutoSaveTextField(
    label: String,
    suffix: String,
    initialValue: String,
    onSave: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                if (!focusState.isFocused) onSave(text)
            }
    )
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenContentPreview() {
    WorkoutPlannerTheme {
        ProfileScreenContent(
            profile = UserProfile(age = 30, heightCm = 180, bodyWeightKg = 80f),
            onBack = {},
            onAgeChange = {},
            onHeightChange = {},
            onBodyWeightChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenContentEmptyPreview() {
    WorkoutPlannerTheme {
        ProfileScreenContent(
            profile = UserProfile(),
            onBack = {},
            onAgeChange = {},
            onHeightChange = {},
            onBodyWeightChange = {}
        )
    }
}
```

- [ ] **Step 2: Build to verify**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/ProfileScreen.kt
git commit -m "feat(ui): add ProfileScreen with auto-save age/height/body-weight fields"
```

---

## Task 9: Settings Screen — Profile Entry

Add the "Profile" navigation item to `SettingsScreen`, wire it in the nav graph.

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/SettingsScreen.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/navigation/NavRoutes.kt`
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/navigation/WorkoutNavGraph.kt`

- [ ] **Step 1: Add `ProfileRoute` to `NavRoutes.kt`**

In `ui/navigation/NavRoutes.kt` add after the last line (`@Serializable object TimerSettingsRoute`):

```kotlin
@Serializable object ProfileRoute
```

- [ ] **Step 2: Add `onNavigateToProfile` parameter to `SettingsScreen`**

In `ui/SettingsScreen.kt` replace the `SettingsScreen` composable signature (lines 42–49):

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToExercises: () -> Unit,
    onNavigateToRoutines: () -> Unit,
    onNavigateToEquipment: () -> Unit,
    onNavigateToTimerSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    timerSettingsViewModel: TimerSettingsViewModel = hiltViewModel()
)
```

- [ ] **Step 3: Add "Profile" list item after "Timer Settings" in `SettingsScreen`**

Add `Icons.Rounded.Person` to the imports. The full import needed:

```kotlin
import androidx.compose.material.icons.rounded.Person
```

After the Timer Settings item block (after line 115, before the "Manage Exercises" item), insert:

```kotlin
            item {
                SettingsListItem(
                    title = "Profile",
                    subtitle = "Age, height and body weight",
                    icon = Icons.Rounded.Person,
                    onClick = onNavigateToProfile
                )
                HorizontalDivider()
            }
```

- [ ] **Step 4: Fix `SettingsScreenPreview` to pass the new param**

In the `SettingsScreenPreview` (line 177), add `onNavigateToProfile = {}`:

```kotlin
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    WorkoutPlannerTheme {
        SettingsScreen(
            onNavigateToExercises = {},
            onNavigateToRoutines = {},
            onNavigateToEquipment = {},
            onNavigateToTimerSettings = {},
            onNavigateToProfile = {},
            onBack = {}
        )
    }
}
```

- [ ] **Step 5: Wire navigation in `WorkoutNavGraph.kt`**

Add the `ProfileScreen` import at the top of `WorkoutNavGraph.kt`:

```kotlin
import de.melobeat.workoutplanner.ui.ProfileScreen
```

Add `ProfileRoute` to the imports:

```kotlin
// already: import de.melobeat.workoutplanner.ui.navigation.* (all routes)
```

In the `SettingsScreen(...)` call (lines 71–78), add `onNavigateToProfile`:

```kotlin
            composable<SettingsRoute> {
                SettingsScreen(
                    onNavigateToExercises = { navController.navigate(ExercisesRoute) },
                    onNavigateToRoutines = { navController.navigate(RoutinesRoute) },
                    onNavigateToEquipment = { navController.navigate(EquipmentRoute) },
                    onNavigateToTimerSettings = { navController.navigate(TimerSettingsRoute) },
                    onNavigateToProfile = { navController.navigate(ProfileRoute) },
                    onBack = { navController.popBackStack() }
                )
            }
```

After the `composable<TimerSettingsRoute>` block (line 87), add:

```kotlin
            composable<ProfileRoute> {
                ProfileScreen(onBack = { navController.popBackStack() })
            }
```

- [ ] **Step 6: Build and run all unit tests**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew :app:assembleDebug && ./gradlew :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL, all tests PASSED.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/SettingsScreen.kt \
        app/src/main/java/de/melobeat/workoutplanner/ui/navigation/NavRoutes.kt \
        app/src/main/java/de/melobeat/workoutplanner/ui/navigation/WorkoutNavGraph.kt
git commit -m "feat(ui): add Profile entry to Settings and wire ProfileScreen in nav graph"
```

---

## Task 10: Final Verification

Run the full build and all unit tests to confirm everything is green.

**Files:** none

- [ ] **Step 1: Clean build**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew clean && ./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: All unit tests**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew :app:testDebugUnitTest
```

Expected: All tests PASSED, including:
- `UserProfileRepositoryTest` — 7 tests
- `RestTimerPreferencesRepositoryTest` — 6 tests
- `ExerciseFilterTest`
- `FormatElapsedTimeTest`
- `ActiveWorkoutViewModelTest`
- `RoutinesViewModelTest`

- [ ] **Step 3: Lint check**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew :app:lintDebug
```

Expected: No new errors introduced.

- [ ] **Step 4: Final commit (if anything was left unstaged)**

```bash
git status
# If clean, nothing to do. If there are uncommitted changes from lint fixes:
git add -A
git commit -m "chore: address lint warnings from equipment-weight-user-profile feature"
```
