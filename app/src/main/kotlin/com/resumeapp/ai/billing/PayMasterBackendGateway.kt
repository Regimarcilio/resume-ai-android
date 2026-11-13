package com.resumeapp.ai.billing

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.resumeapp.domain.model.Result
import com.resumeapp.domain.repository.PaymentGateway
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PayMaster payment gateway.
 *
 * ## Why a backend and not a direct call
 * PayMaster authenticates with a merchant token (`Authorization: Bearer ...`).
 * Shipping that token inside the APK would expose it to anyone who unzips
 * the app. The production setup is therefore:
 *
 * ```
 * app  ──POST /checkout──►  backend (holds the PayMaster token)
 *  │                              │  creates the invoice at PayMaster
 *  │◄── checkout (paymentUrl) ───┘
 *  │                              │  PayMaster notifies the backend (webhook)
 *  └──GET /checkout/{id}─────────►┘  backend verifies the payment status
 * ```
 *
 * `backendBaseUrl` + `merchantId` are public configuration; the PayMaster
 * token never leaves the backend.
 *
 * When no backend is configured the gateway reports a clear error instead of
 * silently pretending the payment succeeded.
 */
@Singleton
class PayMasterBackendGateway @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) : PaymentGateway {

    override suspend fun plans(): List<PaymentGateway.Plan> = listOf(
        PaymentGateway.Plan(
            id = com.resumeapp.domain.model.FreemiumLimits.PLAN_ID_UNLOCK,
            title = com.resumeapp.domain.model.FreemiumLimits.PLAN_TITLE,
            description = com.resumeapp.domain.model.FreemiumLimits.PLAN_DESCRIPTION,
            priceCents = com.resumeapp.domain.model.FreemiumLimits.UNLOCK_PRICE_CENTS,
            currency = com.resumeapp.domain.model.FreemiumLimits.CURRENCY
        )
    )

    override suspend fun startCheckout(
        planId: String,
        userId: String
    ): Result<PaymentGateway.Checkout> {
        val baseUrl = config().backendBaseUrl
            ?: return Result.Failure(
                "Pagamento indisponível: configure PAYMASTER_BACKEND_URL para habilitar o checkout."
            )

        val body = gson.toJson(
            mapOf(
                "planId" to planId,
                "userId" to userId,
                "merchantId" to config().merchantId.orEmpty(),
                "idempotencyKey" to UUID.randomUUID().toString()
            )
        )

        return try {
            val request = Request.Builder()
                .url("$baseUrl/checkout")
                .header("Accept", "application/json")
                .post(body.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val payload = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return Result.Failure("Checkout falhou (HTTP ${response.code}): $payload")
                }
                val dto = gson.fromJson(payload, CheckoutResponse::class.java)
                Result.Success(
                    PaymentGateway.Checkout(
                        id = dto.id,
                        paymentUrl = dto.paymentUrl,
                        pixQrCode = dto.pixQrCode,
                        pixCopyPaste = dto.pixCopyCode,
                        expiresAtEpochMs = dto.expiresAt
                    )
                )
            }
        } catch (e: IOException) {
            Result.Failure("Falha de rede ao iniciar o pagamento: ${e.message}")
        }
    }

    override suspend fun checkoutStatus(checkoutId: String): Result<PaymentGateway.Status> {
        val baseUrl = config().backendBaseUrl
            ?: return Result.Failure("Pagamento indisponível: backend não configurado.")

        return try {
            val request = Request.Builder()
                .url("$baseUrl/checkout/$checkoutId")
                .header("Accept", "application/json")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val payload = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return Result.Failure("Consulta de status falhou (HTTP ${response.code})")
                }
                val dto = gson.fromJson(payload, StatusResponse::class.java)
                Result.Success(
                    PaymentGateway.Status(
                        checkoutId = dto.id ?: checkoutId,
                        state = when (dto.state?.uppercase()) {
                            "PAID", "COMPLETED", "CONFIRMED" -> PaymentGateway.Status.State.PAID
                            "FAILED", "CANCELED", "DECLINED" -> PaymentGateway.Status.State.FAILED
                            "EXPIRED" -> PaymentGateway.Status.State.EXPIRED
                            "PENDING", "CREATED" -> PaymentGateway.Status.State.PENDING
                            else -> PaymentGateway.Status.State.UNKNOWN
                        },
                        message = dto.message
                    )
                )
            }
        } catch (e: IOException) {
            Result.Failure("Falha de rede ao consultar o pagamento: ${e.message}")
        }
    }

    // -----------------------------------------------------------------------
    // Configuration
    // -----------------------------------------------------------------------

    private data class Config(
        val backendBaseUrl: String?,
        val merchantId: String?
    )

    /**
     * Reads the configuration from BuildConfig (values injected by
     * `gradle.properties`, never from the source tree).
     */
    private fun config(): Config = Config(
        backendBaseUrl = com.resumeapp.BuildConfig.PAYMASTER_BACKEND_URL.takeIf { it.isNotBlank() },
        merchantId = com.resumeapp.BuildConfig.PAYMASTER_MERCHANT_ID.takeIf { it.isNotBlank() }
    )

    // -----------------------------------------------------------------------
    // Wire DTOs
    // -----------------------------------------------------------------------

    private data class CheckoutResponse(
        val id: String = "",
        val paymentUrl: String = "",
        val pixQrCode: String? = null,
        @SerializedName("pixCopyCode") val pixCopyCode: String? = null,
        val expiresAt: Long? = null
    )

    private data class StatusResponse(
        val id: String? = null,
        val state: String? = null,
        val message: String? = null
    )

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
