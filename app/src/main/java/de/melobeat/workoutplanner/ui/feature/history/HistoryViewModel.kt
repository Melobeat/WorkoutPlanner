package de.melobeat.workoutplanner.ui.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.melobeat.workoutplanner.data.WorkoutHistoryWithExercises
import de.melobeat.workoutplanner.data.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HistoryUiState(
    val sessions: List<WorkoutHistoryWithExercises> = emptyList(),
    val exerciseNameMap: Map<String, String> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: WorkoutRepository
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.getWorkoutHistoryStream(),
        repository.getExercisesStream()
    ) { history, exercises ->
        HistoryUiState(
            sessions = history,
            exerciseNameMap = exercises.associate { it.id to it.name },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState()
    )
}
