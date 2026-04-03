# Dialog Redesign Spec

**Date:** 2026-04-03
**Status:** Approved
**Scope:** ExerciseSelectionDialog (Swap Exercise + Add Exercise), WorkoutDayChooserDialog (Swap Workout)

---

## Overview

Redesign the three workout dialogs to follow the design guidelines: `ListItem` rows, correct shape tokens, M3 Expressive motion, and gym-friendly touch targets. The exercise selection dialog is upgraded from a bare `Dialog`+`Card` shell to a `ModalBottomSheet` with search.

---

## 1. ExerciseSelectionDialog

**File:** `app/src/main/java/de/melobeat/workoutplanner/ui/ExerciseSelectionDialog.kt`

### Before

- `Dialog` wrapper with a manual `Card` container (`fillMaxHeight(0.8f)`)
- `Surface(onClick)` + `Column` rows (no `ListItem`)
- Hardcoded title "Select Exercise" — no parameter
- No search/filter
- `TextButton("Cancel")` at bottom

### After

Replace the entire composable with `ModalBottomSheet`.

**Signature change:**
```kotlin
@Composable
fun ExerciseSelectionDialog(
    title: String,                         // new — "Add Exercise" or "Swap Exercise"
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onExerciseSelected: (Exercise) -> Unit
)
```

**Structure:**
```
ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(title, style = titleLarge, fontWeight = Bold)
        Spacer(8.dp)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search...") },
            leadingIcon = { Icon(Icons.Rounded.Search) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(8.dp)
        LazyColumn {
            items(filteredExercises) { exercise ->
                ListItem(
                    headlineContent = { Text(exercise.name, style = titleMedium) },
                    supportingContent = { Text(exercise.muscleGroup, style = bodySmall) },
                    leadingContent = {
                        Surface(shape = CircleShape, color = surfaceVariant) {
                            Icon(Icons.Rounded.FitnessCenter, modifier = Modifier.padding(8.dp))
                        }
                    },
                    modifier = Modifier.clickable { onExerciseSelected(exercise) }
                )
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
```

**Search logic:** filter `exercises` where `name` or `muscleGroup` contains `query` (case-insensitive). Query state is local (`remember { mutableStateOf("") }`).

**Dismiss:** sheet drag or scrim tap — no explicit Cancel button.

**Motion:** `ModalBottomSheet` gets M3 Expressive spring animation automatically.

### Call site updates (`WorkoutScreen.kt`)

Both `showAddExerciseDialog` and `showSwapDialog` blocks add a `title` argument:

```kotlin
// Add exercise
ExerciseSelectionDialog(
    title = "Add Exercise",
    exercises = availableExercises,
    onDismiss = { showAddExerciseDialog = false },
    onExerciseSelected = { exercise ->
        onAddExercise(exercise)
        showAddExerciseDialog = false
    }
)

// Swap exercise
ExerciseSelectionDialog(
    title = "Swap Exercise",
    exercises = availableExercises,
    onDismiss = { showSwapDialog = false },
    onExerciseSelected = { exercise ->
        onSwapExercise(ei, exercise)
        showSwapDialog = false
    }
)
```

---

## 2. WorkoutDayChooserDialog

**File:** `app/src/main/java/de/melobeat/workoutplanner/ui/HomeScreen.kt`

### Before

- `AlertDialog` with `text` slot containing manual `Card(onClick)` + `Column` rows
- Card radius: `RoundedCornerShape(12.dp)` (wrong — should be 16dp)
- Cancel in `confirmButton` slot (wrong slot)

### After

Keep `AlertDialog`. Replace manual rows with `ListItem`-based rows.

**Row structure:**
```
Surface(
    onClick = { onDaySelected(index) },
    shape = RoundedCornerShape(16.dp),
    color = surfaceVariant
) {
    ListItem(
        headlineContent = { Text("Day ${index+1}: ${day.name}", style = titleMedium) },
        supportingContent = {
            Text(
                day.exercises.joinToString(", ") { it.name },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = bodySmall
            )
        },
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        "${index + 1}",
                        style = labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    )
}
```

**Buttons:**
- Move Cancel from `confirmButton` to `dismissButton` slot
- Remove `confirmButton` entirely (no confirm action needed)

---

## Files to Change

| File | Change |
|---|---|
| `ui/ExerciseSelectionDialog.kt` | Full rewrite — `ModalBottomSheet` with search |
| `ui/WorkoutScreen.kt` | Add `title` parameter at both call sites |
| `ui/HomeScreen.kt` | Rewrite `WorkoutDayChooserDialog` rows + fix button slots |