package com.resumeapp.features.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.resumeapp.domain.model.ConsentPurpose
import com.resumeapp.domain.model.ConsentRecord
import com.resumeapp.domain.usecase.onboarding.RecordConsentUseCase
import com.resumeapp.domain.usecase.onboarding.CompleteOnboardingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val recordConsentUseCase: RecordConsentUseCase,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(OnboardingUiState.Welcome)
    val uiState: kotlinx.coroutines.flow.StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // Current step tracking
    private var currentStep = 0
    private val selectedTemplateId = MutableStateFlow<String?>(null)
    private val consentPurposes = MutableStateFlow<Set<ConsentPurpose>>(setOf(ConsentPurpose.GENERATE_RESUME)) // Mandatory

    // Actions
    fun onWelcomeNext() {
        _uiState.value = OnboardingUiState.Policy
    }

    fun onPolicyAccepted() {
        _uiState.value = OnboardingUiState.Consent
    }

    fun onConsentPurposeToggled(purpose: ConsentPurpose, checked: Boolean) {
        val current = consentPurposes.value.toMutableSet()
        if (checked) current.add(purpose) else current.remove(purpose)
        consentPurposes.value = current
    }

    fun onConsentAccepted() {
        if (consentPurposes.value.isEmpty()) return // At least GENERATE_RESUME required
        
        val record = ConsentRecord(
            version = "2026.11.07-v1",
            timestamp = System.currentTimeMillis(),
            policyHash = "sha256:policy_hash_here", // TODO: compute from actual policy
            accepted = true,
            purposes = consentPurposes.value
        )
        
        viewModelScope.launch {
            recordConsentUseCase(record)
            _uiState.value = OnboardingUiState.TemplatePicker
        }
    }

    fun onTemplateSelected(templateId: String) {
        selectedTemplateId.value = templateId
    }

    fun onCompleteOnboarding() {
        val templateId = selectedTemplateId.value ?: return
        viewModelScope.launch {
            completeOnboardingUseCase(templateId)
            _uiState.value = OnboardingUiState.Complete
        }
    }

    fun onSkipTemplatePicker() {
        _uiState.value = OnboardingUiState.Complete
    }
}

// UI States
sealed interface OnboardingUiState {
    data class Welcome(val showPolicy: Boolean = false) : OnboardingUiState
    object Policy : OnboardingUiState
    object Consent : OnboardingUiState
    data class TemplatePicker(val templates: List<TemplateOption> = emptyList()) : OnboardingUiState
    object Complete : OnboardingUiState
    data class Error(val message: String) : OnboardingUiState

    // Companion factories
    companion object {
        val Welcome = Welcome()
        val Policy = Policy()
        val Consent = Consent()
        val Complete = Complete()
    }
}

data class TemplateOption(
    val id: String,
    val name: String,
    val description: String,
    val previewImageUrl: String,
    val isPremium: Boolean
)