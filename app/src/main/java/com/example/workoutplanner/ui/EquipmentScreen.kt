package com.example.workoutplanner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.workoutplanner.model.Equipment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var equipmentToEdit by remember { mutableStateOf<Equipment?>(null) }
    var equipmentToDelete by remember { mutableStateOf<Equipment?>(null) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Equipment", fontWeight = FontWeight.Black) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(uiState.equipment) { equipment ->
                ListItem(
                    headlineContent = { Text(equipment.name, fontWeight = FontWeight.SemiBold) },
                    trailingContent = {
                        IconButton(onClick = { equipmentToDelete = equipment }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Equipment", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.clickable { equipmentToEdit = equipment }
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
                onConfirm = { name ->
                    if (equipmentToEdit != null) {
                        viewModel.saveEquipment(name, equipmentToEdit!!.id)
                    } else {
                        viewModel.saveEquipment(name, null)
                    }
                    showAddDialog = false
                    equipmentToEdit = null
                }
            )
        }

        if (equipmentToDelete != null) {
            AlertDialog(
                onDismissRequest = { equipmentToDelete = null },
                title = { Text("Delete Equipment") },
                text = { Text("Are you sure you want to delete '${equipmentToDelete?.name}'?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteEquipment(equipmentToDelete!!.id)
                            equipmentToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { equipmentToDelete = null }) {
                        Text("Cancel")
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
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialEquipment?.name ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialEquipment == null) "Add Equipment" else "Edit Equipment") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Equipment Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
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
                    Icons.Default.Delete,
                    contentDescription = "Delete Equipment",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}