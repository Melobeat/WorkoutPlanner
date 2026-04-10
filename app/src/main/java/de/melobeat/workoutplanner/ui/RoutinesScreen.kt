package de.melobeat.workoutplanner.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.melobeat.workoutplanner.model.Routine
import de.melobeat.workoutplanner.ui.theme.WorkoutPlannerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    onRoutineClick: (routineId: String) -> Unit,
    onCreateRoutineClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutinesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RoutinesScreenContent(
        uiState = uiState,
        onRoutineClick = onRoutineClick,
        onCreateRoutineClick = onCreateRoutineClick,
        onBack = onBack,
        onSelectRoutine = { id -> viewModel.selectRoutine(id) },
        onDeleteRoutine = { id -> viewModel.deleteRoutine(id) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreenContent(
    uiState: RoutinesUiState,
    onRoutineClick: (routineId: String) -> Unit,
    onCreateRoutineClick: () -> Unit,
    onBack: () -> Unit,
    onSelectRoutine: (id: String) -> Unit,
    onDeleteRoutine: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var routineToDelete by remember { mutableStateOf<Routine?>(null) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
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
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("New Routine") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
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
                                if (!isActive) onSelectRoutine(routine.id)
                            }) {
                                Icon(
                                    if (isActive) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = if (isActive) "Active" else "Set Active",
                                    tint = if (isActive) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { routineToDelete = routine }) {
                                Icon(
                                    Icons.Rounded.Delete,
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
                    onDeleteRoutine(routine.id)
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

@Preview(showBackground = true)
@Composable
fun RoutinesScreenContentPreview() {
    WorkoutPlannerTheme {
        RoutinesScreenContent(
            uiState = RoutinesUiState(
                routines = listOf(
                    Routine(id = "r1", name = "Push Pull Legs", isSelected = true),
                    Routine(id = "r2", name = "Upper Lower Split", isSelected = false)
                ),
                isLoading = false
            ),
            onRoutineClick = {},
            onCreateRoutineClick = {},
            onBack = {},
            onSelectRoutine = {},
            onDeleteRoutine = {}
        )
    }
}