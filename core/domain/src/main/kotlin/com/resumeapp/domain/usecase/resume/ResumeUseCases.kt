package com.resumeapp.domain.usecase.resume

import com.resumeapp.domain.model.Result
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.model.ResumeSection
import com.resumeapp.domain.repository.ResumeRepository
import com.resumeapp.domain.usecase.AsyncUseCase
import com.resumeapp.domain.usecase.IdParams
import com.resumeapp.domain.usecase.NoParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Create Resume UseCase
 * Creates a new resume from a template
 */
class CreateResumeUseCase @Inject constructor(
    private val resumeRepository: ResumeRepository,
    private val coroutineScope: CoroutineScope
) : AsyncUseCase<CreateResumeUseCase.Params, Resume> {

    @Serializable
    data class Params(
        val templateId: String,
        val userId: String
    )

    override fun invoke(params: Params): Result<Resume> = coroutineScope.runBlocking {
        try {
            val resume = Resume(
                userId = params.userId,
                templateId = params.templateId,
                personalInfo = com.resumeapp.domain.model.PersonalInfo(),
                sections = defaultSectionsForTemplate(params.templateId)
            )
            
            val saved = resumeRepository.save(resume)
            Result.Success(saved)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    private fun defaultSectionsForTemplate(templateId: String): List<com.resumeapp.domain.model.ResumeSection> {
        val sectionTypes = listOf(
            com.resumeapp.domain.model.SectionType.EXPERIENCE,
            com.resumeapp.domain.model.SectionType.EDUCATION,
            com.resumeapp.domain.model.SectionType.SKILLS,
            com.resumeapp.domain.model.SectionType.PROJECTS
        )
        
        return sectionTypes.mapIndexed { index, type ->
            com.resumeapp.domain.model.ResumeSection(
                resumeId = "", // Will be set after resume creation
                type = type,
                title = type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                itemsJson = "[]",
                order = index,
                isVisible = true
            )
        }
    }
}

/**
 * Get Resume UseCase
 * Retrieves a resume by ID with all sections
 */
class GetResumeUseCase @Inject constructor(
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<IdParams, Resume> {

    override fun invoke(params: IdParams): Result<Resume> {
        return try {
            resumeRepository.getById(params.id)
                ?.let { Result.Success(it) }
                ?: Result.Failure(Exception("Resume not found: ${params.id}"))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}

/**
 * Get All Resumes UseCase
 * Lists all resumes for a user
 */
class GetAllResumesUseCase @Inject constructor(
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<com.resumeapp.domain.usecase.SearchParams, List<com.resumeapp.domain.model.Resume>> {

    override fun invoke(params: com.resumeapp.domain.usecase.SearchParams): Result<List<com.resumeapp.domain.model.Resume>> {
        return try {
            val userId = params.filters["userId"] ?: return Result.Failure(Exception("userId required"))
            val resumes = resumeRepository.getAllByUserId(userId)
            Result.Success(resumes)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}

/**
 * Update Resume UseCase
 * Updates resume metadata (template, personal info)
 */
class UpdateResumeUseCase @Inject constructor(
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<UpdateResumeUseCase.Params, com.resumeapp.domain.model.Resume> {

    @Serializable
    data class Params(
        val resumeId: String,
        val templateId: String? = null,
        val personalInfo: com.resumeapp.domain.model.PersonalInfo? = null
    )

    override fun invoke(params: Params): Result<com.resumeapp.domain.model.Resume> {
        return try {
            val existing = resumeRepository.getById(params.resumeId)
                ?: return Result.Failure(Exception("Resume not found: ${params.resumeId}"))

            val updated = existing.copy(
                templateId = params.templateId ?: existing.templateId,
                personalInfo = params.personalInfo ?: existing.personalInfo,
                updatedAt = System.currentTimeMillis(),
                version = existing.version + 1
            )

            resumeRepository.update(updated)
            Result.Success(updated)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}

/**
 * Delete Resume UseCase
 * Deletes a resume and all its sections
 */
class DeleteResumeUseCase @Inject constructor(
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<IdParams, Unit> {

    override fun invoke(params: IdParams): Result<Unit> {
        return try {
            resumeRepository.deleteById(params.id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}

/**
 * Duplicate Resume UseCase
 * Creates a copy of an existing resume
 */
class DuplicateResumeUseCase @Inject constructor(
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<DuplicateResumeUseCase.Params, com.resumeapp.domain.model.Resume> {

    @Serializable
    data class Params(
        val resumeId: String,
        val newTitle: String? = null
    )

    override fun invoke(params: Params): Result<com.resumeapp.domain.model.Resume> {
        return try {
            val original = resumeRepository.getById(params.resumeId)
                ?: return Result.Failure(Exception("Resume not found: ${params.resumeId}"))

            val duplicate = original.copy(
                id = java.util.UUID.randomUUID().toString(),
                personalInfo = original.personalInfo.copy(
                    fullName = params.newTitle ?: "${original.personalInfo.fullName} (Cópia)"
                ),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                version = 1
            )

            val saved = resumeRepository.save(duplicate)
            Result.Success(saved)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}

/**
 * Reorder Sections UseCase
 * Changes the order of sections in a resume
 */
class ReorderSectionsUseCase @Inject constructor(
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<ReorderSectionsUseCase.Params, com.resumeapp.domain.model.Resume> {

    @Serializable
    data class Params(
        val resumeId: String,
        val sectionOrders: Map<String, Int> // sectionId -> newOrder
    )

    override fun invoke(params: Params): Result<com.resumeapp.domain.model.Resume> {
        return try {
            val resume = resumeRepository.getById(params.resumeId)
                ?: return Result.Failure(Exception("Resume not found: ${params.resumeId}"))

            val updatedSections = resume.sections.map { section ->
                params.sectionOrders[section.id]?.let { newOrder ->
                    section.copy(order = newOrder)
                } ?: section
            }.sortedBy { it.order }

            val updated = resume.copy(
                sections = updatedSections,
                updatedAt = System.currentTimeMillis(),
                version = resume.version + 1
            )

            resumeRepository.update(updated)
            Result.Success(updated)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}

/**
 * Update Section UseCase
 * Updates a specific section's items
 */
class UpdateSectionUseCase @Inject constructor(
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<UpdateSectionUseCase.Params, com.resumeapp.domain.model.Resume> {

    @Serializable
    data class Params(
        val resumeId: String,
        val sectionId: String,
        val itemsJson: String,
        val isVisible: Boolean? = null
    )

    override fun invoke(params: Params): Result<com.resumeapp.domain.model.Resume> {
        return try {
            val resume = resumeRepository.getById(params.resumeId)
                ?: return Result.Failure(Exception("Resume not found: ${params.resumeId}"))

            val updatedSections = resume.sections.map { section ->
                if (section.id == params.sectionId) {
                    section.copy(
                        itemsJson = params.itemsJson,
                        isVisible = params.isVisible ?: section.isVisible
                    )
                } else section
            }

            val updated = resume.copy(
                sections = updatedSections,
                updatedAt = System.currentTimeMillis(),
                version = resume.version + 1
            )

            resumeRepository.update(updated)
            Result.Success(updated)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}