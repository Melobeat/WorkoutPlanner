package de.melobeat.workoutplanner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.melobeat.workoutplanner.R
import de.melobeat.workoutplanner.model.Exercise
import de.melobeat.workoutplanner.model.WorkoutDay

@Composable
fun DayCard(
    day: WorkoutDay,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onNameChange: (String) -> Unit,
    onAddExercise: () -> Unit,
    onRemoveDay: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onUpdateExercise: (Int, Exercise) -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onMoveExerciseUp: (Int) -> Unit,
    onMoveExerciseDown: (Int) -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.clickable { onToggleExpand() }
                )
                OutlinedTextField(
                    value = day.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.create_routine_day_name_label)) },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.titleMedium
                )
                onMoveUp?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Rounded.ArrowUpward, contentDescription = stringResource(R.string.create_routine_move_day_up_cd))
                    }
                }
                onMoveDown?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Rounded.ArrowDownward, contentDescription = stringResource(R.string.create_routine_move_day_down_cd))
                    }
                }
                IconButton(onClick = onRemoveDay) {
                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.create_routine_remove_day_cd))
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    day.exercises.forEachIndexed { index, exercise ->
                        ExerciseEditItem(
                            exercise = exercise,
                            onUpdate = { updated -> onUpdateExercise(index, updated) },
                            onRemove = { onRemoveExercise(index) },
                            onMoveUp = if (index > 0) { { onMoveExerciseUp(index) } } else null,
                            onMoveDown = if (index < day.exercises.size - 1) { { onMoveExerciseDown(index) } } else null
                        )
                        if (index < day.exercises.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }

                    TextButton(
                        onClick = onAddExercise,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text(stringResource(R.string.create_routine_add_exercise))
                    }
                }
            }
        }
    }
}
