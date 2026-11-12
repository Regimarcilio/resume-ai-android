package com.resumeapp.features.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.resumeapp.domain.model.ConsentPurpose
import com.resumeapp.domain.model.ConsentRecord
import com.resumeapp.domain.usecase.onboarding.RecordConsentUseCase
import com.resumeapp.domain.usecase.onboarding.CompleteOnboardingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val recordConsentUseCase: RecordConsentUseCase,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Welcome())
    val uiState: kotlinx.coroutines.flow.StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // Current step tracking
    private var currentStep = 0
    // Preseleciona o template padrao: sem isso "Confirmar" nao fazia nada
    // (o id era null e o use case retornava silenciosamente).
    val selectedTemplateId: MutableStateFlow<String?> =
        MutableStateFlow(TemplateOption.DEFAULT_TEMPLATE_ID)
    private val consentPurposes = MutableStateFlow<Set<ConsentPurpose>>(
        setOf(ConsentPurpose.GENERATE_RESUME, ConsentPurpose.AI_ASSIST, ConsentPurpose.ATS_SCORING)
    ) // Mandatory + opcionais previamente marcadas

    // Actions
    fun onWelcomeNext() {
        _uiState.value = OnboardingUiState.Policy
    }

    fun onPolicyAccepted() {
        _uiState.value = OnboardingUiState.Consent(consentPurposes.value)
    }

    fun onConsentPurposeToggled(purpose: ConsentPurpose, checked: Boolean) {
        // A finalidade mandatoria (GENERATE_RESUME) nao pode ser removida.
        if (!checked && purpose == ConsentPurpose.GENERATE_RESUME) return

        val current = consentPurposes.value.toMutableSet()
        if (checked) current.add(purpose) else current.remove(purpose)
        consentPurposes.value = current

        // Reflete a mudanca no estado da UI (antes os checkboxes ficavam fixos).
        val state = _uiState.value
        if (state is OnboardingUiState.Consent) {
            _uiState.value = state.copy(purposes = current)
        }
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
            recordConsentUseCase(RecordConsentUseCase.Params(record))
            _uiState.value = OnboardingUiState.TemplatePicker()
        }
    }

    fun onTemplateSelected(templateId: String) {
        selectedTemplateId.value = templateId
    }

    fun onCompleteOnboarding() {
        val templateId = selectedTemplateId.value ?: return
        viewModelScope.launch {
            completeOnboardingUseCase(CompleteOnboardingUseCase.Params(templateId))
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
    data class Consent(val purposes: Set<ConsentPurpose>) : OnboardingUiState
    data class TemplatePicker(val templates: List<TemplateOption> = emptyList()) : OnboardingUiState
    object Complete : OnboardingUiState
    data class Error(val message: String) : OnboardingUiState
}

data class TemplateOption(
    val id: String,
    val name: String,
    val description: String,
    val previewImageUrl: String,
    val isPremium: Boolean
) {
    companion object {
        /** Offline-first defaults; the catalogue can be replaced by the remote sync later. */
        val defaultTemplates: List<TemplateOption> = listOf(
            TemplateOption(
                id = "modern_professional",
                name = "Moderno Profissional",
                description = "Layout limpo, otimizado para ATS, ideal para áreas de tecnologia.",
                previewImageUrl = "",
                isPremium = false
            ),
            TemplateOption(
                id = "classic",
                name = "Clássico",
                description = "Formatação tradicional, adequada para áreas conservadoras.",
                previewImageUrl = "",
                isPremium = false
            ),
            TemplateOption(
                id = "creative",
                name = "Criativo",
                description = "Destaques visuais para áreas de design e marketing.",
                previewImageUrl = "",
                isPremium = true
            ),
            TemplateOption(
                id = "ats_optimized",
                name = "Otimizado ATS",
                description = "Estrutura recomendada por sistemas de triagem automática.",
                previewImageUrl = "",
                isPremium = false
            )
        )

        /** Template usado por padrão e em novos currículos. */
        const val DEFAULT_TEMPLATE_ID = "modern_professional"
    }
}