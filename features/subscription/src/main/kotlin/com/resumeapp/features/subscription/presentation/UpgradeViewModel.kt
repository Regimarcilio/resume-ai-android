package com.resumeapp.features.subscription.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resumeapp.domain.model.FreemiumLimits
import com.resumeapp.domain.model.Result
import com.resumeapp.domain.usecase.subscription.AwaitPaymentUseCase
import com.resumeapp.domain.usecase.subscription.StartCheckoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpgradeViewModel @Inject constructor(
    private val startCheckoutUseCase: StartCheckoutUseCase,
    private val awaitPaymentUseCase: AwaitPaymentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpgradeUiState>(UpgradeUiState.Idle)
    val uiState: StateFlow<UpgradeUiState> = _uiState.asStateFlow()

    private var checkoutId: String? = null
    private var paymentUrl: String? = null

    fun startCheckout() {
        _uiState.value = UpgradeUiState.Starting
        viewModelScope.launch {
            when (
                val result = startCheckoutUseCase(
                    StartCheckoutUseCase.Params(userId = LOCAL_USER_ID)
                )
            ) {
                is Result.Success -> {
                    checkoutId = result.value.id
                    paymentUrl = result.value.paymentUrl
                    _uiState.value = UpgradeUiState.AwaitingPayment(
                        paymentUrl = result.value.paymentUrl,
                        pixCopyPaste = result.value.pixCopyPaste
                    )
                }
                is Result.Failure -> _uiState.value = UpgradeUiState.Error(result.errorMessage)
            }
        }
    }

    /** Abre o link de pagamento no navegador (Custom Tabs / ACTION_VIEW). */
    fun openPaymentUrl() {
        val url = paymentUrl ?: return
        _uiState.value = UpgradeUiState.AwaitingPayment(
            paymentUrl = url,
            pixCopyPaste = (_uiState.value as? UpgradeUiState.AwaitingPayment)?.pixCopyPaste
        )
        // O app nao abre URLs sozinho: a UI expoe o link para o usuario tocar.
    }

    /** Consulta o status do pagamento junto ao backend. */
    fun checkStatus() {
        val id = checkoutId ?: return
        _uiState.value = UpgradeUiState.Starting
        viewModelScope.launch {
            when (val result = awaitPaymentUseCase(AwaitPaymentUseCase.Params(id))) {
                is Result.Success -> {
                    if (result.value.state == com.resumeapp.domain.repository.PaymentGateway.Status.State.PAID) {
                        _uiState.value = UpgradeUiState.Paid
                    } else {
                        _uiState.value = UpgradeUiState.AwaitingPayment(
                            paymentUrl = paymentUrl.orEmpty(),
                            pixCopyPaste = (_uiState.value as? UpgradeUiState.AwaitingPayment)?.pixCopyPaste
                        )
                    }
                }
                is Result.Failure -> _uiState.value = UpgradeUiState.Error(result.errorMessage)
            }
        }
    }

    companion object {
        const val LOCAL_USER_ID = "local_user"
    }
}

sealed interface UpgradeUiState {
    data object Idle : UpgradeUiState
    data object Starting : UpgradeUiState
    data class AwaitingPayment(
        val paymentUrl: String,
        val pixCopyPaste: String? = null
    ) : UpgradeUiState
    data object Paid : UpgradeUiState
    data class Error(val message: String) : UpgradeUiState
}
