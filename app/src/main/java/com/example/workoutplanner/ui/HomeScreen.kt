package com.example.workoutplanner.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workoutplanner.model.Routine
import com.example.workoutplanner.model.WorkoutDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    selectedRoutine: Routine?,
    onStartWorkout: (WorkoutDay, Int) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Planner") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Welcome Back!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            if (selectedRoutine == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "No routine selected",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Go to Settings > Manage Routines to select a routine to follow.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                val nextDayIndex = (selectedRoutine.lastCompletedDayIndex + 1) % selectedRoutine.workoutDays.size
                val nextDay = selectedRoutine.workoutDays[nextDayIndex]

                Text(
                    text = "Next Workout: ${selectedRoutine.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = nextDay.name,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "Day ${nextDayIndex + 1} of ${selectedRoutine.workoutDays.size}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        
                        nextDay.exercises.take(3).forEach { exercise ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = "• ${exercise.name} (${exercise.sets}x${exercise.reps})",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                exercise.equipmentName?.let {
                                    Text(
                                        text = "  Equipment: $it",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                            }
                        }
                        if (nextDay.exercises.size > 3) {
                            Text(
                                text = "...and ${nextDay.exercises.size - 3} more",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { onStartWorkout(nextDay, nextDayIndex) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start Workout")
                        }
                    }
                }
            }
        }
    }
}
