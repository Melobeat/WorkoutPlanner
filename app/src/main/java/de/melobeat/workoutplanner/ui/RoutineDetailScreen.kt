package de.melobeat.workoutplanner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import de.melobeat.workoutplanner.model.Exercise
import de.melobeat.workoutplanner.model.Routine
import de.melobeat.workoutplanner.model.RoutineSet
import de.melobeat.workoutplanner.model.WorkoutDay
import de.melobeat.workoutplanner.ui.theme.WorkoutPlannerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    routineId: String,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutinesViewModel = hiltViewModel()
) {
    LaunchedEffect(routineId) { viewModel.loadRoutineDetail(routineId) }
    val routine by viewModel.detailRoutine.collectAsStateWithLifecycle()

    if (routine == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    RoutineDetailScreenContent(
        routine = routine!!,
        onBackClick = onBackClick,
        onEditClick = onEditClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreenContent(
    routine: Routine,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(routine.name, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back_cd))
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.routines_edit_cd))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                if (routine.description.isNotEmpty()) {
                    Text(
                        text = routine.description,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(routine.workoutDays) { day ->
                WorkoutDayItem(day = day)
            }
        }
    }
}

@Composable
fun WorkoutDayItem(day: WorkoutDay) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = day.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        day.exercises.forEach { exercise ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = exercise.name, style = MaterialTheme.typography.bodyLarge)
                    Row {
                        val repsSummary = if (exercise.routineSets.isEmpty()) {
                            "0 sets"
                        } else if (exercise.routineSets.all { it.reps == exercise.routineSets.first().reps && it.isAmrap == exercise.routineSets.first().isAmrap }) {
                            val suffix = if (exercise.routineSets.first().isAmrap) "+" else ""
                            val lrSuffix = if (exercise.sideType.name == "Unilateral") " (L/R)" else ""
                            "${exercise.routineSets.size} sets x ${exercise.routineSets.first().reps}$suffix reps$lrSuffix"
                        } else {
                            val lrSuffix = if (exercise.sideType.name == "Unilateral") " (L/R)" else ""
                            exercise.routineSets.joinToString(", ") {
                                it.reps.toString() + (if (it.isAmrap) "+" else "")
                            } + " reps$lrSuffix"
                        }

                        Text(
                            text = repsSummary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        exercise.equipmentName?.let {
                            Text(
                                text = " • $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                val firstSetWeight = exercise.routineSets.firstOrNull()?.weight ?: 0.0
                if (firstSetWeight > 0) {
                    Text(
                        text = "$firstSetWeight kg",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
    }
}

@Preview(showBackground = true)
@Composable
fun RoutineDetailScreenContentPreview() {
    WorkoutPlannerTheme {
        RoutineDetailScreenContent(
            routine = Routine(
                id = "r1",
                name = "Push Pull Legs",
                description = "3-day split targeting all major muscle groups.",
                isSelected = true,
                workoutDays = listOf(
                    WorkoutDay(
                        id = "d1",
                        name = "Push Day",
                        exercises = listOf(
                            Exercise(id = "e1", name = "Bench Press", muscleGroup = "Chest",
                                equipmentName = "Barbell",
                                routineSets = listOf(RoutineSet(10, 60.0), RoutineSet(10, 60.0), RoutineSet(8, 60.0))),
                            Exercise(id = "e2", name = "Overhead Press", muscleGroup = "Shoulders",
                                routineSets = listOf(RoutineSet(8, 40.0)))
                        )
                    ),
                    WorkoutDay(id = "d2", name = "Pull Day", exercises = listOf())
                )
            ),
            onBackClick = {},
            onEditClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WorkoutDayItemPreview() {
    WorkoutPlannerTheme {
        WorkoutDayItem(
            day = WorkoutDay(
                id = "d1",
                name = "Push Day",
                exercises = listOf(
                    Exercise(id = "e1", name = "Bench Press", muscleGroup = "Chest",
                        equipmentName = "Barbell",
                        routineSets = listOf(RoutineSet(10, 60.0), RoutineSet(10, 60.0), RoutineSet(8, 60.0))),
                    Exercise(id = "e2", name = "Overhead Press", muscleGroup = "Shoulders",
                        routineSets = listOf(RoutineSet(8, 40.0)))
                )
            )
        )
    }
}
