package com.resumeapp.domain.usecase.subscription

import com.resumeapp.domain.model.FreemiumLimits
import com.resumeapp.domain.model.Result
import com.resumeapp.domain.model.SubscriptionStatus
import com.resumeapp.domain.model.SubscriptionTier
import com.resumeapp.domain.repository.PaymentGateway
import kotlinx.coroutines.delay

/**
 * Buy the unlock plan (evaluation mode -> unlimited resumes).
 *
 * The flow is: create the checkout at PayMaster, the user pays (browser or
 * PIX), the app polls the status and, once PAID, the subscription is stored
 * locally.
 */
class StartCheckoutUseCase constructor(
    private val paymentGateway: PaymentGateway,
    private val onPaid: suspend (String) -> Unit
) {
    suspend operator fun invoke(params: Params): Result<PaymentGateway.Checkout> {
        return try {
            val result = paymentGateway.startCheckout(params.planId, params.userId)
            when (result) {
                is Result.Success -> Result.Success(result.value)
                is Result.Failure -> Result.Failure(result.errorMessage)
            }
        } catch (e: Exception) {
            Result.Failure(e.message ?: "Falha ao iniciar o pagamento")
        }
    }

    data class Params(
        val planId: String = FreemiumLimits.PLAN_ID_UNLOCK,
        val userId: String
    )
}

/**
 * Polls the checkout until it is paid, expired or the attempts run out.
 */
class AwaitPaymentUseCase constructor(
    private val paymentGateway: PaymentGateway
) {
    suspend operator fun invoke(params: Params): Result<PaymentGateway.Status> {
        var attempts = 0
        while (attempts < MAX_ATTEMPTS) {
            val result = paymentGateway.checkoutStatus(params.checkoutId)
            if (result is Result.Success) {
                when (result.value.state) {
                    PaymentGateway.Status.State.PAID -> return result
                    PaymentGateway.Status.State.FAILED,
                    PaymentGateway.Status.State.EXPIRED -> return result
                    else -> Unit
                }
            } else if (result is Result.Failure) {
                return result
            }
            attempts++
            delay(POLL_INTERVAL_MS)
        }
        return Result.Failure("Tempo de pagamento esgotado. Tente novamente.")
    }

    data class Params(val checkoutId: String)

    companion object {
        const val MAX_ATTEMPTS = 30
        const val POLL_INTERVAL_MS = 2000L
    }
}

/**
 * Applies the unlocked state after a confirmed payment.
 */
class ApplySubscriptionUseCase constructor(
    private val saveStatus: suspend (SubscriptionStatus) -> Unit
) {
    suspend operator fun invoke(params: Params = Params()): Result<Unit> {
        return try {
            saveStatus(
                SubscriptionStatus(
                    tier = SubscriptionTier.PRO,
                    isActive = true,
                    purchaseToken = params.purchaseToken
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e.message ?: "Falha ao salvar a assinatura")
        }
    }

    data class Params(val purchaseToken: String? = null)
}
