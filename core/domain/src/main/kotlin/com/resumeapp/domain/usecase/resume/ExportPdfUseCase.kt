package com.resumeapp.domain.usecase.resume

import com.resumeapp.domain.model.FreemiumLimits
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.model.Result
import com.resumeapp.domain.model.SubscriptionStatus
import com.resumeapp.domain.model.SubscriptionTier
import com.resumeapp.domain.repository.PdfRepository
import com.resumeapp.domain.repository.TemplateRepository

/**
 * Exports a resume to PDF.
 *
 * The free tier exports with a watermark; the paid plan does not
 * (see [FreemiumLimits] and the `NO_WATERMARK` feature flag).
 */
class ExportPdfUseCase constructor(
    private val pdfRepository: PdfRepository,
    private val templateRepository: TemplateRepository,
    private val subscriptionStatusProvider: suspend () -> SubscriptionStatus = { SubscriptionStatus() }
) {
    suspend operator fun invoke(params: Params): Result<ByteArray> {
        return try {
            val status = subscriptionStatusProvider()
            val isUnlocked = status.isActive && status.tier != SubscriptionTier.FREE
            val withWatermark = !isUnlocked

            val templateId = params.templateId
                ?: templateRepository.getAllActive().firstOrNull()?.id
                ?: "modern_professional"

            val pdf = pdfRepository.generatePdf(
                resume = params.resume,
                template = templateId,
                watermark = withWatermark
            )
            Result.Success(pdf)
        } catch (e: Exception) {
            Result.Failure(e.message ?: "Falha ao exportar o PDF")
        }
    }

    data class Params(
        val resume: Resume,
        val templateId: String? = null
    )
}
