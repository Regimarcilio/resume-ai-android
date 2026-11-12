package com.resumeapp.domain.usecase.resume

import com.resumeapp.domain.model.PersonalInfo
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.model.ResumeSection
import com.resumeapp.domain.model.Result
import com.resumeapp.domain.model.Result.Success
import com.resumeapp.domain.model.Result.Failure
import com.resumeapp.domain.model.SectionType
import com.resumeapp.domain.model.displayTitle
import com.resumeapp.domain.repository.ResumeRepository
import com.resumeapp.domain.usecase.AsyncUseCase
import com.resumeapp.domain.usecase.IdParams
import com.resumeapp.domain.usecase.SearchParams
import kotlinx.serialization.Serializable

/**
 * Create Resume UseCase
 * Creates a new resume from a template
 */
class CreateResumeUseCase constructor(
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<CreateResumeUseCase.Params, Resume> {

    @Serializable
    data class Params(
        val templateId: String,
        val userId: String
    )

    override suspend operator fun invoke(params: Params): Result<Resume> {
        return try {
            val resume = Resume(
                userId = params.userId,
                templateId = params.templateId,
                personalInfo = PersonalInfo(),
                sections = defaultSectionsForTemplate(params.templateId)
            )

            val saved = resumeRepository.save(resume)
            Success(saved)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun defaultSectionsForTemplate(templateId: String): List<ResumeSection> {
        val sectionTypes = listOf(
            SectionType.EXPERIENCE,
            SectionType.EDUCATION,
            SectionType.SKILLS,
            SectionType.PROJECTS
        )

        return sectionTypes.mapIndexed { index, type ->
            ResumeSection(
                resumeId = "", // Will be set after resume creation
                type = type,
                title = type.displayTitle,
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
class GetResumeUseCase constructor(
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<IdParams, Resume> {

    override suspend operator fun invoke(params: IdParams): Result<Resume> {
        return try {
            resumeRepository.getById(params.id)
                ?.let { Success(it) }
                ?: Failure("Resume not found: ${params.id}")
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }
}

/**
 * Get All Resumes UseCase
 * Lists all resumes for a user
 */
class GetAllResumesUseCase constructor(
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<SearchParams, List<Resume>> {

    override suspend operator fun invoke(params: SearchParams): Result<List<Resume>> {
        return try {
            val userId = params.filters["userId"] ?: return Failure("userId required")
            val resumes = resumeRepository.getAllByUserId(userId)
            Success(resumes)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }
}

/**
 * Update Resume UseCase
 * Updates resume metadata (template, personal info)
 */
class UpdateResumeUseCase constructor(
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<UpdateResumeUseCase.Params, Resume> {

    @Serializable
    data class Params(
        val resumeId: String,
        val templateId: String? = null,
        val personalInfo: PersonalInfo? = null
    )

    override suspend operator fun invoke(params: Params): Result<Resume> {
        return try {
            val existing = resumeRepository.getById(params.resumeId)
                ?: return Failure("Resume not found: ${params.resumeId}")

            val updated = existing.copy(
                templateId = params.templateId ?: existing.templateId,
                personalInfo = params.personalInfo ?: existing.personalInfo,
                updatedAt = System.currentTimeMillis(),
                version = existing.version + 1
            )

            resumeRepository.update(updated)
            Success(updated)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }
}

/**
 * Delete Resume UseCase
 * Deletes a resume and all its sections
 */
class DeleteResumeUseCase constructor(
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<IdParams, Unit> {

    override suspend operator fun invoke(params: IdParams): Result<Unit> {
        return try {
            resumeRepository.deleteById(params.id)
            Success(Unit)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }
}

/**
 * Duplicate Resume UseCase
 * Creates a copy of an existing resume
 */
class DuplicateResumeUseCase constructor(
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<DuplicateResumeUseCase.Params, Resume> {

    @Serializable
    data class Params(
        val resumeId: String,
        val newTitle: String? = null
    )

    override suspend operator fun invoke(params: Params): Result<Resume> {
        return try {
            val original = resumeRepository.getById(params.resumeId)
                ?: return Failure("Resume not found: ${params.resumeId}")

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
            Success(saved)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }
}

/**
 * Reorder Sections UseCase
 * Changes the order of sections in a resume
 */
class ReorderSectionsUseCase constructor(
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<ReorderSectionsUseCase.Params, Resume> {

    @Serializable
    data class Params(
        val resumeId: String,
        val sectionOrders: Map<String, Int> // sectionId -> newOrder
    )

    override suspend operator fun invoke(params: Params): Result<Resume> {
        return try {
            val resume = resumeRepository.getById(params.resumeId)
                ?: return Failure("Resume not found: ${params.resumeId}")

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
            Success(updated)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }
}

/**
 * Update Section UseCase
 * Updates a specific section's items
 */
class UpdateSectionUseCase constructor(
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<UpdateSectionUseCase.Params, Resume> {

    @Serializable
    data class Params(
        val resumeId: String,
        val sectionId: String,
        val itemsJson: String,
        val isVisible: Boolean? = null
    )

    override suspend operator fun invoke(params: Params): Result<Resume> {
        return try {
            val resume = resumeRepository.getById(params.resumeId)
                ?: return Failure("Resume not found: ${params.resumeId}")

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
            Success(updated)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }
}

/**
 * Save Resume UseCase
 * Saves/updates a resume
 */
class SaveResumeUseCase constructor(
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<SaveResumeUseCase.Params, Resume> {

    @Serializable
    data class Params(
        val resume: Resume
    )

    override suspend operator fun invoke(params: Params): Result<Resume> {
        return try {
            val saved = resumeRepository.save(params.resume)
            Success(saved)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }
}

/**
 * Add Section UseCase
 * Adds a new section to a resume
 */
class AddSectionUseCase constructor(
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<AddSectionUseCase.Params, Resume> {

    @Serializable
    data class Params(
        val resumeId: String,
        val section: com.resumeapp.domain.model.ResumeSection
    )

    override suspend operator fun invoke(params: Params): Result<Resume> {
        return try {
            val resume = resumeRepository.getById(params.resumeId)
                ?: return Failure("Resume not found: ${params.resumeId}")

            val newOrder = resume.sections.maxByOrNull { it.order }?.order ?: -1
            val newSection = params.section.copy(
                resumeId = params.resumeId,
                order = newOrder + 1
            )

            val updatedSections = resume.sections + newSection
            val updated = resume.copy(
                sections = updatedSections,
                updatedAt = System.currentTimeMillis(),
                version = resume.version + 1
            )

            resumeRepository.update(updated)
            Success(updated)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }
}

/**
 * Delete Section UseCase
 * Removes a section from a resume
 */
class DeleteSectionUseCase constructor(
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<DeleteSectionUseCase.Params, Resume> {

    @Serializable
    data class Params(
        val resumeId: String,
        val sectionId: String
    )

    override suspend operator fun invoke(params: Params): Result<Resume> {
        return try {
            val resume = resumeRepository.getById(params.resumeId)
                ?: return Failure("Resume not found: ${params.resumeId}")

            val updatedSections = resume.sections.filter { it.id != params.sectionId }
                .mapIndexed { index, section ->
                    section.copy(order = index)
                }

            val updated = resume.copy(
                sections = updatedSections,
                updatedAt = System.currentTimeMillis(),
                version = resume.version + 1
            )

            resumeRepository.update(updated)
            Success(updated)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }
}