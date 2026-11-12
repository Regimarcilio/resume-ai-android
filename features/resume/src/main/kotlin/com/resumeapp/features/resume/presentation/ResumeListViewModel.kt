package com.resumeapp.features.resume.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resumeapp.domain.model.Result
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.usecase.resume.CreateResumeUseCase
import com.resumeapp.domain.usecase.resume.DeleteResumeUseCase
import com.resumeapp.domain.usecase.resume.GetAllResumesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ResumeListViewModel @Inject constructor(
    private val getAllResumesUseCase: GetAllResumesUseCase,
    private val createResumeUseCase: CreateResumeUseCase,
    private val deleteResumeUseCase: DeleteResumeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResumeListUiState>(ResumeListUiState.Loading())
    val uiState: StateFlow<ResumeListUiState> = _uiState.asStateFlow()

    init {
        loadResumes()
    }

    private fun loadResumes() {
        viewModelScope.launch {
            _uiState.value = ResumeListUiState.Loading()
            val result = getAllResumesUseCase(
                com.resumeapp.domain.usecase.SearchParams(
                    filters = mapOf("userId" to DEFAULT_USER_ID)
                )
            )
            _uiState.value = when (result) {
                is Result.Success -> ResumeListUiState.Content(
                    resumes = result.value.map { resume ->
                        ResumeListItem(
                            id = resume.id,
                            name = resume.personalInfo.fullName.ifEmpty { "Currículo sem nome" },
                            templateName = resume.templateId,
                            updatedAt = formatDate(resume.updatedAt),
                            sectionsCount = resume.sections.size
                        )
                    }
                )
                is Result.Failure -> ResumeListUiState.Error(result.errorMessage)
            }
        }
    }

    fun onNewResume() {
        // Navigation handled by screen
    }

    fun onDeleteResume(resumeId: String) {
        viewModelScope.launch {
            val result = deleteResumeUseCase(com.resumeapp.domain.usecase.IdParams(resumeId))
            if (result is Result.Success) {
                loadResumes()
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        return java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
    }

    companion object {
        /** Single-user build: LGPD consent is per-device, not per-account. */
        const val DEFAULT_USER_ID = "local_user"
    }
}

sealed interface ResumeListUiState {
    data class Loading(val placeholder: Unit = Unit) : ResumeListUiState
    data class Content(val resumes: List<ResumeListItem>) : ResumeListUiState
    data class Error(val message: String) : ResumeListUiState
}

data class ResumeListItem(
    val id: String,
    val name: String,
    val templateName: String,
    val updatedAt: String,
    val sectionsCount: Int
)