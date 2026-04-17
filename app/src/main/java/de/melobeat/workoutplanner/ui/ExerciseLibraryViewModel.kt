package de.melobeat.workoutplanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.melobeat.workoutplanner.data.WorkoutRepository
import de.melobeat.workoutplanner.model.Equipment
import de.melobeat.workoutplanner.model.Exercise
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseLibraryUiState(
    val exercises: List<Exercise> = emptyList(),
    val equipment: List<Equipment> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExerciseLibraryUiState())
    val uiState: StateFlow<ExerciseLibraryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getExercisesStream(),
                repository.getEquipmentStream()
            ) { exercises, equipment ->
                exercises to equipment
            }.collect { (exercises, equipment) ->
                _state.update { it.copy(exercises = exercises, equipment = equipment, isLoading = false) }
            }
        }
    }

    fun saveExercise(
        name: String,
        muscleGroup: String,
        description: String,
        equipmentId: String?,
        existingId: String?,
        isBodyweight: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                repository.saveExercise(name, muscleGroup, description, equipmentId, existingId, isBodyweight)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to save exercise") }
            }
        }
    }

    fun deleteExercise(exerciseId: String) {
        viewModelScope.launch {
            try {
                repository.deleteExercise(exerciseId)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to delete exercise") }
            }
        }
    }

    fun saveEquipment(name: String, existingId: String?, defaultWeight: Double? = null, weightStep: Double = 2.5) {
        viewModelScope.launch {
            try {
                repository.saveEquipment(name, existingId, defaultWeight, weightStep)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to save equipment") }
            }
        }
    }

    fun deleteEquipment(equipmentId: String) {
        viewModelScope.launch {
            try {
                repository.deleteEquipment(equipmentId)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to delete equipment") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
