package com.resumeapp.domain.repository

import com.resumeapp.domain.model.Result

/**
 * Payment gateway abstraction.
 *
 * The concrete implementation lives in the app module (it needs the network
 * stack and the merchant configuration). The domain only knows the contract,
 * so the freemium rules can be enforced without any Android dependency.
 *
 * ## PayMaster
 * The production gateway is PayMaster (API v2):
 *  - `POST /api/v2/invoices` creates an invoice and returns a payment URL
 *  - `GET  /api/v2/payments/{id}` returns the payment status
 *  - Auth: `Authorization: Bearer {token}` + `Idempotency-Key` on POST
 *
 * The merchant token **must not ship inside the APK**: it is held by a small
 * backend that creates the invoice and verifies the payment (webhook), and the
 * app talks to that backend. See [PayMasterBackendGateway] in the app module.
 */
interface PaymentGateway {

    /** Plans the user can buy. */
    data class Plan(
        val id: String,
        val title: String,
        val description: String,
        val priceCents: Int,
        val currency: String = "BRL"
    ) {
        fun priceFormatted(): String =
            "R$ ${priceCents / 100},${(priceCents % 100).toString().padStart(2, '0')}"
    }

    /** A checkout session ready to be presented to the user. */
    data class Checkout(
        val id: String,
        val paymentUrl: String,
        val pixQrCode: String? = null,
        val pixCopyPaste: String? = null,
        val expiresAtEpochMs: Long? = null
    )

    /** Current status of a checkout. */
    data class Status(
        val checkoutId: String,
        val state: State,
        val message: String? = null
    ) {
        enum class State { PENDING, PAID, FAILED, EXPIRED, UNKNOWN }
    }

    /** The plans offered to the user. */
    suspend fun plans(): List<Plan>

    /**
     * Starts a checkout for [planId].
     * Returns the payment URL (and PIX payload, when available) to present.
     */
    suspend fun startCheckout(planId: String, userId: String): Result<Checkout>

    /** Polls the status of a checkout. */
    suspend fun checkoutStatus(checkoutId: String): Result<Status>
}
