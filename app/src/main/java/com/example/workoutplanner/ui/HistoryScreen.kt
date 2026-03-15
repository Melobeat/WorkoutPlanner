package com.example.workoutplanner.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workoutplanner.WorkoutViewModel
import com.example.workoutplanner.data.ExerciseHistoryEntity
import com.example.workoutplanner.data.WorkoutHistoryWithExercises
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val workoutHistory by viewModel.workoutHistory.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    
    val exerciseNameMap = remember(exercises) {
        exercises.associate { it.id to it.name }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout History") }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (workoutHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No workout history yet. Start a workout to see it here!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(workoutHistory) { session ->
                    WorkoutSessionCard(session, exerciseNameMap)
                }
            }
        }
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
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = durationString,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                
                sets.forEachIndexed { index, set ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Set ${index + 1}: ${set.reps} reps",
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
