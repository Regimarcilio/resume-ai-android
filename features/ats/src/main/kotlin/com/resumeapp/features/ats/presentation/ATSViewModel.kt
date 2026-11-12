package com.resumeapp.features.ats.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resumeapp.domain.model.ATSScore
import com.resumeapp.domain.model.ATSSuggestion
import com.resumeapp.domain.model.Result
import com.resumeapp.domain.usecase.IdParams
import com.resumeapp.domain.usecase.ats.AnalyzeATSUseCase
import com.resumeapp.domain.usecase.ats.ApplyATSSuggestionUseCase
import com.resumeapp.domain.usecase.ats.GetATSScoreUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ATS Analysis ViewModel
 * Runs the ATS scoring pipeline and exposes the result to [ATSScreen].
 */
class ATSViewModel @Inject constructor(
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
    private val analyzeATSUseCase: AnalyzeATSUseCase,
    private val getATSScoreUseCase: GetATSScoreUseCase,
    private val applyATSSuggestionUseCase: ApplyATSSuggestionUseCase
) : ViewModel() {

    private val resumeId: String = savedStateHandle.get<String>("resumeId") ?: ""

    private val _uiState = MutableStateFlow<ATSUiState>(ATSUiState.Loading())
    val uiState: StateFlow<ATSUiState> = _uiState.asStateFlow()

    init {
        loadScore()
    }

    fun loadScore() {
        viewModelScope.launch {
            _uiState.value = ATSUiState.Loading()
            when (val result = getATSScoreUseCase(IdParams(resumeId))) {
                is Result.Success -> _uiState.value = ATSUiState.Content(result.value)
                is Result.Failure -> analyze()
            }
        }
    }

    fun analyze(jobDescription: String? = null) {
        viewModelScope.launch {
            _uiState.value = ATSUiState.Analyzing()
            when (val result = analyzeATSUseCase(AnalyzeATSUseCase.Params(resumeId, jobDescription, true))) {
                is Result.Success -> _uiState.value = ATSUiState.Content(result.value)
                is Result.Failure -> _uiState.value = ATSUiState.Error(result.errorMessage)
            }
        }
    }

    fun applySuggestion(suggestion: ATSSuggestion) {
        viewModelScope.launch {
            when (val result = applyATSSuggestionUseCase(ApplyATSSuggestionUseCase.Params(resumeId, suggestion))) {
                is Result.Success -> loadScore()
                is Result.Failure -> _uiState.value = ATSUiState.Error(result.errorMessage)
            }
        }
    }
}

/**
 * ATS Screen state.
 */
sealed interface ATSUiState {
    data class Loading(val placeholder: Unit = Unit) : ATSUiState
    data class Analyzing(val placeholder: Unit = Unit) : ATSUiState
    data class Error(val message: String) : ATSUiState
    data class Content(val score: ATSScore) : ATSUiState
}
