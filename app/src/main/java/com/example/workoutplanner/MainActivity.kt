package com.example.workoutplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.workoutplanner.ui.ActiveWorkoutViewModel
import com.example.workoutplanner.ui.formatElapsedTime
import com.example.workoutplanner.ui.navigation.ActiveWorkoutRoute
import com.example.workoutplanner.ui.navigation.HistoryRoute
import com.example.workoutplanner.ui.navigation.HomeRoute
import com.example.workoutplanner.ui.navigation.WorkoutNavGraph
import com.example.workoutplanner.ui.theme.WorkoutPlannerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkoutPlannerTheme {
                WorkoutPlannerApp()
            }
        }
    }
}

@Composable
fun WorkoutPlannerApp() {
    val navController = rememberNavController()
    val activeWorkoutViewModel: ActiveWorkoutViewModel = viewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )
    val workoutUiState by activeWorkoutViewModel.uiState.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            item(
                icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
                label = { Text("Home") },
                selected = currentDestination?.hasRoute<HomeRoute>() == true,
                onClick = {
                    navController.navigate(HomeRoute) {
                        popUpTo(HomeRoute) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
            item(
                icon = { Icon(Icons.Rounded.History, contentDescription = "History") },
                label = { Text("History") },
                selected = currentDestination?.hasRoute<HistoryRoute>() == true,
                onClick = {
                    navController.navigate(HistoryRoute) {
                        popUpTo(HomeRoute) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (workoutUiState.isActive && !workoutUiState.isFullScreen) {
                    Surface(
                        onClick = {
                            activeWorkoutViewModel.setFullScreen(true)
                            navController.navigate(ActiveWorkoutRoute)
                        },
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        formatElapsedTime(workoutUiState.elapsedTime),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        workoutUiState.workoutDayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = {
                                    activeWorkoutViewModel.setFullScreen(true)
                                    navController.navigate(ActiveWorkoutRoute)
                                },
                                shape = CircleShape
                            ) {
                                Text("Resume")
                            }
                        }
                    }
                }
            }
        ) { scaffoldPadding ->
            WorkoutNavGraph(
                navController = navController,
                activeWorkoutViewModel = activeWorkoutViewModel,
                modifier = Modifier.padding(scaffoldPadding)
            )
        }
    }
}
