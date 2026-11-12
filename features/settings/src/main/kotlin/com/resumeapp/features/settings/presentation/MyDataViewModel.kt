package com.resumeapp.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resumeapp.data.prefs.UserPrefs
import com.resumeapp.domain.repository.ResumeRepository
import com.resumeapp.domain.usecase.IdParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * My Data ViewModel - LGPD Art. 18 rights: export, anonymize and delete.
 */
@HiltViewModel
class MyDataViewModel @Inject constructor(
    private val resumeRepository: ResumeRepository,
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyDataUiState())
    val uiState: StateFlow<MyDataUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val userId = userPrefs.userId
            val resumes = resumeRepository.getAllByUserId(userId)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                resumeCount = resumes.size,
                consentAccepted = userPrefs.consentAccepted,
                consentVersion = userPrefs.consentVersion,
                consentTimestamp = userPrefs.consentTimestamp
            )
        }
    }

    /** Art. 18, II - export all personal data as a structured payload. */
    fun exportData(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val userId = userPrefs.userId
            val resumes = resumeRepository.getAllByUserId(userId)
            val payload = buildString {
                appendLine("{")
                appendLine("  \"exportedAt\": ${System.currentTimeMillis()},")
                appendLine("  \"userId\": \"$userId\",")
                appendLine("  \"resumes\": ${resumes.size},")
                appendLine("  \"consentAccepted\": ${userPrefs.consentAccepted},")
                appendLine("  \"consentVersion\": \"${userPrefs.consentVersion}\"")
                appendLine("}")
            }
            _uiState.value = _uiState.value.copy(message = "Dados exportados com sucesso")
            onResult(payload)
        }
    }

    /** Art. 18, III/V - anonymize locally stored personal data. */
    fun anonymizeData() {
        viewModelScope.launch {
            val userId = userPrefs.userId
            val resumes = resumeRepository.getAllByUserId(userId)
            resumes.forEach { resume ->
                val anonymized = resume.copy(
                    personalInfo = resume.personalInfo.copy(
                        fullName = "Usuário Anonimizado",
                        email = "anonimo@local.invalid",
                        phone = "",
                        location = ""
                    )
                )
                resumeRepository.save(anonymized)
            }
            _uiState.value = _uiState.value.copy(message = "Dados anonimizados")
        }
    }

    /** Art. 18, V - delete all personal data. */
    fun deleteAllData() {
        viewModelScope.launch {
            val userId = userPrefs.userId
            val resumes = resumeRepository.getAllByUserId(userId)
            resumes.forEach { resume -> resumeRepository.deleteById(resume.id) }
            userPrefs.clearConsent()
            _uiState.value = _uiState.value.copy(
                message = "Dados excluídos",
                resumeCount = 0,
                consentAccepted = false
            )
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

data class MyDataUiState(
    val isLoading: Boolean = true,
    val resumeCount: Int = 0,
    val consentAccepted: Boolean = false,
    val consentVersion: String? = null,
    val consentTimestamp: Long? = null,
    val message: String? = null
)
