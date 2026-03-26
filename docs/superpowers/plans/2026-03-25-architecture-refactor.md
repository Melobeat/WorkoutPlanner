# Architecture Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor WorkoutPlanner from a monolithic single-ViewModel app with manual navigation state into a clean MVVM architecture with a repository layer, 5 focused ViewModels, and Navigation Compose.

**Architecture:** Single-module MVVM. `WorkoutRepository` wraps `WorkoutDao`; 5 feature-scoped ViewModels inject only the repository; each screen is self-contained (gets its ViewModel via `hiltViewModel()`); `NavHost` replaces all manual `mutableStateOf` navigation in `MainActivity`.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Hilt, Navigation Compose (type-safe routes), Coroutines/Flow, `collectAsStateWithLifecycle`

**Spec:** `docs/superpowers/specs/2026-03-25-architecture-refactor-design.md`

**Build command (run after every task):** `cd /home/kai/AndroidStudioProjects/WorkoutPlanner && ./gradlew assembleDebug`

---

## Task 1: Add dependencies + quick wins

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/example/workoutplanner/data/WorkoutDatabase.kt`

### Goal
Add Navigation Compose and `lifecycle-runtime-compose` (needed for `collectAsStateWithLifecycle`). Fix the destructive migration to protect future schema versions. Remove a broken pre-existing dependency (`hilt-lifecycle-viewmodel-compose` at version 1.3.0 does not exist in Maven — it was deprecated at 1.0.0-alpha03).

- [ ] **Step 1: Add versions + library entries to `libs.versions.toml`**

In the `[versions]` block, add:
```toml
navigationCompose = "2.9.0"
```

In the `[libraries]` block, add:
```toml
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeKtx" }
```

Also remove this broken entry from `[libraries]`:
```toml
androidx-hilt-lifecycle-viewmodel-compose = { group = "androidx.hilt", name = "hilt-lifecycle-viewmodel-compose", version.ref = "hiltNavigationCompose" }
```

- [ ] **Step 2: Add/remove the dependencies in `app/build.gradle.kts`**

Inside the `dependencies { }` block, add:
```kotlin
implementation(libs.androidx.navigation.compose)
implementation(libs.androidx.lifecycle.runtime.compose)
```

Remove (it references the broken artifact removed above):
```kotlin
implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
```

- [ ] **Step 3: Fix `WorkoutDatabase.kt` — protect future schema versions**

In `WorkoutDatabase.kt`, change:
```kotlin
.fallbackToDestructiveMigration(true)
```
to:
```kotlin
.fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7)
```

- [ ] **Step 4: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/java/com/example/workoutplanner/data/WorkoutDatabase.kt
git commit -m "feat: add Navigation Compose dep and fix destructive migration scope"
```

---

## Task 2: Add DAO query + create Mappers.kt

**Files:**
- Modify: `app/src/main/java/com/example/workoutplanner/data/WorkoutDao.kt`
- Create: `app/src/main/java/com/example/workoutplanner/data/Mappers.kt`

### Goal
Add the missing `getRoutineWithDays(routineId)` Flow query to the DAO. Move all `toDomain()` extension functions out of `WorkoutViewModel.kt` into a dedicated `Mappers.kt` — `WorkoutViewModel.kt` still exists and still imports from there (no breakage yet).

- [ ] **Step 1: Add the new DAO query to `WorkoutDao.kt`**

At the end of the `// Routines` section (after line 52, before `@Insert suspend fun insertRoutine`), add:

```kotlin
@Transaction
@Query("SELECT * FROM routines WHERE id = :routineId LIMIT 1")
fun getRoutineWithDays(routineId: String): Flow<RoutineWithDays?>
```

- [ ] **Step 2: Create `data/Mappers.kt`**

```kotlin
package com.example.workoutplanner.data

import com.example.workoutplanner.model.Equipment
import com.example.workoutplanner.model.Exercise
import com.example.workoutplanner.model.Routine
import com.example.workoutplanner.model.WorkoutDay

fun EquipmentEntity.toDomain() = Equipment(id, name)

fun ExerciseWithEquipment.toDomain() = Exercise(
    id = exercise.id,
    name = exercise.name,
    description = exercise.description,
    muscleGroup = exercise.muscleGroup,
    equipmentId = exercise.equipmentId,
    equipmentName = equipment?.name
)

fun RoutineWithDays.toDomain() = Routine(
    id = routine.id,
    name = routine.name,
    description = routine.description,
    workoutDays = days.sortedBy { it.day.order }.map { it.toDomain() },
    isSelected = routine.isSelected,
    lastCompletedDayIndex = routine.lastCompletedDayIndex
)

fun WorkoutDayWithExercises.toDomain() = WorkoutDay(
    id = day.id,
    name = day.name,
    exercises = exercises.sortedBy { it.dayExercise.order }.map { it.toDomain() }
)

fun WorkoutDayExerciseWithDetails.toDomain() = Exercise(
    id = exercise.exercise.id,
    name = exercise.exercise.name,
    description = exercise.exercise.description,
    muscleGroup = exercise.exercise.muscleGroup,
    equipmentId = exercise.exercise.equipmentId,
    equipmentName = exercise.equipment?.name,
    routineSets = dayExercise.routineSets
)
```

- [ ] **Step 3: Update `WorkoutViewModel.kt` to import from Mappers instead of defining them**

At the bottom of `WorkoutViewModel.kt`, delete the five extension functions (`EquipmentEntity.toDomain()`, `ExerciseWithEquipment.toDomain()`, `RoutineWithDays.toDomain()`, `WorkoutDayWithExercises.toDomain()`, `WorkoutDayExerciseWithDetails.toDomain()`).

Add this import at the top of `WorkoutViewModel.kt`:
```kotlin
import com.example.workoutplanner.data.toDomain
```

(The functions already exist in `Mappers.kt` in the same package, so they will resolve automatically without the explicit import — but verify the file compiles.)

- [ ] **Step 4: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/data/WorkoutDao.kt \
        app/src/main/java/com/example/workoutplanner/data/Mappers.kt \
        app/src/main/java/com/example/workoutplanner/WorkoutViewModel.kt
git commit -m "refactor: extract toDomain mappers and add getRoutineWithDays DAO query"
```

---

## Task 3: Create WorkoutRepository + update DI

**Files:**
- Create: `app/src/main/java/com/example/workoutplanner/data/WorkoutRepository.kt`
- Modify: `app/src/main/java/com/example/workoutplanner/di/DatabaseModule.kt`

### Goal
Introduce the repository layer. `WorkoutViewModel` continues to work (it still injects `WorkoutDao` directly — that changes in later tasks when it is deleted).

- [ ] **Step 1: Create `data/WorkoutRepository.kt`**

```kotlin
package com.example.workoutplanner.data

import com.example.workoutplanner.di.IoDispatcher
import com.example.workoutplanner.model.Equipment
import com.example.workoutplanner.model.Exercise
import com.example.workoutplanner.model.Routine
import com.example.workoutplanner.model.WorkoutDay
import com.example.workoutplanner.ui.ExerciseHistory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val dao: WorkoutDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {

    fun getRoutinesStream(): Flow<List<Routine>> =
        dao.getAllRoutinesWithDays().map { it.map(RoutineWithDays::toDomain) }

    fun getSelectedRoutineStream(): Flow<Routine?> =
        dao.getSelectedRoutineWithDays().map { it?.toDomain() }

    fun getRoutineStream(routineId: String): Flow<Routine?> =
        dao.getRoutineWithDays(routineId).map { it?.toDomain() }

    fun getExercisesStream(): Flow<List<Exercise>> =
        dao.getAllExercisesWithEquipment().map { it.map(ExerciseWithEquipment::toDomain) }

    fun getEquipmentStream(): Flow<List<Equipment>> =
        dao.getAllEquipment().map { it.map(EquipmentEntity::toDomain) }

    fun getWorkoutHistoryStream(): Flow<List<WorkoutHistoryWithExercises>> =
        dao.getAllWorkoutHistoryWithExercises()

    fun getHistoryForExercise(exerciseId: String): Flow<List<ExerciseHistoryEntity>> =
        dao.getHistoryForExercise(exerciseId)

    suspend fun selectRoutine(routineId: String) = withContext(dispatcher) {
        dao.selectRoutine(routineId)
    }

    suspend fun saveRoutine(
        name: String,
        description: String,
        days: List<WorkoutDay>,
        existingId: String?
    ) = withContext(dispatcher) {
        val routineId = existingId ?: UUID.randomUUID().toString()
        val routineEntity = RoutineEntity(id = routineId, name = name, description = description)
        val daysWithExercises = days.mapIndexed { dayIndex, day ->
            val dayId = if (day.id.isBlank() || day.id.startsWith("temp_"))
                UUID.randomUUID().toString() else day.id
            val dayEntity = WorkoutDayEntity(
                id = dayId, routineId = routineId, name = day.name, order = dayIndex
            )
            val exerciseEntities = day.exercises.mapIndexed { exIndex, ex ->
                WorkoutDayExerciseEntity(
                    workoutDayId = dayId,
                    exerciseId = ex.id,
                    routineSets = ex.routineSets,
                    order = exIndex
                )
            }
            dayEntity to exerciseEntities
        }
        dao.upsertRoutine(routineEntity, daysWithExercises)
    }

    suspend fun deleteRoutine(routineId: String) = withContext(dispatcher) {
        dao.deleteRoutine(routineId)
    }

    suspend fun updateLastCompletedDayIndex(routineId: String, dayIndex: Int) =
        withContext(dispatcher) {
            dao.updateLastCompletedDayIndex(routineId, dayIndex)
        }

    suspend fun saveExercise(
        name: String,
        muscleGroup: String,
        description: String,
        equipmentId: String?,
        existingId: String?
    ) = withContext(dispatcher) {
        dao.insertExercise(
            ExerciseEntity(
                id = existingId ?: UUID.randomUUID().toString(),
                name = name,
                muscleGroup = muscleGroup,
                description = description,
                equipmentId = equipmentId
            )
        )
    }

    suspend fun deleteExercise(exerciseId: String) = withContext(dispatcher) {
        dao.deleteExercise(exerciseId)
    }

    suspend fun saveEquipment(name: String, existingId: String?) = withContext(dispatcher) {
        dao.insertEquipment(
            EquipmentEntity(id = existingId ?: UUID.randomUUID().toString(), name = name)
        )
    }

    suspend fun deleteEquipment(equipmentId: String) = withContext(dispatcher) {
        dao.deleteEquipment(equipmentId)
    }

    suspend fun finishWorkout(
        history: List<ExerciseHistory>,
        workoutDay: WorkoutDay,
        dayIndex: Int,
        durationMs: Long,
        routineName: String
    ) = withContext(dispatcher) {
        val workoutHistoryId = UUID.randomUUID().toString()
        dao.insertWorkoutHistory(
            WorkoutHistoryEntity(
                id = workoutHistoryId,
                routineName = routineName,
                workoutDayName = workoutDay.name,
                date = System.currentTimeMillis(),
                durationMillis = durationMs
            )
        )
        history.forEach { entry ->
            dao.insertExerciseHistory(
                ExerciseHistoryEntity(
                    workoutHistoryId = workoutHistoryId,
                    exerciseId = entry.exerciseId,
                    date = System.currentTimeMillis(),
                    sets = entry.setIndex,
                    reps = entry.reps,
                    weight = entry.weight,
                    isAmrap = entry.isAmrap
                )
            )
        }
        dao.updateLastCompletedDayIndex(
            dao.getSelectedRoutineWithDays().map { it?.routine?.id }.let {
                kotlinx.coroutines.flow.first(it) ?: return@withContext
            },
            dayIndex
        )
    }
}
```

> **Note on `finishWorkout`:** The `updateLastCompletedDayIndex` call at the end is intentionally handled here. However, the calling ViewModel (`ActiveWorkoutViewModel`) already knows the routineId — pass it as a parameter instead to simplify. See the revised signature below — update the method signature to accept `routineId: String` too:

Actually, simplify `finishWorkout` — remove the Flow lookup and take `routineId` as a param:

```kotlin
suspend fun finishWorkout(
    history: List<ExerciseHistory>,
    workoutDay: WorkoutDay,
    dayIndex: Int,
    durationMs: Long,
    routineName: String,
    routineId: String?
) = withContext(dispatcher) {
    val workoutHistoryId = UUID.randomUUID().toString()
    dao.insertWorkoutHistory(
        WorkoutHistoryEntity(
            id = workoutHistoryId,
            routineName = routineName,
            workoutDayName = workoutDay.name,
            date = System.currentTimeMillis(),
            durationMillis = durationMs
        )
    )
    history.forEach { entry ->
        dao.insertExerciseHistory(
            ExerciseHistoryEntity(
                workoutHistoryId = workoutHistoryId,
                exerciseId = entry.exerciseId,
                date = System.currentTimeMillis(),
                sets = entry.setIndex,
                reps = entry.reps,
                weight = entry.weight,
                isAmrap = entry.isAmrap
            )
        )
    }
    routineId?.let { dao.updateLastCompletedDayIndex(it, dayIndex) }
}
```

- [ ] **Step 2: Add `@IoDispatcher` qualifier + repository binding to `DatabaseModule.kt`**

```kotlin
package com.example.workoutplanner.di

import android.content.Context
import com.example.workoutplanner.data.WorkoutDao
import com.example.workoutplanner.data.WorkoutDatabase
import com.example.workoutplanner.data.WorkoutRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WorkoutDatabase {
        return WorkoutDatabase.getDatabase(context)
    }

    @Provides
    fun provideWorkoutDao(database: WorkoutDatabase): WorkoutDao {
        return database.workoutDao()
    }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideWorkoutRepository(
        dao: WorkoutDao,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): WorkoutRepository = WorkoutRepository(dao, dispatcher)
}
```

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/data/WorkoutRepository.kt \
        app/src/main/java/com/example/workoutplanner/di/DatabaseModule.kt
git commit -m "feat: add WorkoutRepository and IoDispatcher qualifier"
```

---

## Task 4: Create HomeViewModel

**Files:**
- Create: `app/src/main/java/com/example/workoutplanner/ui/HomeViewModel.kt`

- [ ] **Step 1: Create `ui/HomeViewModel.kt`**

```kotlin
package com.example.workoutplanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutplanner.data.WorkoutRepository
import com.example.workoutplanner.data.WorkoutHistoryWithExercises
import com.example.workoutplanner.model.Routine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val selectedRoutine: Routine? = null,
    val recentHistory: List<WorkoutHistoryWithExercises> = emptyList(),
    val exerciseNameMap: Map<String, String> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getSelectedRoutineStream(),
        repository.getWorkoutHistoryStream(),
        repository.getExercisesStream()
    ) { routine, history, exercises ->
        HomeUiState(
            selectedRoutine = routine,
            recentHistory = history.take(5),
            exerciseNameMap = exercises.associate { it.id to it.name },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun updateNextDay(routineId: String, dayIndex: Int) {
        viewModelScope.launch {
            try {
                repository.updateLastCompletedDayIndex(routineId, dayIndex)
            } catch (e: Exception) {
                // Error is non-critical; log only
                e.printStackTrace()
            }
        }
    }

    fun clearError() {
        // No mutable error state currently; placeholder for future use
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/HomeViewModel.kt
git commit -m "feat: add HomeViewModel"
```

---

## Task 5: Create HistoryViewModel

**Files:**
- Create: `app/src/main/java/com/example/workoutplanner/ui/HistoryViewModel.kt`

- [ ] **Step 1: Create `ui/HistoryViewModel.kt`**

```kotlin
package com.example.workoutplanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutplanner.data.WorkoutHistoryWithExercises
import com.example.workoutplanner.data.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HistoryUiState(
    val sessions: List<WorkoutHistoryWithExercises> = emptyList(),
    val exerciseNameMap: Map<String, String> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: WorkoutRepository
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.getWorkoutHistoryStream(),
        repository.getExercisesStream()
    ) { history, exercises ->
        HistoryUiState(
            sessions = history,
            exerciseNameMap = exercises.associate { it.id to it.name },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState()
    )
}
```

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/HistoryViewModel.kt
git commit -m "feat: add HistoryViewModel"
```

---

## Task 6: Create RoutinesViewModel

**Files:**
- Create: `app/src/main/java/com/example/workoutplanner/ui/RoutinesViewModel.kt`

- [ ] **Step 1: Create `ui/RoutinesViewModel.kt`**

```kotlin
package com.example.workoutplanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutplanner.data.WorkoutRepository
import com.example.workoutplanner.model.Routine
import com.example.workoutplanner.model.WorkoutDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutinesUiState(
    val routines: List<Routine> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class RoutinesViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RoutinesUiState())
    val uiState: StateFlow<RoutinesUiState> = _state

    // Drives the detail screen — set by calling loadRoutineDetail(routineId)
    private val _detailRoutineId = MutableStateFlow<String?>(null)
    val detailRoutine: StateFlow<Routine?> = _detailRoutineId
        .flatMapLatest { id -> if (id != null) repository.getRoutineStream(id) else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            repository.getRoutinesStream().collect { routines ->
                _state.update { it.copy(routines = routines, isLoading = false) }
            }
        }
    }

    fun loadRoutineDetail(routineId: String) {
        _detailRoutineId.value = routineId
    }

    fun selectRoutine(routineId: String) {
        viewModelScope.launch {
            try {
                repository.selectRoutine(routineId)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to select routine") }
            }
        }
    }

    fun deleteRoutine(routineId: String) {
        viewModelScope.launch {
            try {
                repository.deleteRoutine(routineId)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to delete routine") }
            }
        }
    }

    fun saveRoutine(name: String, description: String, days: List<WorkoutDay>, existingId: String?) {
        viewModelScope.launch {
            try {
                repository.saveRoutine(name, description, days, existingId)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to save routine") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
```

> **Note on screen usage:** All screens in this group use `viewModel.uiState` (not `routinesUiState` — that name is gone). `RoutineDetailScreen` uses `viewModel.detailRoutine`.

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/RoutinesViewModel.kt
git commit -m "feat: add RoutinesViewModel"
```

---

## Task 7: Create ExerciseLibraryViewModel

**Files:**
- Create: `app/src/main/java/com/example/workoutplanner/ui/ExerciseLibraryViewModel.kt`

- [ ] **Step 1: Create `ui/ExerciseLibraryViewModel.kt`**

Use a single `MutableStateFlow<ExerciseLibraryUiState>` so that both stream updates and mutation errors write to the same state object, and screens need only observe `uiState`.

```kotlin
package com.example.workoutplanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutplanner.data.WorkoutRepository
import com.example.workoutplanner.model.Equipment
import com.example.workoutplanner.model.Exercise
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseLibraryUiState(
    val exercises: List<Exercise> = emptyList(),
    val equipment: List<Equipment> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExerciseLibraryUiState())
    val uiState: StateFlow<ExerciseLibraryUiState> = _state

    init {
        viewModelScope.launch {
            combine(
                repository.getExercisesStream(),
                repository.getEquipmentStream()
            ) { exercises, equipment ->
                exercises to equipment
            }.collect { (exercises, equipment) ->
                _state.update { it.copy(exercises = exercises, equipment = equipment, isLoading = false) }
            }
        }
    }

    fun saveExercise(
        name: String,
        muscleGroup: String,
        description: String,
        equipmentId: String?,
        existingId: String?
    ) {
        viewModelScope.launch {
            try {
                repository.saveExercise(name, muscleGroup, description, equipmentId, existingId)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to save exercise") }
            }
        }
    }

    fun deleteExercise(exerciseId: String) {
        viewModelScope.launch {
            try {
                repository.deleteExercise(exerciseId)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to delete exercise") }
            }
        }
    }

    fun saveEquipment(name: String, existingId: String?) {
        viewModelScope.launch {
            try {
                repository.saveEquipment(name, existingId)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to save equipment") }
            }
        }
    }

    fun deleteEquipment(equipmentId: String) {
        viewModelScope.launch {
            try {
                repository.deleteEquipment(equipmentId)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to delete equipment") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/ExerciseLibraryViewModel.kt
git commit -m "feat: add ExerciseLibraryViewModel"
```

---

## Task 8: Create ActiveWorkoutViewModel

**Files:**
- Create: `app/src/main/java/com/example/workoutplanner/ui/ActiveWorkoutViewModel.kt`

### Notes
- This ViewModel is **scoped to the Activity** (not to a nav back stack entry) so the minimized banner and the full workout screen share the same instance.
- Timer uses a per-workout `Job` that resets on each `startWorkout()` call.
- `ExerciseHistory` is already defined in `WorkoutScreen.kt` — import from there.

- [ ] **Step 1: Create `ui/ActiveWorkoutViewModel.kt`**

```kotlin
package com.example.workoutplanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutplanner.data.WorkoutRepository
import com.example.workoutplanner.model.Exercise
import com.example.workoutplanner.model.RoutineSet
import com.example.workoutplanner.model.WorkoutDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Immutable state classes
data class ActiveWorkoutUiState(
    val isActive: Boolean = false,
    val isFullScreen: Boolean = false,
    val workoutDayName: String = "",
    val exercises: List<ExerciseUiState> = emptyList(),
    val elapsedTime: Long = 0L,
    val isFinished: Boolean = false,
    val error: String? = null
)

data class ExerciseUiState(
    val exerciseId: String,
    val name: String,
    val sets: List<SetUiState>,
    val isExpanded: Boolean = true,
    val lastSets: List<Pair<Double, Int>> = emptyList()
)

data class SetUiState(
    val index: Int,
    val weight: String,
    val reps: String,
    val isAmrap: Boolean,
    val isDone: Boolean = false,
    val originalReps: String
)

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    private var timerJob: Job? = null
    private var currentWorkoutDay: WorkoutDay? = null
    private var currentDayIndex: Int = 0
    private var currentRoutineName: String = ""
    private var currentRoutineId: String? = null

    fun startWorkout(day: WorkoutDay, dayIndex: Int, routineName: String, routineId: String?) {
        viewModelScope.launch {
            try {
                val exerciseStates = day.exercises.map { exercise ->
                    val history = repository.getHistoryForExercise(exercise.id).first()
                    val lastSets = if (history.isNotEmpty()) {
                        val latestWorkoutId = history[0].workoutHistoryId
                        history.filter { it.workoutHistoryId == latestWorkoutId }
                            .sortedBy { it.sets }
                            .map { it.weight to it.reps }
                    } else emptyList()

                    buildExerciseUiState(exercise, lastSets)
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
                        error = null
                    )
                }
                startTimer()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to start workout: ${e.message}") }
            }
        }
    }

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
            lastSets = lastSets
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        _elapsedTime.value = 0L
        val start = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - start
                _elapsedTime.value = elapsed
                _uiState.update { it.copy(elapsedTime = elapsed) }
                delay(1000)
            }
        }
    }

    fun cancelWorkout() {
        timerJob?.cancel()
        timerJob = null
        _elapsedTime.value = 0L
        currentWorkoutDay = null
        _uiState.value = ActiveWorkoutUiState()
    }

    fun finishWorkout() {
        val day = currentWorkoutDay ?: return
        val duration = _elapsedTime.value
        timerJob?.cancel()
        timerJob = null

        val history = _uiState.value.exercises.flatMap { exercise ->
            exercise.sets
                .filter { it.reps.isNotEmpty() && it.weight.isNotEmpty() }
                .mapIndexed { setIndex, set ->
                    ExerciseHistory(
                        exerciseId = exercise.exerciseId,
                        reps = set.reps.toIntOrNull() ?: 0,
                        weight = set.weight.toDoubleOrNull() ?: 0.0,
                        setIndex = setIndex + 1,
                        isAmrap = set.isAmrap
                    )
                }
        }

        viewModelScope.launch {
            try {
                repository.finishWorkout(
                    history = history,
                    workoutDay = day,
                    dayIndex = currentDayIndex,
                    durationMs = duration,
                    routineName = currentRoutineName,
                    routineId = currentRoutineId
                )
                _uiState.value = ActiveWorkoutUiState(isFinished = true)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to save workout: ${e.message}") }
            }
        }
    }

    fun setFullScreen(fullScreen: Boolean) {
        _uiState.update { it.copy(isFullScreen = fullScreen) }
    }

    fun toggleExerciseExpanded(exerciseIndex: Int) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { i, ex ->
                if (i == exerciseIndex) ex.copy(isExpanded = !ex.isExpanded) else ex
            })
        }
    }

    fun toggleSetDone(exerciseIndex: Int, setIndex: Int) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
                if (ei != exerciseIndex) return@mapIndexed ex
                ex.copy(sets = ex.sets.mapIndexed { si, set ->
                    if (si != setIndex) return@mapIndexed set
                    if (set.isAmrap) {
                        set // AMRAP handled by RepsDialog
                    } else {
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
                })
            })
        }
    }

    fun updateSetReps(exerciseIndex: Int, setIndex: Int, reps: String) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
                if (ei != exerciseIndex) return@mapIndexed ex
                ex.copy(sets = ex.sets.mapIndexed { si, set ->
                    if (si == setIndex) set.copy(reps = reps, originalReps = reps, isDone = true)
                    else set
                })
            })
        }
    }

    fun updateSetWeight(exerciseIndex: Int, setIndex: Int, weight: String) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
                if (ei != exerciseIndex) return@mapIndexed ex
                ex.copy(sets = ex.sets.mapIndexed { si, set ->
                    if (si == setIndex) set.copy(weight = weight) else set
                })
            })
        }
    }

    fun addSet(exerciseIndex: Int) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
                if (ei != exerciseIndex) return@mapIndexed ex
                val newIndex = ex.sets.size
                ex.copy(sets = ex.sets + SetUiState(
                    index = newIndex, weight = "0", reps = "0",
                    isAmrap = false, originalReps = "0"
                ))
            })
        }
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
                if (ei != exerciseIndex) return@mapIndexed ex
                if (ex.sets.size <= 1) return@mapIndexed ex
                ex.copy(sets = ex.sets.filterIndexed { si, _ -> si != setIndex }
                    .mapIndexed { i, s -> s.copy(index = i) })
            })
        }
    }

    fun addExercise(exercise: Exercise) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises + buildExerciseUiState(exercise, emptyList()))
        }
    }

    fun swapExercise(exerciseIndex: Int, newExercise: Exercise) {
        val currentSets = _uiState.value.exercises.getOrNull(exerciseIndex)?.sets?.size ?: 3
        _uiState.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
                if (ei != exerciseIndex) return@mapIndexed ex
                buildExerciseUiState(
                    newExercise.copy(
                        routineSets = if (newExercise.routineSets.isNotEmpty())
                            newExercise.routineSets
                        else List(currentSets) { RoutineSet(reps = 0, weight = 0.0) }
                    ),
                    emptyList()
                )
            })
        }
    }

    fun removeExercise(exerciseIndex: Int) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.filterIndexed { i, _ -> i != exerciseIndex })
        }
    }

    fun reorderExercise(from: Int, to: Int) {
        _uiState.update { state ->
            val list = state.exercises.toMutableList()
            val item = list.removeAt(from)
            list.add(to, item)
            state.copy(exercises = list)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/ActiveWorkoutViewModel.kt
git commit -m "feat: add ActiveWorkoutViewModel with immutable UiState"
```

---

## Task 9: Migrate HistoryScreen

**Files:**
- Modify: `app/src/main/java/com/example/workoutplanner/ui/HistoryScreen.kt`
- Modify: `app/src/main/java/com/example/workoutplanner/MainActivity.kt`

### Goal
`HistoryScreen` stops taking a `WorkoutViewModel` and becomes self-contained. It gets its own `HistoryViewModel` internally.

- [ ] **Step 1: Rewrite `HistoryScreen` signature + internals**

Replace the current `HistoryScreen` composable with:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Workout History") })
        },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No workout history yet. Start a workout to see it here!")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.sessions) { session ->
                    WorkoutSessionCard(session, uiState.exerciseNameMap)
                }
            }
        }
    }
}
```

Add missing imports:
```kotlin
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
```

Remove the old imports:
```kotlin
import androidx.compose.runtime.collectAsState  // remove
import com.example.workoutplanner.WorkoutViewModel  // remove
```

- [ ] **Step 2: Update `MainActivity.kt` — remove viewModel arg from HistoryScreen call**

Find:
```kotlin
AppDestinations.HISTORY -> {
    HistoryScreen(
        viewModel = viewModel,
        modifier = contentModifier
    )
}
```

Replace with:
```kotlin
AppDestinations.HISTORY -> {
    HistoryScreen(modifier = contentModifier)
}
```

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/HistoryScreen.kt \
        app/src/main/java/com/example/workoutplanner/MainActivity.kt
git commit -m "refactor: HistoryScreen now uses HistoryViewModel internally"
```

---

## Task 10: Migrate HomeScreen

**Files:**
- Modify: `app/src/main/java/com/example/workoutplanner/ui/HomeScreen.kt`
- Modify: `app/src/main/java/com/example/workoutplanner/MainActivity.kt`

### Goal
`HomeScreen` gets data from `HomeViewModel` internally. It still accepts `onStartWorkout` and `onNavigateToSettings` callbacks from MainActivity (those come from navigation, not data).

- [ ] **Step 1: Rewrite `HomeScreen` signature + internals**

Change signature from:
```kotlin
fun HomeScreen(
    selectedRoutine: Routine?,
    workoutHistory: List<WorkoutHistoryWithExercises>,
    exerciseNameMap: Map<String, String>,
    onStartWorkout: (WorkoutDay, Int) -> Unit,
    onUpdateNextDay: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

To:
```kotlin
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onStartWorkout: () -> Unit,   // called AFTER startWorkout() so caller can navigate to WorkoutScreen
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
)
```

The Start button inside `HomeScreen` calls `viewModel` logic to get the day, then fires `onStartWorkout()` to trigger navigation. In Task 13, `HomeScreen` will also inject `ActiveWorkoutViewModel` internally to call `startWorkout(...)` before firing the callback.

Inside the composable body:
```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
var showWorkoutChooser by remember { mutableStateOf(false) }

// Replace selectedRoutine with uiState.selectedRoutine
// Replace workoutHistory with uiState.recentHistory
// Replace exerciseNameMap with uiState.exerciseNameMap
// Replace onSettingsClick with onNavigateToSettings
// Replace onUpdateNextDay logic:
//   onUpdateNextDay(index) called in WorkoutDayChooserDialog becomes:
//   val routine = uiState.selectedRoutine ?: return@WorkoutDayChooserDialog
//   val totalDays = routine.workoutDays.size
//   val lastCompletedIndex = (index + totalDays - 1) % totalDays
//   viewModel.updateNextDay(routine.id, lastCompletedIndex)
```

The dismiss handler `onDismiss = { }` in `WorkoutDayChooserDialog` call should be changed to:
```kotlin
onDismiss = { showWorkoutChooser = false }
```

Add required imports and remove the ones for plain data types that are no longer params.

- [ ] **Step 2: Update `MainActivity.kt` — update HomeScreen call**

Find the `AppDestinations.HOME` block and replace:
```kotlin
HomeScreen(
    selectedRoutine = activeRoutine,
    workoutHistory = workoutHistory,
    exerciseNameMap = exerciseNameMap,
    onStartWorkout = { day, index ->
        viewModel.startWorkout(day, index)
        isWorkoutMinimized = false
    },
    onUpdateNextDay = { index ->
        activeRoutine?.let { routine ->
            val totalDays = routine.workoutDays.size
            val lastCompletedIndex = (index + totalDays - 1) % totalDays
            viewModel.completeWorkoutDay(routine.id, lastCompletedIndex)
        }
    },
    onSettingsClick = { isSettingsVisible = true },
    modifier = contentModifier
)
```
With:
```kotlin
HomeScreen(
    onNavigateToSettings = { isSettingsVisible = true },
    onStartWorkout = { isWorkoutMinimized = false },  // navigation to WorkoutScreen is still manual here; Task 13 revisits this
    modifier = contentModifier
)
```

Also remove now-unused `val activeRoutine`, `val workoutHistory`, `val exerciseNameMap` from `WorkoutPlannerApp`. Keep `val exercises` if it is still referenced for the workout screen's available exercises.

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/HomeScreen.kt \
        app/src/main/java/com/example/workoutplanner/MainActivity.kt
git commit -m "refactor: HomeScreen now uses HomeViewModel internally"
```

---

## Task 11: Migrate ExercisesScreen + EquipmentScreen

**Files:**
- Modify: `app/src/main/java/com/example/workoutplanner/ui/ExercisesScreen.kt`
- Modify: `app/src/main/java/com/example/workoutplanner/ui/EquipmentScreen.kt`
- Modify: `app/src/main/java/com/example/workoutplanner/MainActivity.kt`

### Goal
Both screens become self-contained via `ExerciseLibraryViewModel`. Fix the dead `onClick = {}` FABs.

- [ ] **Step 1: Rewrite `ExercisesScreen` signature**

Change from:
```kotlin
fun ExercisesScreen(
    exercises: List<Exercise>,
    equipmentList: List<Equipment>,
    onAddExercise: (name: String, muscleGroup: String, description: String, equipmentId: String?) -> Unit,
    onUpdateExercise: (Exercise) -> Unit,
    onDeleteExercise: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
)
```
To:
```kotlin
@Composable
fun ExercisesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseLibraryViewModel = hiltViewModel()
)
```

Inside the body:
```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
// Use uiState.exercises and uiState.equipment in place of the removed params
// Replace onAddExercise call with viewModel.saveExercise(name, muscle, desc, equipId, null)
// Replace onUpdateExercise call with viewModel.saveExercise(e.name, e.muscleGroup, e.description, e.equipmentId, e.id)
// Replace onDeleteExercise with viewModel.deleteExercise(id)
```

Fix the dead FAB — find `onClick = { }` on the FAB and replace with:
```kotlin
onClick = { showAddDialog = true }
```

- [ ] **Step 2: Rewrite `EquipmentScreen` signature**

Change from:
```kotlin
fun EquipmentScreen(
    equipmentList: List<Equipment>,
    onAddEquipment: (String) -> Unit,
    onUpdateEquipment: (Equipment) -> Unit,
    onDeleteEquipment: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
)
```
To:
```kotlin
@Composable
fun EquipmentScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseLibraryViewModel = hiltViewModel()
)
```

Inside the body:
```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
// Use uiState.equipment in place of equipmentList
// Replace onAddEquipment with viewModel.saveEquipment(name, null)
// Replace onUpdateEquipment with viewModel.saveEquipment(equipment.name, equipment.id)
// Replace onDeleteEquipment with viewModel.deleteEquipment(id)
```

Fix the dead FAB in EquipmentScreen too — same pattern.

- [ ] **Step 3: Update `MainActivity.kt` — simplify ExercisesScreen + EquipmentScreen calls**

Replace:
```kotlin
ExercisesScreen(
    exercises = exercises,
    equipmentList = equipment,
    onAddExercise = { name, muscle, desc, equipId -> viewModel.addExercise(name, muscle, desc, equipId) },
    onUpdateExercise = { exercise -> viewModel.updateExercise(exercise) },
    onDeleteExercise = { viewModel.deleteExercise(it) },
    onBack = { settingsSubDestination = null },
    modifier = contentModifier
)
```
With:
```kotlin
ExercisesScreen(
    onBack = { settingsSubDestination = null },
    modifier = contentModifier
)
```

Replace:
```kotlin
EquipmentScreen(
    equipmentList = equipment,
    onAddEquipment = { viewModel.addEquipment(it) },
    onUpdateEquipment = { viewModel.updateEquipment(it) },
    onDeleteEquipment = { viewModel.deleteEquipment(it) },
    onBack = { settingsSubDestination = null },
    modifier = contentModifier
)
```
With:
```kotlin
EquipmentScreen(
    onBack = { settingsSubDestination = null },
    modifier = contentModifier
)
```

- [ ] **Step 4: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/ExercisesScreen.kt \
        app/src/main/java/com/example/workoutplanner/ui/EquipmentScreen.kt \
        app/src/main/java/com/example/workoutplanner/MainActivity.kt
git commit -m "refactor: ExercisesScreen and EquipmentScreen use ExerciseLibraryViewModel; fix dead FABs"
```

---

## Task 12: Migrate RoutinesScreen + RoutineDetailScreen + CreateRoutineScreen

**Files:**
- Modify: `app/src/main/java/com/example/workoutplanner/ui/RoutinesScreen.kt`
- Modify: `app/src/main/java/com/example/workoutplanner/ui/RoutineDetailScreen.kt`
- Modify: `app/src/main/java/com/example/workoutplanner/ui/CreateRoutineScreen.kt`
- Modify: `app/src/main/java/com/example/workoutplanner/MainActivity.kt`

### Goal
All three screens use `RoutinesViewModel`. `RoutineDetailScreen` takes a `routineId: String` instead of a `Routine` object. `MainActivity`'s `selectedRoutineInDetail: Routine?` changes to `selectedRoutineId: String?`.

- [ ] **Step 1: Rewrite `RoutinesScreen` signature**

Change from:
```kotlin
fun RoutinesScreen(
    routines: List<Routine>,
    onRoutineClick: (Routine) -> Unit,
    onCreateRoutineClick: () -> Unit,
    onDeleteRoutine: (String) -> Unit,
    onSelectRoutine: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
)
```
To:
```kotlin
@Composable
fun RoutinesScreen(
    onRoutineClick: (routineId: String) -> Unit,
    onCreateRoutineClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutinesViewModel = hiltViewModel()
)
```

Inside the body:
```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
// Use uiState.routines
// Replace onDeleteRoutine with viewModel.deleteRoutine(id)
// Replace onSelectRoutine with viewModel.selectRoutine(id)
// Replace onRoutineClick(routine) with onRoutineClick(routine.id)
```

- [ ] **Step 2: Rewrite `RoutineDetailScreen` signature**

Change from:
```kotlin
fun RoutineDetailScreen(
    routine: Routine,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
)
```
To:
```kotlin
@Composable
fun RoutineDetailScreen(
    routineId: String,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutinesViewModel = hiltViewModel()
)
```

Inside the body:
```kotlin
LaunchedEffect(routineId) { viewModel.loadRoutineDetail(routineId) }
val routine by viewModel.detailRoutine.collectAsStateWithLifecycle()

if (routine == null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
    return
}
// Use routine!! for the rest of the existing UI
```

- [ ] **Step 3: Rewrite `CreateRoutineScreen` signature**

Change from:
```kotlin
fun CreateRoutineScreen(
    initialRoutine: Routine?,
    availableExercises: List<Exercise>,
    onSave: (name: String, description: String, days: List<WorkoutDay>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
)
```
To:
```kotlin
@Composable
fun CreateRoutineScreen(
    routineId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutinesViewModel = hiltViewModel(),
    exerciseLibraryViewModel: ExerciseLibraryViewModel = hiltViewModel()
)
```

Inside the body:
```kotlin
// If editing, load the existing routine
LaunchedEffect(routineId) { routineId?.let { viewModel.loadRoutineDetail(it) } }
val initialRoutine by if (routineId != null) viewModel.detailRoutine.collectAsStateWithLifecycle()
    else remember { MutableStateFlow<Routine?>(null) }.collectAsStateWithLifecycle()
val exerciseLibState by exerciseLibraryViewModel.uiState.collectAsStateWithLifecycle()
val availableExercises = exerciseLibState.exercises

// Replace onSave call with:
// viewModel.saveRoutine(name, desc, days, routineId)
// onBack()
```

- [ ] **Step 4: Update `MainActivity.kt`**

Change `selectedRoutineInDetail: Routine?` to `selectedRoutineId: String?` and `routineToEdit: Routine?` to `routineToEditId: String?` (storing IDs not objects).

Update `RoutinesScreen` call:
```kotlin
RoutinesScreen(
    onRoutineClick = { routineId -> selectedRoutineId = routineId },
    onCreateRoutineClick = { isCreatingRoutine = true },
    onBack = { settingsSubDestination = null },
    modifier = contentModifier
)
```

Update `RoutineDetailScreen` call:
```kotlin
RoutineDetailScreen(
    routineId = selectedRoutineId!!,
    onBackClick = { selectedRoutineId = null },
    onEditClick = {
        routineToEditId = selectedRoutineId
        selectedRoutineId = null
    },
    modifier = contentModifier
)
```

Update `CreateRoutineScreen` call:
```kotlin
CreateRoutineScreen(
    routineId = routineToEditId,
    onBack = {
        isCreatingRoutine = false
        routineToEditId = null
    },
    modifier = contentModifier
)
```

Update the back handler conditions to use `routineToEditId` instead of `routineToEdit` and `selectedRoutineId` instead of `selectedRoutineInDetail`.

Remove `val routines by viewModel.routines.collectAsState()` from `WorkoutPlannerApp` if no longer used.

- [ ] **Step 5: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/RoutinesScreen.kt \
        app/src/main/java/com/example/workoutplanner/ui/RoutineDetailScreen.kt \
        app/src/main/java/com/example/workoutplanner/ui/CreateRoutineScreen.kt \
        app/src/main/java/com/example/workoutplanner/MainActivity.kt
git commit -m "refactor: Routines screens use RoutinesViewModel; detail screen takes routineId"
```

---

## Task 13: Migrate WorkoutScreen to ActiveWorkoutViewModel

**Files:**
- Modify: `app/src/main/java/com/example/workoutplanner/ui/WorkoutScreen.kt`
- Modify: `app/src/main/java/com/example/workoutplanner/MainActivity.kt`

### Goal
Replace the mutable `ExerciseState`/`SetState` classes with the new immutable `ExerciseUiState`/`SetUiState` from `ActiveWorkoutViewModel`. `WorkoutScreen` becomes self-contained.

`ActiveWorkoutViewModel` is **Activity-scoped** — use `hiltViewModel<ActiveWorkoutViewModel>(LocalContext.current as ComponentActivity)` in both `WorkoutScreen` and the minimized banner in `MainActivity` to share the same instance.

- [ ] **Step 1: Rewrite `WorkoutScreen` to use `ActiveWorkoutViewModel`**

New signature:
```kotlin
@Composable
fun WorkoutScreen(
    modifier: Modifier = Modifier,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(
        viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
    )
)
```

Add import:
```kotlin
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
```

Inside the body:
```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
var showAddExerciseDialog by remember { mutableStateOf(false) }
var exerciseToSwapIndex by remember { mutableStateOf<Int?>(null) }
var showCancelDialog by remember { mutableStateOf(false) }

// Use uiState.workoutDayName for the title
// Use uiState.exercises for the LazyColumn (iterate ExerciseUiState not ExerciseState)
// Use uiState.elapsedTime for formatElapsedTime(uiState.elapsedTime)
```

Replace each mutable operation with a ViewModel call:
- `state.isExpanded = !state.isExpanded` → `viewModel.toggleExerciseExpanded(index)`
- `state.addSet()` → `viewModel.addSet(exerciseIndex)`
- `state.removeSet(it)` → `viewModel.removeSet(exerciseIndex, setIndex)`
- `exerciseStates.removeAt(index)` → `viewModel.removeExercise(index)`
- `exerciseToSwapIndex = index` → still local state
- Move/reorder: `viewModel.reorderExercise(from, to)`
- Tap on reps box → `viewModel.toggleSetDone(exerciseIndex, setIndex)`
- Long tap / AMRAP → show `RepsDialog`, then `viewModel.updateSetReps(exerciseIndex, setIndex, reps)`
- Weight field change → `viewModel.updateSetWeight(exerciseIndex, setIndex, weight)`
- Add exercise dialog confirm → `viewModel.addExercise(exercise); showAddExerciseDialog = false`
- Swap exercise dialog confirm → `viewModel.swapExercise(exerciseToSwapIndex!!, exercise); exerciseToSwapIndex = null`
- Finish workout → `viewModel.finishWorkout()`
- Cancel workout → `viewModel.cancelWorkout()`
- Minimize → `viewModel.setFullScreen(false)`

Fix all dialog `onDismissRequest = { }` to actually dismiss:
- Add/swap dialog: `onDismissRequest = { showAddExerciseDialog = false }` etc.
- Cancel dialog dismiss button: `TextButton(onClick = { showCancelDialog = false })`

`ExerciseCard` now receives `ExerciseUiState` instead of `ExerciseState`. Update its signature and all field references.

Delete `ExerciseState`, `SetState`, and `ActiveWorkout` classes from this file — they are no longer needed (the `ActiveWorkout` class is in `WorkoutViewModel.kt` and will be deleted there too).

Keep `ExerciseHistory`, `formatElapsedTime`, `formatWeight`, `RepsDialog`, `ExerciseSelectionDialog` — they are still used.

- [ ] **Step 2: Update `HomeScreen` to inject `ActiveWorkoutViewModel` and call startWorkout**

`HomeScreen` already accepted an `onStartWorkout: () -> Unit` callback since Task 10. Now wire the actual start logic inside `HomeScreen`:

Change signature:
```kotlin
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    activeWorkoutViewModel: ActiveWorkoutViewModel = hiltViewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
)
```

Inside the Start button's `onClick`:
```kotlin
val routine = uiState.selectedRoutine ?: return@Button
val nextDayIndex = (routine.lastCompletedDayIndex + 1) % routine.workoutDays.size
val nextDay = routine.workoutDays[nextDayIndex]
activeWorkoutViewModel.startWorkout(
    day = nextDay,
    dayIndex = nextDayIndex,
    routineName = routine.name,
    routineId = routine.id
)
onStartWorkout()  // triggers navigation to WorkoutScreen
```

- [ ] **Step 3: Update `MainActivity.kt`**

Replace the old `WorkoutScreen(...)` call block and minimized banner logic:

```kotlin
val activeWorkoutViewModel: ActiveWorkoutViewModel = hiltViewModel(
    viewModelStoreOwner = LocalContext.current as ComponentActivity
)
val workoutUiState by activeWorkoutViewModel.uiState.collectAsStateWithLifecycle()
```

Remove `val activeWorkout by viewModel.activeWorkout.collectAsState()` and `var isWorkoutMinimized`.

Replace banner condition: `if (workout != null && isWorkoutMinimized)` → `if (workoutUiState.isActive && !workoutUiState.isFullScreen)`

Replace `WorkoutScreen(workoutDay = ..., exerciseStates = ...)` with `WorkoutScreen(modifier = contentModifier)`.

Replace the condition `if (workout != null && !isWorkoutMinimized)` with `if (workoutUiState.isActive && workoutUiState.isFullScreen)`.

Update banner content to use `workoutUiState.workoutDayName`.

Update minimized banner click / Resume button: call `activeWorkoutViewModel.setFullScreen(true)` (the `isWorkoutMinimized = false` equivalent).

Update `HomeScreen` call in MainActivity to:
```kotlin
HomeScreen(
    onNavigateToSettings = { isSettingsVisible = true },
    onStartWorkout = { /* isFullScreen is already set to true by ActiveWorkoutViewModel */ },
    modifier = contentModifier
)
```

Remove now-unused `val exercises`, `val equipment`, `val routines` from `WorkoutPlannerApp` if nothing else references them.

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. This task has many moving parts — fix any compile errors before committing.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/WorkoutScreen.kt \
        app/src/main/java/com/example/workoutplanner/ui/HomeScreen.kt \
        app/src/main/java/com/example/workoutplanner/MainActivity.kt
git commit -m "refactor: WorkoutScreen uses ActiveWorkoutViewModel with immutable state"
```

---

## Task 14: Create NavRoutes + WorkoutNavGraph

**Files:**
- Create: `app/src/main/java/com/example/workoutplanner/ui/navigation/NavRoutes.kt`
- Create: `app/src/main/java/com/example/workoutplanner/ui/navigation/WorkoutNavGraph.kt`

### Goal
Define all routes and the NavHost composable. Does not change any screen or activity yet — just creates the files.

- [ ] **Step 1: Create `ui/navigation/NavRoutes.kt`**

```kotlin
package com.example.workoutplanner.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object HistoryRoute
// SettingsGraphRoute is the nested graph key; SettingsRoute is the first destination inside it
@Serializable object SettingsGraphRoute
@Serializable object SettingsRoute
@Serializable object ExercisesRoute
@Serializable object EquipmentRoute
@Serializable object RoutinesRoute
@Serializable data class RoutineDetailRoute(val routineId: String)
@Serializable data class CreateRoutineRoute(val routineId: String? = null)
@Serializable object ActiveWorkoutRoute
```

- [ ] **Step 2: Create `ui/navigation/WorkoutNavGraph.kt`**

`activeWorkoutViewModel` is passed in from `WorkoutPlannerApp` (it is Activity-scoped). A `DisposableEffect` on `ActiveWorkoutRoute` keeps `isFullScreen` in sync.

```kotlin
package com.example.workoutplanner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.example.workoutplanner.ui.ActiveWorkoutViewModel
import com.example.workoutplanner.ui.CreateRoutineScreen
import com.example.workoutplanner.ui.EquipmentScreen
import com.example.workoutplanner.ui.ExercisesScreen
import com.example.workoutplanner.ui.HistoryScreen
import com.example.workoutplanner.ui.HomeScreen
import com.example.workoutplanner.ui.RoutineDetailScreen
import com.example.workoutplanner.ui.RoutinesScreen
import com.example.workoutplanner.ui.SettingsScreen
import com.example.workoutplanner.ui.WorkoutScreen

@Composable
fun WorkoutNavGraph(
    navController: NavHostController,
    activeWorkoutViewModel: ActiveWorkoutViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onNavigateToSettings = { navController.navigate(SettingsGraphRoute) },
                onStartWorkout = { navController.navigate(ActiveWorkoutRoute) }
            )
        }

        composable<HistoryRoute> {
            HistoryScreen()
        }

        composable<ActiveWorkoutRoute> {
            // Keep isFullScreen in sync with whether this destination is on screen
            DisposableEffect(Unit) {
                activeWorkoutViewModel.setFullScreen(true)
                onDispose { activeWorkoutViewModel.setFullScreen(false) }
            }
            WorkoutScreen()
        }

        // Settings nested graph: SettingsGraphRoute is the graph key, SettingsRoute is start
        navigation<SettingsGraphRoute>(startDestination = SettingsRoute) {
            composable<SettingsRoute> {
                SettingsScreen(
                    onNavigateToExercises = { navController.navigate(ExercisesRoute) },
                    onNavigateToRoutines = { navController.navigate(RoutinesRoute) },
                    onNavigateToEquipment = { navController.navigate(EquipmentRoute) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<ExercisesRoute> {
                ExercisesScreen(onBack = { navController.popBackStack() })
            }
            composable<EquipmentRoute> {
                EquipmentScreen(onBack = { navController.popBackStack() })
            }
            composable<RoutinesRoute> {
                RoutinesScreen(
                    onRoutineClick = { routineId ->
                        navController.navigate(RoutineDetailRoute(routineId))
                    },
                    onCreateRoutineClick = { navController.navigate(CreateRoutineRoute()) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<RoutineDetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<RoutineDetailRoute>()
                RoutineDetailScreen(
                    routineId = route.routineId,
                    onBackClick = { navController.popBackStack() },
                    onEditClick = { navController.navigate(CreateRoutineRoute(route.routineId)) }
                )
            }
            composable<CreateRoutineRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<CreateRoutineRoute>()
                CreateRoutineScreen(
                    routineId = route.routineId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
```

> **Note on Settings tab navigation:** The bottom nav Settings tab should navigate to `SettingsGraphRoute`, not `SettingsRoute`. This takes the user to the settings nested graph (which starts at `SettingsRoute` automatically). Update the `WorkoutPlannerApp` bottom nav accordingly (see Task 15).

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/navigation/
git commit -m "feat: add NavRoutes and WorkoutNavGraph"
```

---

## Task 15: Rewrite MainActivity with NavHost

**Files:**
- Modify: `app/src/main/java/com/example/workoutplanner/MainActivity.kt`

### Goal
Replace all manual `mutableStateOf` navigation state in `WorkoutPlannerApp` with a `NavController` and `WorkoutNavGraph`. Keep the minimized workout banner above the `NavHost`. Delete `AppDestinations` and `SettingsDestinations` enums.

- [ ] **Step 1: Rewrite `WorkoutPlannerApp`**

```kotlin
@Composable
fun WorkoutPlannerApp() {
    val navController = rememberNavController()
    val activeWorkoutViewModel: ActiveWorkoutViewModel = hiltViewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
    val workoutUiState by activeWorkoutViewModel.uiState.collectAsStateWithLifecycle()

    // Track the current route to highlight the correct bottom nav item
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            // Home tab
            item(
                icon = { Icon(painterResource(R.drawable.ic_home), contentDescription = "Home") },
                label = { Text("Home") },
                selected = currentDestination?.hasRoute<HomeRoute>() == true,
                onClick = {
                    navController.navigate(HomeRoute) {
                        popUpTo(HomeRoute) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
            // History tab
            item(
                icon = { Icon(painterResource(R.drawable.ic_history), contentDescription = "History") },
                label = { Text("History") },
                selected = currentDestination?.hasRoute<HistoryRoute>() == true,
                onClick = {
                    navController.navigate(HistoryRoute) {
                        popUpTo(HomeRoute) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
            // Settings tab — navigate to SettingsGraphRoute (the nested graph), not SettingsRoute directly
            item(
                icon = { Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings") },
                label = { Text("Settings") },
                selected = currentDestination?.hasRoute<SettingsRoute>() == true
                    || currentDestination?.hasRoute<SettingsGraphRoute>() == true,
                onClick = {
                    navController.navigate(SettingsGraphRoute) {
                        popUpTo(HomeRoute) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                // Minimized workout banner
                if (workoutUiState.isActive && !workoutUiState.isFullScreen) {
                    Surface(
                        onClick = {
                            activeWorkoutViewModel.setFullScreen(true)
                            navController.navigate(ActiveWorkoutRoute)
                        },
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth().height(64.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Active Workout", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        text = workoutUiState.workoutDayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            TextButton(onClick = {
                                activeWorkoutViewModel.setFullScreen(true)
                                navController.navigate(ActiveWorkoutRoute)
                            }) {
                                Text("Resume")
                            }
                        }
                    }
                }
            }
        ) { scaffoldPadding ->
            WorkoutNavGraph(
                navController = navController,
                activeWorkoutViewModel = activeWorkoutViewModel,
                modifier = Modifier.padding(scaffoldPadding)
            )
        }
    }
}
```

**Settings icon:** Check `res/drawable/` for an existing settings icon. Run: `ls app/src/main/res/drawable/`. If `ic_settings.xml` does not exist, use `Icons.Default.Settings` instead (Material Icons Extended is already in deps) — or copy/create a vector drawable.

Add required imports:
```kotlin
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavDestination.Companion.hasRoute
import com.example.workoutplanner.ui.navigation.*
import com.example.workoutplanner.ui.ActiveWorkoutViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
```

Delete the `AppDestinations` and `SettingsDestinations` enums from `MainActivity.kt` — they are no longer needed.

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. Fix any compile errors (missing icon, import conflicts, etc.) before moving on.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/MainActivity.kt
git commit -m "feat: replace manual navigation state with Navigation Compose NavHost"
```

---

## Task 16: Delete WorkoutViewModel + final cleanup

**Files:**
- Delete: `app/src/main/java/com/example/workoutplanner/WorkoutViewModel.kt`
- Verify: All screens use `collectAsStateWithLifecycle` (not `collectAsState`)

### Goal
Remove the now-unused `WorkoutViewModel`, `ActiveWorkout`, and the duplicate `toDomain()` extension functions. Verify the build is clean.

- [ ] **Step 1: Delete `WorkoutViewModel.kt`**

```bash
rm app/src/main/java/com/example/workoutplanner/WorkoutViewModel.kt
```

- [ ] **Step 2: Search for any remaining `collectAsState()` calls and replace with `collectAsStateWithLifecycle()`**

Run: `grep -r "collectAsState()" app/src/main/java/`

For each occurrence found, replace `collectAsState()` with `collectAsStateWithLifecycle()` and ensure the import `androidx.lifecycle.compose.collectAsStateWithLifecycle` is present in that file.

- [ ] **Step 3: Search for any remaining references to `WorkoutViewModel`**

Run: `grep -r "WorkoutViewModel" app/src/main/java/`

Fix any remaining references.

- [ ] **Step 4: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Final build with lint warnings check**

Run: `./gradlew assembleDebug --warning-mode all 2>&1 | head -50`

Review any warnings. Fix deprecation warnings if trivial.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor: delete WorkoutViewModel; all screens now use focused ViewModels"
```

---

## Post-Implementation Verification

- [ ] Install and launch on device/emulator: `./gradlew installDebug`
- [ ] Verify Home screen shows next workout day
- [ ] Verify starting a workout opens WorkoutScreen
- [ ] Verify minimizing workout shows the banner and Resume works
- [ ] Verify finishing a workout saves to history
- [ ] Verify History screen shows past workouts
- [ ] Verify Settings → Exercises/Equipment/Routines navigation works
- [ ] Verify creating and editing a routine works
- [ ] Verify back navigation throughout the app