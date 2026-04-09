package de.melobeat.workoutplanner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.melobeat.workoutplanner.ui.theme.WorkoutPlannerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToExercises: () -> Unit,
    onNavigateToRoutines: () -> Unit,
    onNavigateToEquipment: () -> Unit,
    onNavigateToTimerSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    timerSettingsViewModel: TimerSettingsViewModel = hiltViewModel()
) {
    val themeMode by timerSettingsViewModel.themeMode.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        ) {
            item {
                ListItem(
                    headlineContent = { Text("Theme", fontWeight = FontWeight.SemiBold) },
                    supportingContent = {
                        val label = when (themeMode) {
                            "light"  -> "Light"
                            "system" -> "Follow system"
                            else     -> "Dark"
                        }
                        Text(label)
                    },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingContent = {
                        SingleChoiceSegmentedButtonRow {
                            listOf("dark" to "Dark", "light" to "Light", "system" to "System")
                                .forEachIndexed { index, (mode, label) ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                                        onClick = { timerSettingsViewModel.setThemeMode(mode) },
                                        selected = themeMode == mode,
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                        }
                    }
                )
                HorizontalDivider()
            }
            item {
                SettingsListItem(
                    title = "Timer Settings",
                    subtitle = "Rest timer durations between sets and exercises",
                    icon = Icons.Outlined.Timer,
                    onClick = onNavigateToTimerSettings
                )
                HorizontalDivider()
            }
            item {
                SettingsListItem(
                    title = "Manage Exercises",
                    subtitle = "Add, edit or delete exercises",
                    icon = Icons.Outlined.FitnessCenter,
                    onClick = onNavigateToExercises
                )
                HorizontalDivider()
            }
            item {
                SettingsListItem(
                    title = "Manage Equipment",
                    subtitle = "Dumbbells, barbells, machines, etc.",
                    icon = Icons.Outlined.Construction,
                    onClick = onNavigateToEquipment
                )
                HorizontalDivider()
            }
            item {
                SettingsListItem(
                    title = "Manage Routines",
                    subtitle = "Create and organize your workout routines",
                    icon = Icons.AutoMirrored.Outlined.ListAlt,
                    onClick = onNavigateToRoutines
                )
            }
        }
    }
}

@Composable
fun SettingsListItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingContent = {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    WorkoutPlannerTheme {
        SettingsScreen(
            onNavigateToExercises = {},
            onNavigateToRoutines = {},
            onNavigateToEquipment = {},
            onNavigateToTimerSettings = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsListItemPreview() {
    WorkoutPlannerTheme {
        SettingsListItem(
            title = "Manage Exercises",
            subtitle = "Add, edit or delete exercises",
            icon = Icons.Outlined.FitnessCenter,
            onClick = {}
        )
    }
}
