# M3 Expressive Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign every app screen with Material 3 Expressive — spring-physics motion, gradient hero surfaces, pill shapes, and large touch targets — without changing the data layer or architecture.

**Architecture:** Pure UI layer changes. `ActiveWorkoutViewModel` gains `currentExerciseIndex` / `currentSetIndex` navigation state and stepper/progression methods. Every other ViewModel and the data layer is untouched. Screens are updated independently on the shared updated theme.

**Tech Stack:** Compose BOM 2026.03.01, Material3 1.4.0 (`ExperimentalMaterial3ExpressiveApi`), `material-icons-extended`, existing Hilt/Room/Navigation setup. No new dependencies required.

---

## File Map

| Action | File | What changes |
|---|---|---|
| Modify | `ui/theme/Theme.kt` | Add `MotionScheme.expressive()`, update fallback color schemes |
| Modify | `ui/theme/Color.kt` | Update seed palette to deep purple + pink |
| Modify | `ui/ActiveWorkoutViewModel.kt` | Add cursor state fields; add stepper + `completeCurrentSet()` methods |
| Rewrite | `ui/WorkoutScreen.kt` | One-set-at-a-time layout with stepper cards |
| Rewrite | `ui/HomeScreen.kt` | Full-width gradient hero, no TopAppBar |
| Modify | `MainActivity.kt` | Updated nav icons + active workout mini-bar redesign |
| Rewrite | `ui/HistoryScreen.kt` | `LargeTopAppBar`, expandable date-grouped cards |
| Modify | `ui/SettingsScreen.kt` | `LargeTopAppBar`, `ListItem` composables |
| Modify | `ui/RoutinesScreen.kt` | `surfaceVariant` cards, `ExtendedFloatingActionButton` |
| Modify | `ui/RoutineDetailScreen.kt` | `LargeTopAppBar`, `surfaceVariant` day cards |
| Modify | `ui/CreateRoutineScreen.kt` | `OutlinedCard` day containers, pill `FilledButton` |
| Modify | `ui/ExercisesScreen.kt` | `LargeTopAppBar`, `ExtendedFloatingActionButton`, `ListItem` rows |
| Modify | `ui/EquipmentScreen.kt` | `LargeTopAppBar`, `ExtendedFloatingActionButton`, `ListItem` rows |

---

### Task 1: Update Theme and Colors

**Files:**
- Modify: `app/src/main/java/com/example/workoutplanner/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/example/workoutplanner/ui/theme/Color.kt`

- [ ] **Step 1: Replace Color.kt**

```kotlin
package com.example.workoutplanner.ui.theme

import androidx.compose.ui.graphics.Color

val Purple10  = Color(0xFF21005D)
val Purple40  = Color(0xFF6750A4)
val Purple80  = Color(0xFFD0BCFF)
val Purple90  = Color(0xFFEADDFF)

val Pink40    = Color(0xFFB5488A)
val Pink80    = Color(0xFFFFB0C8)
val Pink90    = Color(0xFFFFD8E4)

val Neutral10 = Color(0xFF1C1B1F)
val Neutral90 = Color(0xFFE6E1E5)
```

- [ ] **Step 2: Replace Theme.kt**

```kotlin
package com.example.workoutplanner.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = Pink80,
    tertiary = Purple90
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = Pink40,
    tertiary = Purple90
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WorkoutPlannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}
```

- [ ] **Step 3: Build and verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL` with 0 errors.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/theme/Theme.kt \
        app/src/main/java/com/example/workoutplanner/ui/theme/Color.kt
git commit -m "feat: add MotionScheme.expressive() and update seed palette"
```

---

### Task 2: Update ActiveWorkoutViewModel

**Files:**
- Modify: `app/src/main/java/com/example/workoutplanner/ui/ActiveWorkoutViewModel.kt`

The new workout screen navigates one set at a time. The ViewModel tracks which exercise and which set the user is currently on, and exposes stepper methods and `completeCurrentSet()`.

- [ ] **Step 1: Add `currentExerciseIndex` and `currentSetIndex` to `ActiveWorkoutUiState`**

In `ActiveWorkoutViewModel.kt`, update the `ActiveWorkoutUiState` data class:

```kotlin
data class ActiveWorkoutUiState(
    val isActive: Boolean = false,
    val isFullScreen: Boolean = false,
    val workoutDayName: String = "",
    val exercises: List<ExerciseUiState> = emptyList(),
    val elapsedTime: Long = 0L,
    val isFinished: Boolean = false,
    val error: String? = null,
    val currentExerciseIndex: Int = 0,
    val currentSetIndex: Int = 0,
)
```

- [ ] **Step 2: Add stepper and navigation methods to `ActiveWorkoutViewModel`**

Add the following methods inside `ActiveWorkoutViewModel`, after `clearError()`:

```kotlin
fun incrementReps(exerciseIndex: Int, setIndex: Int) {
    val current = _uiState.value.exercises
        .getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex)
        ?.reps?.toIntOrNull() ?: 0
    setRepsValue(exerciseIndex, setIndex, (current + 1).toString())
}

fun decrementReps(exerciseIndex: Int, setIndex: Int) {
    val current = _uiState.value.exercises
        .getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex)
        ?.reps?.toIntOrNull() ?: 0
    if (current > 0) setRepsValue(exerciseIndex, setIndex, (current - 1).toString())
}

fun incrementWeight(exerciseIndex: Int, setIndex: Int) {
    val current = _uiState.value.exercises
        .getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex)
        ?.weight?.toDoubleOrNull() ?: 0.0
    setWeightValue(exerciseIndex, setIndex, formatWeight(current + 2.5))
}

fun decrementWeight(exerciseIndex: Int, setIndex: Int) {
    val current = _uiState.value.exercises
        .getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex)
        ?.weight?.toDoubleOrNull() ?: 0.0
    if (current >= 2.5) setWeightValue(exerciseIndex, setIndex, formatWeight(current - 2.5))
}

fun completeCurrentSet() {
    val state = _uiState.value
    val ei = state.currentExerciseIndex
    val si = state.currentSetIndex
    // mark done
    _uiState.update { s ->
        s.copy(exercises = s.exercises.mapIndexed { eIdx, ex ->
            if (eIdx != ei) ex
            else ex.copy(sets = ex.sets.mapIndexed { sIdx, set ->
                if (sIdx == si) set.copy(isDone = true) else set
            })
        })
    }
    val exercise = _uiState.value.exercises[ei]
    when {
        si < exercise.sets.size - 1 ->
            _uiState.update { it.copy(currentSetIndex = si + 1) }
        ei < _uiState.value.exercises.size - 1 ->
            _uiState.update { it.copy(currentExerciseIndex = ei + 1, currentSetIndex = 0) }
        else -> finishWorkout()
    }
}

// Value-only updates (do not flip isDone) used by steppers
private fun setRepsValue(exerciseIndex: Int, setIndex: Int, reps: String) {
    _uiState.update { state ->
        state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
            if (ei != exerciseIndex) return@mapIndexed ex
            ex.copy(sets = ex.sets.mapIndexed { si, set ->
                if (si == setIndex) set.copy(reps = reps, originalReps = reps) else set
            })
        })
    }
}

private fun setWeightValue(exerciseIndex: Int, setIndex: Int, weight: String) {
    _uiState.update { state ->
        state.copy(exercises = state.exercises.mapIndexed { ei, ex ->
            if (ei != exerciseIndex) return@mapIndexed ex
            ex.copy(sets = ex.sets.mapIndexed { si, set ->
                if (si == setIndex) set.copy(weight = weight) else set
            })
        })
    }
}
```

Also reset `currentExerciseIndex` and `currentSetIndex` to 0 in `startWorkout()` inside `_uiState.update { it.copy(...) }`:

```kotlin
_uiState.update {
    it.copy(
        isActive = true,
        isFullScreen = true,
        workoutDayName = day.name,
        exercises = exerciseStates,
        isFinished = false,
        error = null,
        currentExerciseIndex = 0,
        currentSetIndex = 0,
    )
}
```

- [ ] **Step 3: Build and verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/ActiveWorkoutViewModel.kt
git commit -m "feat: add cursor state and stepper methods to ActiveWorkoutViewModel"
```

---

### Task 3: Rewrite WorkoutScreen

**Files:**
- Rewrite: `app/src/main/java/com/example/workoutplanner/ui/WorkoutScreen.kt`

Replace the all-exercises-at-once list with a focused one-set-at-a-time layout. The `ExerciseCard` and `RepsDialog` composables are removed. The `formatElapsedTime` function is kept.

- [ ] **Step 1: Replace WorkoutScreen.kt**

```kotlin
package com.example.workoutplanner.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workoutplanner.ui.theme.Pink40
import com.example.workoutplanner.ui.theme.Purple40
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveWorkoutViewModel = viewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    ),
    exerciseLibraryViewModel: ExerciseLibraryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exerciseLibState by exerciseLibraryViewModel.uiState.collectAsStateWithLifecycle()
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) onNavigateBack()
    }

    val exercises = uiState.exercises
    val ei = uiState.currentExerciseIndex.coerceIn(0, (exercises.size - 1).coerceAtLeast(0))
    val currentExercise = exercises.getOrNull(ei)
    val si = uiState.currentSetIndex.coerceIn(
        0, ((currentExercise?.sets?.size ?: 1) - 1).coerceAtLeast(0)
    )
    val currentSet = currentExercise?.sets?.getOrNull(si)
    val nextExercise = exercises.getOrNull(ei + 1)
    val progress = if (exercises.isEmpty()) 0f
    else (ei.toFloat() + (si.toFloat() / (currentExercise?.sets?.size?.toFloat() ?: 1f))) / exercises.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.workoutDayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = formatElapsedTime(uiState.elapsedTime),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Minimize")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showAddExerciseDialog = true },
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Exercise", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = { showCancelDialog = true },
                        shape = CircleShape,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("End", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.width(8.dp))
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Progress bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXERCISE ${ei + 1} OF ${exercises.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().clip(CircleShape).height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            if (currentExercise != null && currentSet != null) {
                // Exercise header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentExercise.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    FilledTonalButton(
                        onClick = { /* swap handled via dialog */ },
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Swap", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Set dot indicators
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    currentExercise.sets.forEachIndexed { index, set ->
                        val width = if (index == si) 28.dp else 8.dp
                        Surface(
                            modifier = Modifier.height(8.dp).width(width).clip(CircleShape),
                            color = when {
                                set.isDone -> MaterialTheme.colorScheme.primary
                                index == si -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {}
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "SET ${si + 1} OF ${currentExercise.sets.size}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Stepper cards row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StepperCard(
                        label = "Reps",
                        value = currentSet.reps,
                        onIncrement = { viewModel.incrementReps(ei, si) },
                        onDecrement = { viewModel.decrementReps(ei, si) },
                        modifier = Modifier.weight(1f)
                    )
                    StepperCard(
                        label = "kg",
                        value = currentSet.weight,
                        onIncrement = { viewModel.incrementWeight(ei, si) },
                        onDecrement = { viewModel.decrementWeight(ei, si) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // AMRAP toggle — shown only on last set
                if (si == currentExercise.sets.size - 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Switch(
                            checked = currentSet.isAmrap,
                            onCheckedChange = { /* isAmrap is from routine definition — read-only in active workout */ }
                        )
                        Text("Last set AMRAP", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Done CTA
                val isLastSet = si == currentExercise.sets.size - 1
                val isLastExercise = ei == exercises.size - 1
                val ctaLabel = when {
                    isLastSet && isLastExercise -> "✓  Finish Workout"
                    isLastSet -> "✓  Next Exercise"
                    else -> "✓  Done — Set ${si + 2}"
                }
                Button(
                    onClick = { viewModel.completeCurrentSet() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    // Gradient background via Box behind
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(50))
                            .then(
                                Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(ctaLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                // Re-implement as Surface with gradient since Button doesn't support brush
                // Replace the Button above with this:
                Surface(
                    onClick = { viewModel.completeCurrentSet() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(50))
                            .then(
                                Modifier.run {
                                    background(
                                        Brush.linearGradient(listOf(Purple40, Pink40))
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            ctaLabel,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Completed sets chips
                val doneSets = currentExercise.sets.filter { it.isDone }
                AnimatedVisibility(
                    visible = doneSets.isNotEmpty(),
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        currentExercise.sets.forEachIndexed { index, set ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (set.isDone) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                            ) {
                                val label = if (set.isDone) "Set ${index + 1}\n${set.reps}×${set.weight}" else "Set ${index + 1}\n—"
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    color = if (set.isDone) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Next exercise preview
                if (nextExercise != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "NEXT EXERCISE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    nextExercise.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${nextExercise.sets.size} sets · ${nextExercise.sets.firstOrNull()?.reps ?: "0"} reps · ${nextExercise.sets.firstOrNull()?.weight ?: "0"} kg",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No exercises in this workout.")
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        ExerciseSelectionDialog(
            exercises = exerciseLibState.exercises,
            onDismiss = { showAddExerciseDialog = false },
            onExerciseSelected = { exercise ->
                viewModel.addExercise(exercise)
                showAddExerciseDialog = false
            }
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("End Workout?") },
            text = { Text("All progress in this session will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelWorkout()
                    onNavigateBack()
                }) {
                    Text("End Workout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Keep Going") }
            }
        )
    }
}

@Composable
fun StepperCard(
    label: String,
    value: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onDecrement,
                    shape = CircleShape,
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("−", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onIncrement,
                    shape = CircleShape,
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun formatElapsedTime(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
```

> **Note on gradient CTA:** The `Surface`+`Brush.linearGradient` block replaces the `Button` block above it. Remove the entire `Button { Box { ... } }` block, keeping only the `Surface { Box { Modifier.background(Brush...) } }` version. The redundant Button block is shown for context — the final file should only have the Surface implementation.

- [ ] **Step 2: Add missing import for `background` modifier in WorkoutScreen.kt**

Ensure `androidx.compose.ui.draw.background` is imported (it is already included via wildcard imports in most screens; if not, add it explicitly).

- [ ] **Step 3: Build and verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/WorkoutScreen.kt
git commit -m "feat: rewrite WorkoutScreen with one-set-at-a-time stepper layout"
```

---

### Task 4: Rewrite HomeScreen

**Files:**
- Rewrite: `app/src/main/java/com/example/workoutplanner/ui/HomeScreen.kt`

The `TopAppBar` is removed. The gradient hero bleeds edge-to-edge into the status bar using `WindowInsets.statusBars`. The "Start Workout" button is a white-background pill. History cards are `surfaceVariant` with a tonal duration chip.

- [ ] **Step 1: Replace HomeScreen.kt**

```kotlin
package com.example.workoutplanner.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workoutplanner.model.WorkoutDay
import com.example.workoutplanner.ui.theme.Pink40
import com.example.workoutplanner.ui.theme.Purple10
import com.example.workoutplanner.ui.theme.Purple40

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    activeWorkoutViewModel: ActiveWorkoutViewModel = viewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showWorkoutChooser by remember { mutableStateOf(false) }
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val heroBrush = Brush.linearGradient(listOf(Purple10, Purple40, Pink40))

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Hero banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .then(Modifier.run { this })
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .run {
                                background(heroBrush)
                            }
                            .padding(top = statusBarPadding + 16.dp, start = 20.dp, end = 20.dp, bottom = 28.dp)
                    ) {
                        // Title row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.FitnessCenter,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Workout Planner",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(
                                    Icons.Rounded.Tune,
                                    contentDescription = "Settings",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        if (uiState.selectedRoutine != null) {
                            val routine = uiState.selectedRoutine!!
                            val nextDayIndex = (routine.lastCompletedDayIndex + 1) % routine.workoutDays.size
                            val nextDay = routine.workoutDays[nextDayIndex]

                            // Routine label
                            Text(
                                text = "${routine.name.uppercase()} · DAY ${nextDayIndex + 1} OF ${routine.workoutDays.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = nextDay.name,
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                lineHeight = MaterialTheme.typography.displaySmall.lineHeight
                            )
                            Text(
                                text = "${nextDay.exercises.size} exercises",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.65f)
                            )

                            // Exercise chips
                            if (nextDay.exercises.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    nextDay.exercises.take(2).forEach { ex ->
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                ex.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                    if (nextDay.exercises.size > 2) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "+${nextDay.exercises.size - 2} more",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            // CTA buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        activeWorkoutViewModel.startWorkout(
                                            day = nextDay,
                                            dayIndex = nextDayIndex,
                                            routineName = routine.name,
                                            routineId = routine.id
                                        )
                                        onStartWorkout()
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Purple40
                                    )
                                ) {
                                    Text("▶ Start Workout", fontWeight = FontWeight.Bold)
                                }
                                FilledTonalButton(
                                    onClick = { showWorkoutChooser = true },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color.White.copy(alpha = 0.2f),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Rounded.SwapHoriz, contentDescription = "Swap Day")
                                }
                            }
                        } else {
                            // Empty state
                            Text(
                                "No Active Routine",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Select a routine to start tracking your progress.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onNavigateToSettings,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Purple40
                                )
                            ) {
                                Text("Manage Routines", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Recent workouts header
        if (uiState.recentHistory.isNotEmpty()) {
            item {
                Text(
                    "RECENT WORKOUTS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
                )
            }
            items(uiState.recentHistory.take(5), key = { it.workout.id }) { session ->
                WorkoutSessionCard(session, uiState.exerciseNameMap)
            }
        }
    }

    if (showWorkoutChooser && uiState.selectedRoutine != null) {
        WorkoutDayChooserDialog(
            workoutDays = uiState.selectedRoutine!!.workoutDays,
            onDaySelected = { index ->
                val routine = uiState.selectedRoutine ?: return@WorkoutDayChooserDialog
                val totalDays = routine.workoutDays.size
                val lastCompletedIndex = (index + totalDays - 1) % totalDays
                viewModel.updateNextDay(routine.id, lastCompletedIndex)
                showWorkoutChooser = false
            },
            onDismiss = { showWorkoutChooser = false }
        )
    }
}

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
                    Card(
                        onClick = { onDaySelected(index) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Day ${index + 1}: ${day.name}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            day.exercises.forEach { exercise ->
                                Text("• ${exercise.name}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
```

> **Note on `Modifier.background(brush)`:** `Column` and other layouts require importing `androidx.compose.foundation.background`. Add `import androidx.compose.ui.draw.background` if the wildcard import does not cover it. The `heroBrush` is applied via `Modifier.background(heroBrush)` on the Column.

- [ ] **Step 2: Build and verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/HomeScreen.kt
git commit -m "feat: rewrite HomeScreen with edge-to-edge gradient hero"
```

---

### Task 5: Update MainActivity (icons + mini-bar)

**Files:**
- Modify: `app/src/main/java/com/example/workoutplanner/MainActivity.kt`

Two changes: nav icons → `Icons.Rounded.*`; active workout mini-bar → `surfaceVariant` with `FitnessCenter` icon and `FilledTonalButton`.

- [ ] **Step 1: Replace import of icon drawables with Material Icons Rounded**

In `MainActivity.kt`, the two nav items currently use `painterResource(R.drawable.ic_home)` and `painterResource(R.drawable.ic_history)`. Replace them with vector icons.

Update the imports to include:
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.shape.CircleShape
```

- [ ] **Step 2: Update NavigationSuiteScaffold items and mini-bar in `WorkoutPlannerApp`**

Replace the full `WorkoutPlannerApp` composable body:

```kotlin
@Composable
fun WorkoutPlannerApp() {
    val navController = rememberNavController()
    val activeWorkoutViewModel: ActiveWorkoutViewModel = viewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )
    val workoutUiState by activeWorkoutViewModel.uiState.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            item(
                icon = { Icon(Icons.Rounded.FitnessCenter, contentDescription = "Home") },
                label = { Text("Home") },
                selected = currentDestination?.hasRoute<HomeRoute>() == true,
                onClick = {
                    navController.navigate(HomeRoute) {
                        popUpTo(HomeRoute) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
            item(
                icon = { Icon(Icons.Rounded.History, contentDescription = "History") },
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
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (workoutUiState.isActive && !workoutUiState.isFullScreen) {
                    Surface(
                        onClick = {
                            activeWorkoutViewModel.setFullScreen(true)
                            navController.navigate(ActiveWorkoutRoute)
                        },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        formatElapsedTime(workoutUiState.elapsedTime),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        workoutUiState.workoutDayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = {
                                    activeWorkoutViewModel.setFullScreen(true)
                                    navController.navigate(ActiveWorkoutRoute)
                                },
                                shape = CircleShape
                            ) {
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

Remove the `import androidx.compose.ui.res.painterResource` line (no longer needed).

- [ ] **Step 3: Build and verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/MainActivity.kt
git commit -m "feat: update nav icons to Rounded and redesign active workout mini-bar"
```

---

### Task 6: Rewrite HistoryScreen

**Files:**
- Rewrite: `app/src/main/java/com/example/workoutplanner/ui/HistoryScreen.kt`

`LargeTopAppBar` with scroll collapse, expandable date-grouped session cards, `SuggestionChip` duration display.

- [ ] **Step 1: Replace HistoryScreen.kt**

```kotlin
package com.example.workoutplanner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workoutplanner.data.WorkoutHistoryWithExercises
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    // Track expanded state per session id
    val expandedIds = remember { mutableStateMapOf<Long, Boolean>() }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("History", fontWeight = FontWeight.Black) },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.sessions.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.padding(bottom = 12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "No workouts yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Complete a workout to see it here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                // Group sessions by date label
                val grouped = uiState.sessions.groupBy { getDateGroupLabel(it.workout.date) }
                // Preserve order: This Week, Last Week, then months descending
                val orderedKeys = grouped.keys.sortedWith(Comparator { a, b ->
                    val order = listOf("This Week", "Last Week")
                    val ai = order.indexOf(a)
                    val bi = order.indexOf(b)
                    when {
                        ai != -1 && bi != -1 -> ai - bi
                        ai != -1 -> -1
                        bi != -1 -> 1
                        else -> b.compareTo(a) // months descending
                    }
                })

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    orderedKeys.forEach { groupLabel ->
                        item(key = "header_$groupLabel") {
                            Text(
                                groupLabel.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(
                            grouped[groupLabel] ?: emptyList(),
                            key = { it.workout.id }
                        ) { session ->
                            val isExpanded = expandedIds[session.workout.id] ?: false
                            HistorySessionCard(
                                session = session,
                                exerciseNameMap = uiState.exerciseNameMap,
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    expandedIds[session.workout.id] = !isExpanded
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistorySessionCard(
    session: WorkoutHistoryWithExercises,
    exerciseNameMap: Map<String, String>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEE, MMM dd", Locale.getDefault()) }
    val dateString = dateFormat.format(Date(session.workout.date))
    val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(session.workout.durationMillis)
    val durationSeconds = TimeUnit.MILLISECONDS.toSeconds(session.workout.durationMillis) % 60
    val durationString = if (durationMinutes > 0) "${durationMinutes}m ${durationSeconds}s"
    else "${durationSeconds}s"

    Card(
        onClick = onToggleExpand,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        session.workout.workoutDayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "$dateString · ${session.workout.routineName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(durationString, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Rounded.Schedule, contentDescription = null) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                    Icon(
                        if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    val exerciseEntries = session.exercises.groupBy { it.exerciseId }
                    val displayEntries = exerciseEntries.entries.toList()
                    val visibleEntries = displayEntries.take(3)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            visibleEntries.forEach { (exerciseId, sets) ->
                                val name = exerciseNameMap[exerciseId] ?: "Unknown"
                                val summary = "${sets.size} × ${sets.firstOrNull()?.reps ?: 0}" +
                                    " · ${sets.firstOrNull()?.weight ?: 0} kg"
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (displayEntries.size > 3) {
                                Text(
                                    "+${displayEntries.size - 3} more exercises",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getDateGroupLabel(dateMs: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = dateMs }
    val now = Calendar.getInstance()
    val thisWeekStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val lastWeekStart = (thisWeekStart.clone() as Calendar).apply { add(Calendar.WEEK_OF_YEAR, -1) }
    return when {
        !cal.before(thisWeekStart) -> "This Week"
        !cal.before(lastWeekStart) -> "Last Week"
        else -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(dateMs))
    }
}
```

> **Note:** `WorkoutSessionCard` in `HistoryScreen.kt` is used by `HomeScreen.kt` for recent history cards. Keep `WorkoutSessionCard` in `HistoryScreen.kt` — the old implementation is unchanged and continues to serve `HomeScreen`.

- [ ] **Step 2: Build and verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/HistoryScreen.kt
git commit -m "feat: rewrite HistoryScreen with LargeTopAppBar and expandable date-grouped cards"
```

---

### Task 7: Update SettingsScreen

**Files:**
- Modify: `app/src/main/java/com/example/workoutplanner/ui/SettingsScreen.kt`

Replace manual Row items with `ListItem`, add `LargeTopAppBar`.

- [ ] **Step 1: Replace SettingsScreen.kt**

```kotlin
package com.example.workoutplanner.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToExercises: () -> Unit,
    onNavigateToRoutines: () -> Unit,
    onNavigateToEquipment: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        ) {
            item {
                SettingsListItem(
                    title = "Manage Exercises",
                    subtitle = "Add, edit or delete exercises",
                    icon = Icons.Default.FitnessCenter,
                    onClick = onNavigateToExercises
                )
                HorizontalDivider()
            }
            item {
                SettingsListItem(
                    title = "Manage Equipment",
                    subtitle = "Dumbbells, barbells, machines, etc.",
                    icon = Icons.Default.Construction,
                    onClick = onNavigateToEquipment
                )
                HorizontalDivider()
            }
            item {
                SettingsListItem(
                    title = "Manage Routines",
                    subtitle = "Create and organize your workout routines",
                    icon = Icons.AutoMirrored.Filled.ListAlt,
                    onClick = onNavigateToRoutines
                )
            }
        }
    }
}

@Composable
fun SettingsListItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp).then(Modifier /* padding handled by Surface */)
                )
            }
        },
        trailingContent = {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
```

- [ ] **Step 2: Build and verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/SettingsScreen.kt
git commit -m "feat: update SettingsScreen with LargeTopAppBar and ListItem"
```

---

### Task 8: Update RoutinesScreen

**Files:**
- Modify: `app/src/main/java/com/example/workoutplanner/ui/RoutinesScreen.kt`

`LargeTopAppBar` with scroll collapse, `surfaceVariant` routine cards, `ExtendedFloatingActionButton`.

- [ ] **Step 1: Replace RoutinesScreen.kt**

```kotlin
package com.example.workoutplanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workoutplanner.model.Routine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    onRoutineClick: (routineId: String) -> Unit,
    onCreateRoutineClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutinesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var routineToDelete by remember { mutableStateOf<Routine?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Routines", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateRoutineClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Routine") }
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
        ) {
            items(uiState.routines, key = { it.id }) { routine ->
                val isActive = routine.isSelected
                Card(
                    onClick = { onRoutineClick(routine.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                routine.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${routine.workoutDays.size} days",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                if (!isActive) viewModel.selectRoutine(routine.id)
                            }) {
                                Icon(
                                    if (isActive) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = if (isActive) "Active" else "Set Active",
                                    tint = if (isActive) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { routineToDelete = routine }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    routineToDelete?.let { routine ->
        AlertDialog(
            onDismissRequest = { routineToDelete = null },
            title = { Text("Delete Routine?") },
            text = { Text("\"${routine.name}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRoutine(routine.id)
                    routineToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { routineToDelete = null }) { Text("Cancel") }
            }
        )
    }
}
```

- [ ] **Step 2: Build and verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/RoutinesScreen.kt
git commit -m "feat: update RoutinesScreen with LargeTopAppBar and surfaceVariant cards"
```

---

### Task 9: Update RoutineDetailScreen and CreateRoutineScreen

**Files:**
- Modify: `app/src/main/java/com/example/workoutplanner/ui/RoutineDetailScreen.kt`
- Modify: `app/src/main/java/com/example/workoutplanner/ui/CreateRoutineScreen.kt`

Both get `LargeTopAppBar`. RoutineDetail gets `surfaceVariant` day containers. CreateRoutine gets `OutlinedCard` day containers and a pill `FilledButton`.

- [ ] **Step 1: Update RoutineDetailScreen.kt — replace TopAppBar with LargeTopAppBar**

In `RoutineDetailScreen.kt`, replace the `TopAppBar` block with `LargeTopAppBar` and add scroll behavior:

```kotlin
// Add these imports:
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.font.FontWeight

// At the top of the composable, add:
val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

// Replace the Scaffold call to include:
Scaffold(
    topBar = {
        LargeTopAppBar(
            title = { Text("Routine Detail", fontWeight = FontWeight.Black) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            },
            scrollBehavior = scrollBehavior
        )
    },
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
) { innerPadding ->
    // ... existing LazyColumn content unchanged ...
}
```

Also update the day `Card` inside `WorkoutDayCard` (if it exists) to use `surfaceVariant`:

```kotlin
// In the day card composable, change Card colors:
Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    shape = RoundedCornerShape(16.dp)
) { /* ... existing content ... */ }
```

- [ ] **Step 2: Update CreateRoutineScreen.kt — replace TopAppBar with LargeTopAppBar and update day containers**

In `CreateRoutineScreen.kt`, add these imports:
```kotlin
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
```

Replace `TopAppBar` with `LargeTopAppBar`:
```kotlin
val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
// In Scaffold:
topBar = {
    LargeTopAppBar(
        title = { Text(if (routineId != null) "Edit Routine" else "New Routine", fontWeight = FontWeight.Black) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        scrollBehavior = scrollBehavior
    )
},
modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
```

Replace each workout day `Card` container with `OutlinedCard`:
```kotlin
OutlinedCard(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp)
) { /* ... existing content ... */ }
```

Replace the primary save button with a pill-shaped `Button`:
```kotlin
Button(
    onClick = { /* existing save logic */ },
    modifier = Modifier.fillMaxWidth().height(52.dp),
    shape = CircleShape
) {
    Text("Save Routine", fontWeight = FontWeight.Bold)
}
```

- [ ] **Step 3: Build and verify**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/RoutineDetailScreen.kt \
        app/src/main/java/com/example/workoutplanner/ui/CreateRoutineScreen.kt
git commit -m "feat: update RoutineDetailScreen and CreateRoutineScreen with LargeTopAppBar"
```

---

### Task 10: Update ExercisesScreen and EquipmentScreen

**Files:**
- Modify: `app/src/main/java/com/example/workoutplanner/ui/ExercisesScreen.kt`
- Modify: `app/src/main/java/com/example/workoutplanner/ui/EquipmentScreen.kt`

Both get `LargeTopAppBar`, `ExtendedFloatingActionButton` for add action, and `ListItem` rows.

- [ ] **Step 1: Update ExercisesScreen.kt**

Add imports:
```kotlin
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.CircleShape
```

Replace the `Scaffold` declaration:
```kotlin
val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
Scaffold(
    topBar = {
        LargeTopAppBar(
            title = { Text("Exercises", fontWeight = FontWeight.Black) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            scrollBehavior = scrollBehavior
        )
    },
    floatingActionButton = {
        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Add Exercise") }
        )
    },
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
) { innerPadding ->
    LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        // Equipment filter dropdown stays unchanged
        // Exercise items: replace existing Row with ListItem
        items(filteredExercises, key = { it.id }) { exercise ->
            ListItem(
                headlineContent = { Text(exercise.name, fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text(exercise.primaryMuscleGroup) },
                trailingContent = {
                    IconButton(onClick = { /* existing delete logic */ }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.clickable { /* existing click logic */ }
            )
            HorizontalDivider()
        }
    }
}
```

> Remove the `IconButton(onClick = { showAddDialog = true })` from `TopAppBar` actions — the FAB replaces it.

- [ ] **Step 2: Update EquipmentScreen.kt**

Apply the same pattern as ExercisesScreen:

```kotlin
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll

val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
Scaffold(
    topBar = {
        LargeTopAppBar(
            title = { Text("Equipment", fontWeight = FontWeight.Black) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            scrollBehavior = scrollBehavior
        )
    },
    floatingActionButton = {
        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Add Equipment") }
        )
    },
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
) { innerPadding ->
    LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        items(uiState.equipmentList, key = { it.id }) { equipment ->
            ListItem(
                headlineContent = { Text(equipment.name, fontWeight = FontWeight.SemiBold) },
                trailingContent = {
                    IconButton(onClick = { equipmentToEdit = equipment }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
            HorizontalDivider()
        }
    }
}
```

> Remove the `IconButton` for add from the old `TopAppBar` actions.

- [ ] **Step 3: Build and verify — final clean build**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug 2>&1 | grep -E "error:|warning:|BUILD"
```

Expected: `BUILD SUCCESSFUL` with no errors.

- [ ] **Step 4: Run lint**

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew lintDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/workoutplanner/ui/ExercisesScreen.kt \
        app/src/main/java/com/example/workoutplanner/ui/EquipmentScreen.kt
git commit -m "feat: update ExercisesScreen and EquipmentScreen with LargeTopAppBar and ExtendedFAB"
```