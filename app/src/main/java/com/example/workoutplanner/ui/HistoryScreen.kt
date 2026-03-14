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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val allHistory by viewModel.allHistory.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    
    val exerciseNameMap = remember(exercises) {
        exercises.associate { it.id to it.name }
    }

    val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
    val groupedHistory = remember(allHistory) {
        allHistory.groupBy { dateFormat.format(Date(it.date)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout History") }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (groupedHistory.isEmpty()) {
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
                items(groupedHistory.keys.toList()) { date ->
                    val entries = groupedHistory[date] ?: emptyList()
                    WorkoutSessionCard(date, entries, exerciseNameMap)
                }
            }
        }
    }
}

@Composable
fun WorkoutSessionCard(
    date: String,
    entries: List<ExerciseHistoryEntity>,
    exerciseNameMap: Map<String, String>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = date,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Group entries by exercise for this session/date
            val exerciseEntries = entries.groupBy { it.exerciseId }
            
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
