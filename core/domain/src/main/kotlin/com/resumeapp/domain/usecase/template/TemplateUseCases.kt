package com.resumeapp.domain.usecase.template

import com.resumeapp.domain.model.Result
import com.resumeapp.domain.model.Result.Success
import com.resumeapp.domain.model.Result.Failure
import com.resumeapp.domain.model.Template
import com.resumeapp.domain.repository.TemplateRepository
import com.resumeapp.domain.usecase.AsyncUseCase
import com.resumeapp.domain.usecase.IdParams
import com.resumeapp.domain.usecase.SearchParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Get All Templates UseCase
 * Retrieves all active templates, optionally filtered by category
 */
class GetTemplatesUseCase @Inject constructor(
    private val templateRepository: TemplateRepository
) : AsyncUseCase<GetTemplatesUseCase.Params, List<com.resumeapp.domain.model.Template>> {

    @Serializable
    data class Params(
        val category: com.resumeapp.domain.model.TemplateCategory? = null,
        val premiumOnly: Boolean = false,
        val freeOnly: Boolean = false
    )

    override fun invoke(params: Params): Result<List<com.resumeapp.domain.model.Template>> {
        return try {
            val templates = when {
                params.category != null -> templateRepository.getByCategory(params.category!!)
                params.premiumOnly -> templateRepository.getPremiumTemplates()
                params.freeOnly -> templateRepository.getFreeTemplates()
                else -> templateRepository.getAllActive()
            }
            Result.Success(templates)
        } catch (e: Exception) {
            Result.failure(e.message ?: e.javaClass.simpleName)
        }
    }
}

/**
 * Get Template by ID UseCase
 * Retrieves a specific template by ID
 */
class GetTemplateByIdUseCase @Inject constructor(
    private val templateRepository: TemplateRepository
) : AsyncUseCase<com.resumeapp.domain.usecase.IdParams, com.resumeapp.domain.model.Template> {

    override fun invoke(params: com.resumeapp.domain.usecase.IdParams): Result<com.resumeapp.domain.model.Template> {
        return try {
            templateRepository.getById(params.id)
                ?.let { Result.Success(it) }
                ?: Result.failure("Template not found: ${params.id}")
        } catch (e: Exception) {
            Result.failure(e.message ?: e.javaClass.simpleName)
        }
    }
}

/**
 * Preview Template UseCase
 * Generates a preview of a template with sample data
 */
class PreviewTemplateUseCase @Inject constructor(
    private val templateRepository: TemplateRepository
) : AsyncUseCase<PreviewTemplateUseCase.Params, PreviewTemplateUseCase.PreviewResult> {

    @Serializable
    data class Params(
        val templateId: String,
        val sampleData: Boolean = true
    )

    @Serializable
    data class PreviewResult(
        val template: com.resumeapp.domain.model.Template,
        val previewImageUrl: String,
        val samplePdfBytes: ByteArray? = null
    )

    override fun invoke(params: Params): Result<PreviewResult> {
        return try {
            val template = templateRepository.getById(params.templateId)
                ?: return Result.failure("Template not found: ${params.templateId}")

            // In real implementation, would generate PDF preview
            val result = PreviewResult(
                template = template,
                previewImageUrl = template.previewImageUrl,
                samplePdfBytes = if (params.sampleData) generateSamplePdf(template) else null
            )
            
            Result.Success(result)
        } catch (e: Exception) {
            Result.failure(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun generateSamplePdf(template: com.resumeapp.domain.model.Template): ByteArray {
        // Placeholder - would integrate with PDF generator
        return ByteArray(0)
    }
}

/**
 * Select Template UseCase
 * Applies a template to a resume
 */
class SelectTemplateUseCase @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val resumeRepository: com.resumeapp.domain.repository.ResumeRepository
) : AsyncUseCase<SelectTemplateUseCase.Params, com.resumeapp.domain.model.Resume> {

    @Serializable
    data class Params(
        val resumeId: String,
        val templateId: String
    )

    override fun invoke(params: Params): Result<com.resumeapp.domain.model.Resume> {
        return try {
            val template = templateRepository.getById(params.templateId)
                ?: return Result.failure("Template not found: ${params.templateId}")

            val resume = resumeRepository.getById(params.resumeId)
                ?: return Result.failure("Resume not found: ${params.resumeId}")

            val updated = resume.copy(
                templateId = params.templateId,
                updatedAt = System.currentTimeMillis(),
                version = resume.version + 1
            )

            // TODO: Apply template config to resume sections if needed
            
            val updatedResume = resume.copy(
                templateId = params.templateId,
                updatedAt = System.currentTimeMillis(),
                version = resume.version + 1
            )

            val saved = resumeRepository.update(updatedResume)
            Result.Success(saved)
        } catch (e: Exception) {
            Result.failure(e.message ?: e.javaClass.simpleName)
        }
    }
}

/**
 * Get Templates by Category UseCase
 * Convenience method for getting templates by category
 */
class GetTemplatesByCategoryUseCase @Inject constructor(
    private val templateRepository: TemplateRepository
) : AsyncUseCase<GetTemplatesByCategoryUseCase.Params, List<com.resumeapp.domain.model.Template>> {

    @Serializable
    data class Params(
        val category: com.resumeapp.domain.model.TemplateCategory,
        val includePremium: Boolean = true
    )

    override fun invoke(params: Params): Result<List<com.resumeapp.domain.model.Template>> {
        return try {
            val templates = templateRepository.getByCategory(params.category)
            val filtered = if (params.includePremium) {
                templates
            } else {
                templates.filter { !it.isPremium }
            }
            Result.Success(filtered)
        } catch (e: Exception) {
            Result.failure("Failed to load templates: ${e.message}")
        }
    }
}