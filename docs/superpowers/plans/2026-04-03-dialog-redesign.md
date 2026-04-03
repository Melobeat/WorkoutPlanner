# Dialog Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the three workout dialogs (Swap Workout, Swap Exercise, Add Exercise) to follow the design guidelines — `ModalBottomSheet` with search for exercise selection, `ListItem` rows throughout, correct M3 shape tokens, and gym-friendly touch targets.

**Architecture:** `ExerciseSelectionDialog` is rewritten as a `ModalBottomSheet` with a pinned `OutlinedTextField` search bar and `ListItem` rows. `WorkoutDayChooserDialog` keeps `AlertDialog` but replaces manual `Card`+`Column` rows with `ListItem` inside `Surface(onClick)`. A pure `filterExercises` function is extracted and unit-tested.

**Tech Stack:** Kotlin, Jetpack Compose, Material3 (`ModalBottomSheet`, `ListItem`, `OutlinedTextField`), JUnit 4

---

## Files

| File | Action |
|---|---|
| `app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseSelectionDialog.kt` | Full rewrite |
| `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutScreen.kt` | Add `title` arg at both call sites |
| `app/src/main/java/de/melobeat/workoutplanner/ui/HomeScreen.kt` | Rewrite `WorkoutDayChooserDialog` |
| `app/src/test/java/de/melobeat/workoutplanner/ExerciseFilterTest.kt` | Create — unit tests for filter function |

---

### Task 1: Extract and test `filterExercises`

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseSelectionDialog.kt`
- Create: `app/src/test/java/de/melobeat/workoutplanner/ExerciseFilterTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/de/melobeat/workoutplanner/ExerciseFilterTest.kt`:

```kotlin
package de.melobeat.workoutplanner

import de.melobeat.workoutplanner.model.Exercise
import de.melobeat.workoutplanner.ui.filterExercises
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseFilterTest {

    private val exercises = listOf(
        Exercise(id = "1", name = "Bench Press", muscleGroup = "Chest"),
        Exercise(id = "2", name = "Squat", muscleGroup = "Legs"),
        Exercise(id = "3", name = "Overhead Press", muscleGroup = "Shoulders"),
        Exercise(id = "4", name = "Leg Press", muscleGroup = "Legs")
    )

    @Test
    fun `blank query returns all exercises`() {
        assertEquals(exercises, filterExercises(exercises, ""))
    }

    @Test
    fun `whitespace only query returns all exercises`() {
        assertEquals(exercises, filterExercises(exercises, "   "))
    }

    @Test
    fun `query matches exercise name case insensitively`() {
        val result = filterExercises(exercises, "bench")
        assertEquals(listOf(exercises[0]), result)
    }

    @Test
    fun `query matches muscle group case insensitively`() {
        val result = filterExercises(exercises, "legs")
        assertEquals(listOf(exercises[1], exercises[3]), result)
    }

    @Test
    fun `query with no matches returns empty list`() {
        val result = filterExercises(exercises, "zzz")
        assertEquals(emptyList<Exercise>(), result)
    }

    @Test
    fun `query matches partial name`() {
        val result = filterExercises(exercises, "press")
        assertEquals(listOf(exercises[0], exercises[2], exercises[3]), result)
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew app:testDebugUnitTest --tests "de.melobeat.workoutplanner.ExerciseFilterTest" 2>&1 | tail -20
```

Expected: compilation error — `filterExercises` does not exist yet.

- [ ] **Step 3: Add `filterExercises` to `ExerciseSelectionDialog.kt`**

Replace the entire contents of `ExerciseSelectionDialog.kt` with just the package declaration and the filter function for now (the composable will be replaced in Task 2, but keep the old composable intact so the build still compiles):

```kotlin
package de.melobeat.workoutplanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.melobeat.workoutplanner.model.Exercise

internal fun filterExercises(exercises: List<Exercise>, query: String): List<Exercise> {
    if (query.isBlank()) return exercises
    val lower = query.lowercase()
    return exercises.filter {
        it.name.lowercase().contains(lower) || it.muscleGroup.lowercase().contains(lower)
    }
}

@Composable
fun ExerciseSelectionDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onExerciseSelected: (Exercise) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select Exercise",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(exercises) { _, exercise ->
                        Surface(
                            onClick = { onExerciseSelected(exercise) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = exercise.muscleGroup,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 16.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew app:testDebugUnitTest --tests "de.melobeat.workoutplanner.ExerciseFilterTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, 6 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseSelectionDialog.kt \
        app/src/test/java/de/melobeat/workoutplanner/ExerciseFilterTest.kt
git commit -m "feat: extract and test filterExercises function"
```

---

### Task 2: Rewrite `ExerciseSelectionDialog` as `ModalBottomSheet`

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseSelectionDialog.kt`

- [ ] **Step 1: Replace the composable**

Replace the full contents of `ExerciseSelectionDialog.kt` with:

```kotlin
package de.melobeat.workoutplanner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.melobeat.workoutplanner.model.Exercise

internal fun filterExercises(exercises: List<Exercise>, query: String): List<Exercise> {
    if (query.isBlank()) return exercises
    val lower = query.lowercase()
    return exercises.filter {
        it.name.lowercase().contains(lower) || it.muscleGroup.lowercase().contains(lower)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSelectionDialog(
    title: String,
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onExerciseSelected: (Exercise) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val filtered = remember(exercises, query) { filterExercises(exercises, query) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(filtered, key = { it.id }) { exercise ->
                    ListItem(
                        headlineContent = {
                            Text(
                                exercise.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        supportingContent = {
                            Text(
                                exercise.muscleGroup,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        Icons.Rounded.FitnessCenter,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onExerciseSelected(exercise) }
                    )
                }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}
```

- [ ] **Step 2: Build to confirm no errors**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew app:assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`. If you see a compilation error about `ExerciseSelectionDialog` missing a `title` parameter, that is expected — fix it in Task 3 first.

_Note: the build will fail here because `WorkoutScreen.kt` still calls the old signature. Proceed immediately to Task 3._

- [ ] **Step 3: Commit** _(after Task 3 fixes compile errors)_

Wait until after Task 3 before committing — the project won't compile until the call sites are updated.

---

### Task 3: Update call sites in `WorkoutScreen.kt`

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutScreen.kt`

- [ ] **Step 1: Add `title` to the Add Exercise call site**

In `WorkoutScreen.kt`, find the `if (showAddExerciseDialog)` block (around line 244) and replace it:

```kotlin
if (showAddExerciseDialog) {
    ExerciseSelectionDialog(
        title = "Add Exercise",
        exercises = availableExercises,
        onDismiss = { showAddExerciseDialog = false },
        onExerciseSelected = { exercise ->
            onAddExercise(exercise)
            showAddExerciseDialog = false
        }
    )
}
```

- [ ] **Step 2: Add `title` to the Swap Exercise call site**

Find the `if (showSwapDialog)` block (around line 255) and replace it:

```kotlin
if (showSwapDialog) {
    ExerciseSelectionDialog(
        title = "Swap Exercise",
        exercises = availableExercises,
        onDismiss = { showSwapDialog = false },
        onExerciseSelected = { exercise ->
            onSwapExercise(ei, exercise)
            showSwapDialog = false
        }
    )
}
```

- [ ] **Step 3: Build to confirm no errors**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew app:assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Run all unit tests to confirm nothing broke**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew app:testDebugUnitTest 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseSelectionDialog.kt \
        app/src/main/java/de/melobeat/workoutplanner/ui/WorkoutScreen.kt
git commit -m "feat: redesign ExerciseSelectionDialog as ModalBottomSheet with search"
```

---

### Task 4: Rewrite `WorkoutDayChooserDialog`

**Files:**
- Modify: `app/src/main/java/de/melobeat/workoutplanner/ui/HomeScreen.kt`

- [ ] **Step 1: Add missing imports to `HomeScreen.kt`**

Add these imports to the import block in `HomeScreen.kt` (keep all existing imports):

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
```

- [ ] **Step 2: Replace `WorkoutDayChooserDialog`**

Find the `WorkoutDayChooserDialog` composable (starting around line 312) and replace it entirely with:

```kotlin
@Composable
fun WorkoutDayChooserDialog(
    workoutDays: List<WorkoutDay>,
    onDaySelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Next Workout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                workoutDays.forEachIndexed { index, day ->
                    Surface(
                        onClick = { onDaySelected(index) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    "Day ${index + 1}: ${day.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            supportingContent = {
                                Text(
                                    day.exercises.joinToString(", ") { it.name },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingContent = {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        confirmButton = {}
    )
}
```

- [ ] **Step 3: Build to confirm no errors**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew app:assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`. If you see unused import warnings for `Card` or `CardDefaults`, remove those imports from `HomeScreen.kt`.

- [ ] **Step 4: Run all unit tests**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew app:testDebugUnitTest 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/melobeat/workoutplanner/ui/HomeScreen.kt
git commit -m "feat: redesign WorkoutDayChooserDialog with ListItem rows and correct shape tokens"
```