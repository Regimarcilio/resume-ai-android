package com.resumeapp.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resumeapp.data.prefs.UserPrefs
import com.resumeapp.domain.model.SubscriptionTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings ViewModel - exposes subscription/consent state to the settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val tier = userPrefs.subscriptionTier
            _uiState.value = SettingsUiState(
                subscriptionTier = TierInfo(tier.displayName()),
                subscriptionStatus = if (userPrefs.subscriptionExpiryDate != null) {
                    "Ativa"
                } else {
                    "Inativa"
                },
                consentAccepted = userPrefs.consentAccepted,
                consentVersion = userPrefs.consentVersion
            )
        }
    }
}

data class SettingsUiState(
    val subscriptionTier: TierInfo = TierInfo("Grátis"),
    val subscriptionStatus: String = "Inativa",
    val consentAccepted: Boolean = false,
    val consentVersion: String? = null
)

data class TierInfo(val displayName: String)

private fun SubscriptionTier.displayName(): String = when (this) {
    SubscriptionTier.FREE -> "Grátis"
    SubscriptionTier.PRO -> "Pro"
    SubscriptionTier.LIFETIME -> "Vitalício"
    SubscriptionTier.ENTERPRISE -> "Enterprise"
}
