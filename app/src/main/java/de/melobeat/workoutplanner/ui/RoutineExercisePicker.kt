package de.melobeat.workoutplanner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.melobeat.workoutplanner.R
import de.melobeat.workoutplanner.model.Exercise

@Composable
fun ExercisePicker(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onExerciseSelected: (Exercise) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_routine_select_exercise)) },
        text = {
            LazyColumn {
                itemsIndexed(exercises) { _, exercise ->
                    ListItem(
                        headlineContent = { Text(exercise.name) },
                        supportingContent = { Text(exercise.muscleGroup) },
                        modifier = Modifier.fillMaxWidth().clickable { onExerciseSelected(exercise) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
