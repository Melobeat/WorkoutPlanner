package de.melobeat.workoutplanner.ui.feature.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import de.melobeat.workoutplanner.R
import de.melobeat.workoutplanner.data.ExerciseHistoryEntity
import de.melobeat.workoutplanner.data.WorkoutHistoryEntity
import de.melobeat.workoutplanner.data.WorkoutHistoryWithExercises
import de.melobeat.workoutplanner.ui.theme.WorkoutPlannerTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreenContent(uiState = uiState, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreenContent(
    uiState: HistoryUiState,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val expandedIds = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.history_title), fontWeight = FontWeight.Black) },
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
                            stringResource(R.string.history_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.history_empty_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                val grouped = uiState.sessions.groupBy { getDateGroupLabel(it.workout.date) }
                val orderedKeys = grouped.keys.sortedWith(Comparator { a, b ->
                    val order = listOf("This Week", "Last Week")
                    val ai = order.indexOf(a)
                    val bi = order.indexOf(b)
                    when {
                        ai != -1 && bi != -1 -> ai - bi
                        ai != -1 -> -1
                        bi != -1 -> 1
                        else -> b.compareTo(a)
                    }
                })

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    orderedKeys.forEachIndexed { groupIndex, groupLabel ->
                        item(key = "header_$groupLabel") {
                            val displayLabel = when (groupLabel) {
                                "This Week" -> stringResource(R.string.history_this_week)
                                "Last Week" -> stringResource(R.string.history_last_week)
                                else -> groupLabel
                            }
                            Text(
                                displayLabel.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = if (groupIndex == 0) 0.dp else 8.dp, bottom = 4.dp)
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
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor     = MaterialTheme.colorScheme.primary
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
                                val summary = sets.joinToString(" · ") { "${it.reps}×${it.weight}kg" }
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
                                    stringResource(R.string.history_more_exercises, displayEntries.size - 3),
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

@Composable
fun WorkoutSessionCard(
    session: WorkoutHistoryWithExercises,
    exerciseNameMap: Map<String, String>
) {
    val dateFormat = remember { SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()) }
    val dateString = dateFormat.format(Date(session.workout.date))

    val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(session.workout.durationMillis)
    val durationSeconds = TimeUnit.MILLISECONDS.toSeconds(session.workout.durationMillis) % 60
    val durationString = if (durationMinutes > 0) {
        "${durationMinutes}m ${durationSeconds}s"
    } else {
        "${durationSeconds}s"
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = session.workout.workoutDayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = durationString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Group entries by exercise for this session
            val exerciseEntries = session.exercises.groupBy { it.exerciseId }

            exerciseEntries.forEach { (exerciseId, sets) ->
                val exerciseName = exerciseNameMap[exerciseId] ?: "Unknown Exercise"
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                sets.forEach { set ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                             text = stringResource(R.string.history_set_reps_label, set.sets, set.reps, if (set.isAmrap) "+" else ""),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${set.weight} kg",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenContentPreview() {
    val session = WorkoutHistoryWithExercises(
        workout = WorkoutHistoryEntity(
            id = "h1",
            routineName = "Push Pull Legs",
            workoutDayName = "Push Day",
            date = System.currentTimeMillis() - 86_400_000L,
            durationMillis = 3_720_000L
        ),
        exercises = listOf(
            ExerciseHistoryEntity(id = "eh1", workoutHistoryId = "h1", exerciseId = "ex1",
                date = System.currentTimeMillis(), sets = 1, reps = 10, weight = 60.0),
            ExerciseHistoryEntity(id = "eh2", workoutHistoryId = "h1", exerciseId = "ex2",
                date = System.currentTimeMillis(), sets = 1, reps = 8, weight = 80.0)
        )
    )
    WorkoutPlannerTheme {
        HistoryScreenContent(
            uiState = HistoryUiState(
                sessions = listOf(session),
                exerciseNameMap = mapOf("ex1" to "Bench Press", "ex2" to "Overhead Press"),
                isLoading = false
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HistorySessionCardPreview() {
    val session = WorkoutHistoryWithExercises(
        workout = WorkoutHistoryEntity(
            id = "h1",
            routineName = "Push Pull Legs",
            workoutDayName = "Push Day",
            date = System.currentTimeMillis(),
            durationMillis = 3_720_000L
        ),
        exercises = listOf(
            ExerciseHistoryEntity(id = "eh1", workoutHistoryId = "h1", exerciseId = "ex1",
                date = System.currentTimeMillis(), sets = 1, reps = 10, weight = 60.0)
        )
    )
    WorkoutPlannerTheme {
        HistorySessionCard(
            session = session,
            exerciseNameMap = mapOf("ex1" to "Bench Press"),
            isExpanded = true,
            onToggleExpand = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WorkoutSessionCardPreview() {
    val session = WorkoutHistoryWithExercises(
        workout = WorkoutHistoryEntity(
            id = "h1",
            routineName = "Push Pull Legs",
            workoutDayName = "Push Day",
            date = System.currentTimeMillis(),
            durationMillis = 2_400_000L
        ),
        exercises = listOf(
            ExerciseHistoryEntity(id = "eh1", workoutHistoryId = "h1", exerciseId = "ex1",
                date = System.currentTimeMillis(), sets = 1, reps = 10, weight = 60.0),
            ExerciseHistoryEntity(id = "eh2", workoutHistoryId = "h1", exerciseId = "ex2",
                date = System.currentTimeMillis(), sets = 2, reps = 8, weight = 80.0)
        )
    )
    WorkoutPlannerTheme {
        WorkoutSessionCard(
            session = session,
            exerciseNameMap = mapOf("ex1" to "Bench Press", "ex2" to "Overhead Press")
        )
    }
}