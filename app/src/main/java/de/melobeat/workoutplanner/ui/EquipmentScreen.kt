package de.melobeat.workoutplanner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.melobeat.workoutplanner.R
import de.melobeat.workoutplanner.model.Equipment
import de.melobeat.workoutplanner.ui.theme.WorkoutPlannerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    EquipmentScreenContent(
        equipment = uiState.equipment,
        onBack = onBack,
        onSaveEquipment = { name, id, defaultWeight -> viewModel.saveEquipment(name, id, defaultWeight) },
        onDeleteEquipment = { id -> viewModel.deleteEquipment(id) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentScreenContent(
    equipment: List<Equipment>,
    onBack: () -> Unit,
    onSaveEquipment: (name: String, id: String?, defaultWeight: Double?) -> Unit,
    onDeleteEquipment: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var equipmentToEdit by remember { mutableStateOf<Equipment?>(null) }
    var equipmentToDelete by remember { mutableStateOf<Equipment?>(null) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.equipment_title), fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.equipment_back_cd))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.equipment_add_fab)) },
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
            items(equipment) { item ->
                ListItem(
                    headlineContent = { Text(item.name, fontWeight = FontWeight.SemiBold) },
                    supportingContent = item.defaultWeight?.let { w ->
                        { Text(stringResource(R.string.equipment_bar_weight, if (w % 1.0 == 0.0) w.toInt().toString() else w.toString())) }
                    },
                    trailingContent = {
                        IconButton(onClick = { equipmentToDelete = item }) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.equipment_delete_cd), tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.clickable { equipmentToEdit = item }
                )
                HorizontalDivider()
            }
        }

        if (showAddDialog || equipmentToEdit != null) {
            EquipmentDialog(
                initialEquipment = equipmentToEdit,
                onDismiss = {
                    showAddDialog = false
                    equipmentToEdit = null
                },
                onConfirm = { name, defaultWeight ->
                    onSaveEquipment(name, equipmentToEdit?.id, defaultWeight)
                    showAddDialog = false
                    equipmentToEdit = null
                }
            )
        }

        if (equipmentToDelete != null) {
            AlertDialog(
                onDismissRequest = { equipmentToDelete = null },
                title = { Text(stringResource(R.string.equipment_delete_dialog_title)) },
                text = { Text(stringResource(R.string.equipment_delete_dialog_body, equipmentToDelete?.name ?: "")) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteEquipment(equipmentToDelete!!.id)
                            equipmentToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { equipmentToDelete = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun EquipmentDialog(
    initialEquipment: Equipment? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, defaultWeight: Double?) -> Unit
) {
    var name by remember { mutableStateOf(initialEquipment?.name ?: "") }
    var weightText by remember {
        mutableStateOf(
            initialEquipment?.defaultWeight?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            } ?: ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialEquipment == null) stringResource(R.string.equipment_add_dialog_title) else stringResource(R.string.equipment_edit_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.equipment_name_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text(stringResource(R.string.equipment_bar_weight_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val weight = weightText.trim().toDoubleOrNull()
                    onConfirm(name, weight)
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (initialEquipment == null) stringResource(R.string.action_add) else stringResource(R.string.action_save))
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
fun EquipmentItem(
    equipment: Equipment,
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
            Text(
                text = equipment.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.equipment_delete_cd),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EquipmentScreenContentPreview() {
    WorkoutPlannerTheme {
        EquipmentScreenContent(
            equipment = listOf(
                Equipment(id = "1", name = "Barbell"),
                Equipment(id = "2", name = "Dumbbell"),
                Equipment(id = "3", name = "Cable Machine")
            ),
            onBack = {},
            onSaveEquipment = { _, _, _ -> },
            onDeleteEquipment = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EquipmentDialogPreview() {
    WorkoutPlannerTheme {
        EquipmentDialog(
            initialEquipment = null,
            onDismiss = {},
            onConfirm = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EquipmentItemPreview() {
    WorkoutPlannerTheme {
        EquipmentItem(
            equipment = Equipment(id = "1", name = "Barbell"),
            onClick = {},
            onDelete = {}
        )
    }
}