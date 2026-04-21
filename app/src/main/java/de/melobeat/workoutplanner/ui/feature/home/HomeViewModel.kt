package de.melobeat.workoutplanner.ui.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.melobeat.workoutplanner.data.WorkoutRepository
import de.melobeat.workoutplanner.data.WorkoutHistoryWithExercises
import de.melobeat.workoutplanner.domain.model.Routine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val selectedRoutine: Routine? = null,
    val recentHistory: List<WorkoutHistoryWithExercises> = emptyList(),
    val exerciseNameMap: Map<String, String> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getSelectedRoutineStream(),
        repository.getWorkoutHistoryStream(),
        repository.getExercisesStream()
    ) { routine, history, exercises ->
        HomeUiState(
            selectedRoutine = routine,
            recentHistory = history.take(5),
            exerciseNameMap = exercises.associate { it.id to it.name },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun updateNextDay(routineId: String, dayIndex: Int) {
        viewModelScope.launch {
            try {
                repository.updateLastCompletedDayIndex(routineId, dayIndex)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to update next day", e)
            }
        }
    }

}
