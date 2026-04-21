package de.melobeat.workoutplanner.ui.feature.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.melobeat.workoutplanner.data.WorkoutRepository
import de.melobeat.workoutplanner.domain.model.Routine
import de.melobeat.workoutplanner.domain.model.WorkoutDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutinesUiState(
    val routines: List<Routine> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class RoutinesViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RoutinesUiState())
    val uiState: StateFlow<RoutinesUiState> = _state.asStateFlow()

    private val _detailRoutineId = MutableStateFlow<String?>(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    val detailRoutine: StateFlow<Routine?> = _detailRoutineId
        .flatMapLatest { id -> if (id != null) repository.getRoutineStream(id) else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            repository.getRoutinesStream().collect { routines ->
                _state.update { it.copy(routines = routines, isLoading = false) }
            }
        }
    }

    fun loadRoutineDetail(routineId: String) {
        _detailRoutineId.value = routineId
    }

    fun selectRoutine(routineId: String) {
        viewModelScope.launch {
            try {
                repository.selectRoutine(routineId)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to select routine") }
            }
        }
    }

    fun deleteRoutine(routineId: String) {
        viewModelScope.launch {
            try {
                repository.deleteRoutine(routineId)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to delete routine") }
            }
        }
    }

    private val _saveComplete = MutableStateFlow(false)
    val saveComplete: StateFlow<Boolean> = _saveComplete.asStateFlow()

    fun saveRoutine(name: String, description: String, days: List<WorkoutDay>, existingId: String?) {
        viewModelScope.launch {
            try {
                repository.saveRoutine(name, description, days, existingId)
                _saveComplete.value = true
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to save routine") }
            }
        }
    }

    fun onSaveHandled() {
        _saveComplete.value = false
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
