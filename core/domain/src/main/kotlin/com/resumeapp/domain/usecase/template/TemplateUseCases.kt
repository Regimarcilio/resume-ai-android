package com.resumeapp.domain.usecase.template

import com.resumeapp.domain.model.Result
import com.resumeapp.domain.model.Result.Success
import com.resumeapp.domain.model.Result.Failure
import com.resumeapp.domain.model.Template
import com.resumeapp.domain.model.TemplateCategory
import com.resumeapp.domain.repository.ResumeRepository
import com.resumeapp.domain.repository.TemplateRepository
import com.resumeapp.domain.usecase.AsyncUseCase
import com.resumeapp.domain.usecase.IdParams
import com.resumeapp.domain.usecase.SearchParams
import kotlinx.serialization.Serializable

/**
 * Get All Templates UseCase
 * Retrieves all active templates, optionally filtered by category
 */
class GetTemplatesUseCase constructor(
    private val templateRepository: TemplateRepository
) : AsyncUseCase<GetTemplatesUseCase.Params, List<Template>> {

    @Serializable
    data class Params(
        val category: TemplateCategory? = null,
        val premiumOnly: Boolean = false,
        val freeOnly: Boolean = false
    )

    override suspend fun invoke(params: Params): Result<List<Template>> {
        return try {
            val templates = when {
                params.category != null -> templateRepository.getByCategory(params.category!!)
                params.premiumOnly -> templateRepository.getPremiumTemplates()
                params.freeOnly -> templateRepository.getFreeTemplates()
                else -> templateRepository.getAllActive()
            }
            Success(templates)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }
}

/**
 * Get Template by ID UseCase
 * Retrieves a specific template by ID
 */
class GetTemplateByIdUseCase constructor(
    private val templateRepository: TemplateRepository
) : AsyncUseCase<IdParams, Template> {

    override suspend fun invoke(params: IdParams): Result<Template> {
        return try {
            templateRepository.getById(params.id)
                ?.let { Success(it) }
                ?: Failure("Template not found: ${params.id}")
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }
}

/**
 * Preview Template UseCase
 * Generates a preview of a template with sample data
 */
class PreviewTemplateUseCase constructor(
    private val templateRepository: TemplateRepository
) : AsyncUseCase<PreviewTemplateUseCase.Params, PreviewTemplateUseCase.PreviewResult> {

    @Serializable
    data class Params(
        val templateId: String,
        val sampleData: Boolean = true
    )

    @Serializable
    data class PreviewResult(
        val template: Template,
        val previewImageUrl: String,
        val samplePdfBytes: ByteArray? = null
    )

    override suspend fun invoke(params: Params): Result<PreviewResult> {
        return try {
            val template = templateRepository.getById(params.templateId)
                ?: return Failure("Template not found: ${params.templateId}")

            // In real implementation, would generate PDF preview
            val result = PreviewResult(
                template = template,
                previewImageUrl = template.previewImageUrl,
                samplePdfBytes = if (params.sampleData) generateSamplePdf(template) else null
            )

            Success(result)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun generateSamplePdf(template: Template): ByteArray {
        // Placeholder - would integrate with PDF generator
        return ByteArray(0)
    }
}

/**
 * Select Template UseCase
 * Applies a template to a resume
 */
class SelectTemplateUseCase constructor(
    private val templateRepository: TemplateRepository,
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<SelectTemplateUseCase.Params, com.resumeapp.domain.model.Resume> {

    @Serializable
    data class Params(
        val resumeId: String,
        val templateId: String
    )

    override suspend fun invoke(params: Params): Result<com.resumeapp.domain.model.Resume> {
        return try {
            val template = templateRepository.getById(params.templateId)
                ?: return Failure("Template not found: ${params.templateId}")

            val resume = resumeRepository.getById(params.resumeId)
                ?: return Failure("Resume not found: ${params.resumeId}")

            val updated = resume.copy(
                templateId = params.templateId,
                updatedAt = System.currentTimeMillis(),
                version = resume.version + 1
            )

            val saved = resumeRepository.update(updated)
            Success(saved)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }
}

/**
 * Get Templates by Category UseCase
 * Convenience method for getting templates by category
 */
class GetTemplatesByCategoryUseCase constructor(
    private val templateRepository: TemplateRepository
) : AsyncUseCase<GetTemplatesByCategoryUseCase.Params, List<Template>> {

    @Serializable
    data class Params(
        val category: TemplateCategory,
        val includePremium: Boolean = true
    )

    override suspend fun invoke(params: Params): Result<List<Template>> {
        return try {
            val templates = templateRepository.getByCategory(params.category)
            val filtered = if (params.includePremium) {
                templates
            } else {
                templates.filter { !it.isPremium }
            }
            Success(filtered)
        } catch (e: Exception) {
            Failure("Failed to load templates: ${e.message}")
        }
    }
}