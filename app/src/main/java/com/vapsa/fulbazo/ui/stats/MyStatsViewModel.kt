package com.vapsa.fulbazo.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vapsa.fulbazo.di.AppContainer
import com.vapsa.fulbazo.domain.model.FieldSkill
import com.vapsa.fulbazo.domain.model.Foot
import com.vapsa.fulbazo.domain.model.GoalkeeperSkill
import com.vapsa.fulbazo.domain.model.Position
import com.vapsa.fulbazo.domain.model.SelfAssessment
import com.vapsa.fulbazo.domain.model.SkillScale
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.StatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyStatsViewModel(
    private val authRepository: AuthRepository,
    private val statsRepository: StatsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyStatsUiState())
    val uiState: StateFlow<MyStatsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Carga la autoevaluación guardada para rellenar el formulario. */
    fun load() {
        val userId = authRepository.currentUser.value?.id
        if (userId == null) {
            _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            return
        }
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            val saved = runCatching { statsRepository.observeProfile(userId).first() }
                .getOrElse {
                    _uiState.update { state -> state.copy(isLoading = false, loadFailed = true) }
                    return@launch
                }
                .selfAssessment

            _uiState.update { state ->
                if (saved == null) {
                    state.copy(isLoading = false)
                } else {
                    state.copy(
                        isLoading = false,
                        dominantFoot = saved.dominantFoot,
                        mainPosition = saved.mainPosition,
                        secondaryPosition = saved.secondaryPosition,
                        fieldSkills = state.fieldSkills + saved.fieldSkills,
                        goalkeeperSkills = state.goalkeeperSkills + saved.goalkeeperSkills.orEmpty(),
                    )
                }
            }
        }
    }

    fun onDominantFootChange(foot: Foot) {
        _uiState.update { it.copy(dominantFoot = foot, footMissing = false, isSaved = false) }
    }

    fun onMainPositionChange(position: Position) {
        _uiState.update {
            it.copy(
                mainPosition = position,
                // La secundaria no puede repetir la principal.
                secondaryPosition = it.secondaryPosition.takeIf { secondary -> secondary != position },
                positionMissing = false,
                isSaved = false,
            )
        }
    }

    /** `null` quita la posición secundaria. */
    fun onSecondaryPositionChange(position: Position?) {
        _uiState.update {
            if (position != null && position == it.mainPosition) it
            else it.copy(secondaryPosition = position, isSaved = false)
        }
    }

    fun onFieldSkillChange(skill: FieldSkill, value: Int) {
        _uiState.update {
            it.copy(fieldSkills = it.fieldSkills + (skill to SkillScale.clamp(value)), isSaved = false)
        }
    }

    fun onGoalkeeperSkillChange(skill: GoalkeeperSkill, value: Int) {
        _uiState.update {
            it.copy(
                goalkeeperSkills = it.goalkeeperSkills + (skill to SkillScale.clamp(value)),
                isSaved = false,
            )
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return
        val userId = authRepository.currentUser.value?.id ?: return

        val foot = state.dominantFoot
        val position = state.mainPosition
        if (foot == null || position == null) {
            _uiState.update { it.copy(footMissing = foot == null, positionMissing = position == null) }
            return
        }

        val assessment = SelfAssessment(
            dominantFoot = foot,
            mainPosition = position,
            secondaryPosition = state.secondaryPosition,
            fieldSkills = state.fieldSkills,
            goalkeeperSkills = state.goalkeeperSkills.takeIf { state.showGoalkeeperSkills },
        )

        _uiState.update { it.copy(isSaving = true, saveFailed = false, isSaved = false) }
        viewModelScope.launch {
            val result = statsRepository.saveSelfAssessment(userId, assessment)
            _uiState.update {
                it.copy(
                    isSaving = false,
                    isSaved = result.isSuccess,
                    saveFailed = result.isFailure,
                )
            }
        }
    }

    /** La vista llama a esto después de mostrar la confirmación o el error. */
    fun onSaveMessageShown() {
        _uiState.update { it.copy(isSaved = false, saveFailed = false) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MyStatsViewModel(AppContainer.authRepository, AppContainer.statsRepository)
            }
        }
    }
}
