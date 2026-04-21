package de.melobeat.workoutplanner.ui.feature.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.melobeat.workoutplanner.R
import de.melobeat.workoutplanner.model.Equipment
import de.melobeat.workoutplanner.model.Exercise
import de.melobeat.workoutplanner.model.SideType
import de.melobeat.workoutplanner.ui.theme.WorkoutPlannerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExercisesScreenContent(
        exercises = uiState.exercises,
        equipment = uiState.equipment,
        onBack = onBack,
        onSaveExercise = { name, muscle, desc, equipId, id, isBodyweight, sideType ->
            viewModel.saveExercise(name, muscle, desc, equipId, id, isBodyweight, sideType)
        },
        onDeleteExercise = { id -> viewModel.deleteExercise(id) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreenContent(
    exercises: List<Exercise>,
    equipment: List<Equipment>,
    onBack: () -> Unit,
    onSaveExercise: (name: String, muscle: String, desc: String, equipId: String?, id: String?, isBodyweight: Boolean, sideType: String) -> Unit,
    onDeleteExercise: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<Exercise?>(null) }
    var exerciseToDelete by remember { mutableStateOf<Exercise?>(null) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.exercises_title), fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.exercises_back_cd))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.exercises_add_fab)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(exercises) { exercise ->
                ListItem(
                    headlineContent = { Text(exercise.name, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(exercise.muscleGroup) },
                    trailingContent = {
                        IconButton(onClick = { exerciseToDelete = exercise }) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.exercises_delete_cd), tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.clickable { exerciseToEdit = exercise }
                )
                HorizontalDivider()
            }
        }

        if (showAddDialog || exerciseToEdit != null) {
            AddExerciseDialog(
                initialExercise = exerciseToEdit,
                equipmentList = equipment,
                onDismiss = {
                    showAddDialog = false
                    exerciseToEdit = null
                },
                onConfirm = { name, muscle, desc, equipId, isBodyweight, sideType ->
                    onSaveExercise(name, muscle, desc, equipId, exerciseToEdit?.id, isBodyweight, sideType)
                    showAddDialog = false
                    exerciseToEdit = null
                }
            )
        }

        if (exerciseToDelete != null) {
            AlertDialog(
                onDismissRequest = { exerciseToDelete = null },
                title = { Text(stringResource(R.string.exercises_delete_dialog_title)) },
                text = { Text(stringResource(R.string.exercises_delete_dialog_body, exerciseToDelete?.name ?: "")) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteExercise(exerciseToDelete!!.id)
                            exerciseToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { exerciseToDelete = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialog(
    initialExercise: Exercise? = null,
    equipmentList: List<Equipment>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String?, Boolean, String) -> Unit
) {
    var name by remember { mutableStateOf(initialExercise?.name ?: "") }
    var muscleGroup by remember { mutableStateOf(initialExercise?.muscleGroup ?: "") }
    var description by remember { mutableStateOf(initialExercise?.description ?: "") }
    var selectedEquipmentId by remember { mutableStateOf(initialExercise?.equipmentId) }
    var isBodyweight by remember { mutableStateOf(initialExercise?.isBodyweight ?: false) }
    var sideType by remember { mutableStateOf(initialExercise?.sideType?.name ?: "Bilateral") }

    var expanded by remember { mutableStateOf(false) }
    val selectedEquipmentName = equipmentList.find { it.id == selectedEquipmentId }?.name ?: stringResource(R.string.exercises_no_equipment)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialExercise == null) stringResource(R.string.exercises_add_dialog_title) else stringResource(R.string.exercises_edit_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.exercises_name_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = muscleGroup,
                    onValueChange = { muscleGroup = it },
                    label = { Text(stringResource(R.string.exercises_muscle_label)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.exercises_description_label)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.exercises_bodyweight_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isBodyweight,
                        onCheckedChange = { isBodyweight = it }
                    )
                }

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

                Spacer(modifier = Modifier.height(8.dp))

                Text(stringResource(R.string.exercises_equipment_label), style = MaterialTheme.typography.labelMedium)
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
                            text = { Text(stringResource(R.string.exercises_no_equipment)) },
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, muscleGroup, description, selectedEquipmentId, isBodyweight, sideType) },
                enabled = name.isNotBlank() && muscleGroup.isNotBlank()
            ) {
                Text(if (initialExercise == null) stringResource(R.string.action_add) else stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun ExerciseLibraryItem(
    exercise: Exercise,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = exercise.name, style = MaterialTheme.typography.titleLarge)
                Row {
                    Text(
                        text = exercise.muscleGroup,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (exercise.sideType == SideType.Unilateral) {
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
                if (exercise.description.isNotEmpty()) {
                    Text(
                        text = exercise.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.exercises_delete_cd), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExercisesScreenContentPreview() {
    WorkoutPlannerTheme {
        ExercisesScreenContent(
            exercises = listOf(
                Exercise(id = "e1", name = "Bench Press", muscleGroup = "Chest",
                    description = "Flat barbell chest press", equipmentId = "eq1", equipmentName = "Barbell"),
                Exercise(id = "e2", name = "Squat", muscleGroup = "Legs",
                    description = "", equipmentId = null, equipmentName = null)
            ),
            equipment = listOf(Equipment(id = "eq1", name = "Barbell")),
            onBack = {},
            onSaveExercise = { _, _, _, _, _, _, _ -> },
            onDeleteExercise = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddExerciseDialogPreview() {
    WorkoutPlannerTheme {
        AddExerciseDialog(
            initialExercise = null,
            equipmentList = listOf(
                Equipment(id = "eq1", name = "Barbell"),
                Equipment(id = "eq2", name = "Dumbbell")
            ),
            onDismiss = {},
            onConfirm = { _, _, _, _, _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ExerciseLibraryItemPreview() {
    WorkoutPlannerTheme {
        ExerciseLibraryItem(
            exercise = Exercise(
                id = "e1",
                name = "Bench Press",
                muscleGroup = "Chest",
                description = "Flat barbell press",
                equipmentId = "eq1",
                equipmentName = "Barbell"
            ),
            onClick = {},
            onDelete = {}
        )
    }
}