package de.melobeat.workoutplanner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.melobeat.workoutplanner.data.RestTimerPreferencesRepository
import de.melobeat.workoutplanner.data.RestTimerSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerSettingsViewModel @Inject constructor(
    private val timerPrefs: RestTimerPreferencesRepository
) : ViewModel() {

    val settings: StateFlow<RestTimerSettings> = timerPrefs.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RestTimerSettings())

    val themeMode: StateFlow<String> = timerPrefs.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "dark")

    fun setThemeMode(mode: String) {
        viewModelScope.launch { timerPrefs.setThemeMode(mode) }
    }

    fun updateEasyThreshold(seconds: Int) {
        viewModelScope.launch { timerPrefs.update(settings.value.copy(betweenSetsEasySeconds = seconds)) }
    }

    fun updateHardThreshold(seconds: Int) {
        viewModelScope.launch { timerPrefs.update(settings.value.copy(betweenSetsHardSeconds = seconds)) }
    }

    fun updateExerciseThreshold(seconds: Int) {
        viewModelScope.launch { timerPrefs.update(settings.value.copy(betweenExercisesSeconds = seconds)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TimerSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Timer Settings", fontWeight = FontWeight.Black) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TimerSettingRow(
                title = "Easy set rest",
                subtitle = "Notify when it's time for the next easy set",
                seconds = settings.betweenSetsEasySeconds,
                onValueChange = { viewModel.updateEasyThreshold(it) }
            )
            HorizontalDivider()
            TimerSettingRow(
                title = "Hard set rest",
                subtitle = "Notify when it's time for the next hard set",
                seconds = settings.betweenSetsHardSeconds,
                onValueChange = { viewModel.updateHardThreshold(it) }
            )
            HorizontalDivider()
            TimerSettingRow(
                title = "Between exercises",
                subtitle = "Notify when it's time for the next exercise",
                seconds = settings.betweenExercisesSeconds,
                onValueChange = { viewModel.updateExerciseThreshold(it) }
            )
        }
    }
}

@Composable
private fun TimerSettingRow(
    title: String,
    subtitle: String,
    seconds: Int,
    onValueChange: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Text(
                text = formatRestSeconds(seconds),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable { showDialog = true }
    )

    if (showDialog) {
        TimerEditDialog(
            title = title,
            currentSeconds = seconds,
            onDismiss = { showDialog = false },
            onConfirm = { newSeconds ->
                onValueChange(newSeconds)
                showDialog = false
            }
        )
    }
}

@Composable
private fun TimerEditDialog(
    title: String,
    currentSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var inputText by remember { mutableStateOf(currentSeconds.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("Enter duration in seconds:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it.filter { c -> c.isDigit() } },
                    singleLine = true,
                    suffix = { Text("s") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = inputText.toIntOrNull()
                if (v != null && v in 1..3600) onConfirm(v)
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun formatRestSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "$m:${s.toString().padStart(2, '0')}" else "${s}s"
}
