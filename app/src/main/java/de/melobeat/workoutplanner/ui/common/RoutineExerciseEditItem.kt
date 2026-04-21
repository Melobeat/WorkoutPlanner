package de.melobeat.workoutplanner.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import de.melobeat.workoutplanner.R
import de.melobeat.workoutplanner.model.Exercise
import de.melobeat.workoutplanner.model.RoutineSet
import de.melobeat.workoutplanner.model.SideType

@Composable
fun ExerciseEditItem(
    exercise: Exercise,
    onUpdate: (Exercise) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.clickable { isExpanded = !isExpanded }
            )
            Text(
                exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).clickable { isExpanded = !isExpanded },
                fontWeight = FontWeight.Bold
            )
            if (exercise.sideType == SideType.Unilateral) {
                Text(
                    text = stringResource(R.string.label_unilateral),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            onMoveUp?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = stringResource(R.string.create_routine_move_exercise_up_cd))
                }
            }
            onMoveDown?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Rounded.ArrowDownward, contentDescription = stringResource(R.string.create_routine_move_exercise_down_cd))
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.create_routine_remove_exercise_cd))
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                exercise.routineSets.forEachIndexed { index, set ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text("${index + 1}", modifier = Modifier.width(20.dp))

                        var repsText by remember(set.reps) { mutableStateOf(set.reps.toString()) }
                        OutlinedTextField(
                            value = repsText,
                            onValueChange = {
                                repsText = it
                                val newReps = it.toIntOrNull()
                                if (newReps != null) {
                                    val newSets = exercise.routineSets.toMutableList()
                                    newSets[index] = set.copy(reps = newReps)
                                    onUpdate(exercise.copy(routineSets = newSets))
                                }
                            },
                            label = { Text(stringResource(R.string.create_routine_reps_label)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        var weightText by remember(set.weight) { mutableStateOf(set.weight.toString()) }
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = {
                                weightText = it
                                val newWeight = it.toDoubleOrNull()
                                if (newWeight != null) {
                                    val newSets = exercise.routineSets.toMutableList()
                                    newSets[index] = set.copy(weight = newWeight)
                                    onUpdate(exercise.copy(routineSets = newSets))
                                }
                            },
                            label = { Text(stringResource(R.string.create_routine_weight_label)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.label_amrap), style = MaterialTheme.typography.labelSmall)
                            Checkbox(
                                checked = set.isAmrap,
                                onCheckedChange = { isChecked ->
                                    val newSets = exercise.routineSets.toMutableList()
                                    newSets[index] = set.copy(isAmrap = isChecked)
                                    onUpdate(exercise.copy(routineSets = newSets))
                                }
                            )
                        }

                        IconButton(onClick = {
                            val newSets = exercise.routineSets.toMutableList().apply { removeAt(index) }
                            onUpdate(exercise.copy(routineSets = newSets))
                        }) {
                            Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.create_routine_remove_set_cd))
                        }
                    }
                }
                TextButton(
                    onClick = {
                        val newSets = exercise.routineSets + RoutineSet(10, 0.0)
                        onUpdate(exercise.copy(routineSets = newSets))
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text(stringResource(R.string.create_routine_add_set))
                }
            }
        }
    }
}
