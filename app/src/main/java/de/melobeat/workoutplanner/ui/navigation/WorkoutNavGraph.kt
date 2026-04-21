package de.melobeat.workoutplanner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import de.melobeat.workoutplanner.ui.feature.workout.ActiveWorkoutViewModel
import de.melobeat.workoutplanner.ui.feature.routines.CreateRoutineScreen
import de.melobeat.workoutplanner.ui.feature.equipment.EquipmentScreen
import de.melobeat.workoutplanner.ui.feature.exercises.ExercisesScreen
import de.melobeat.workoutplanner.ui.feature.history.HistoryScreen
import de.melobeat.workoutplanner.ui.feature.home.HomeScreen
import de.melobeat.workoutplanner.ui.feature.profile.ProfileScreen
import de.melobeat.workoutplanner.ui.feature.routines.RoutineDetailScreen
import de.melobeat.workoutplanner.ui.feature.routines.RoutinesScreen
import de.melobeat.workoutplanner.ui.feature.settings.SettingsScreen
import de.melobeat.workoutplanner.ui.feature.settings.TimerSettingsScreen
import de.melobeat.workoutplanner.ui.feature.workout.WorkoutScreen
import de.melobeat.workoutplanner.ui.feature.workout.WorkoutSummaryScreen

@Composable
fun WorkoutNavGraph(
    navController: NavHostController,
    activeWorkoutViewModel: ActiveWorkoutViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onNavigateToSettings = { navController.navigate(SettingsGraphRoute) },
                onNavigateToRoutines = { navController.navigate(RoutinesRoute) },
                onStartWorkout = { navController.navigate(ActiveWorkoutRoute) }
            )
        }

        composable<HistoryRoute> {
            HistoryScreen()
        }

        composable<ActiveWorkoutRoute> {
            DisposableEffect(Unit) {
                activeWorkoutViewModel.setFullScreen(true)
                onDispose { activeWorkoutViewModel.setFullScreen(false) }
            }
            WorkoutScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSummary = { navController.navigate(WorkoutSummaryRoute) }
            )
        }

        composable<WorkoutSummaryRoute> {
            WorkoutSummaryScreen(
                onResumeWorkout = {
                    activeWorkoutViewModel.resumeWorkout()
                    navController.popBackStack()
                },
                onWorkoutFinished = {
                    navController.popBackStack(ActiveWorkoutRoute, inclusive = true)
                }
            )
        }

        navigation<SettingsGraphRoute>(startDestination = SettingsRoute) {
            composable<SettingsRoute> {
                SettingsScreen(
                    onNavigateToExercises = { navController.navigate(ExercisesRoute) },
                    onNavigateToRoutines = { navController.navigate(RoutinesRoute) },
                    onNavigateToEquipment = { navController.navigate(EquipmentRoute) },
                    onNavigateToTimerSettings = { navController.navigate(TimerSettingsRoute) },
                    onNavigateToProfile = { navController.navigate(ProfileRoute) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<ExercisesRoute> {
                ExercisesScreen(onBack = { navController.popBackStack() })
            }
            composable<EquipmentRoute> {
                EquipmentScreen(onBack = { navController.popBackStack() })
            }
            composable<TimerSettingsRoute> {
                TimerSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable<ProfileRoute> {
                ProfileScreen(onBack = { navController.popBackStack() })
            }
            composable<RoutinesRoute> {
                RoutinesScreen(
                    onRoutineClick = { routineId ->
                        navController.navigate(RoutineDetailRoute(routineId))
                    },
                    onCreateRoutineClick = { navController.navigate(CreateRoutineRoute()) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<RoutineDetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<RoutineDetailRoute>()
                RoutineDetailScreen(
                    routineId = route.routineId,
                    onBackClick = { navController.popBackStack() },
                    onEditClick = { navController.navigate(CreateRoutineRoute(route.routineId)) }
                )
            }
            composable<CreateRoutineRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<CreateRoutineRoute>()
                CreateRoutineScreen(
                    routineId = route.routineId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
