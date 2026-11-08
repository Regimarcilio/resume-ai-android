package com.resumeapp.domain.usecase.resume

import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.model.Result
import com.resumeapp.domain.model.SectionType
import com.resumeapp.domain.repository.ResumeRepository
import com.resumeapp.domain.usecase.Result.Success
import com.resumeapp.domain.usecase.Result.Failure
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateResumeUseCaseTest {

    private val testDispatcher = TestDispatcher()
    private lateinit var mockRepository: ResumeRepository
    private lateinit var useCase: CreateResumeUseCase

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = CreateResumeUseCase(mockRepository, kotlinx.coroutines.test.runBlockingTest { Dispatchers.Main })
    }

    @Test
    fun `creates resume with default sections`() = runBlockingTest {
        // Given
        val expectedResume = Resume(
            id = "test-id",
            userId = "user-1",
            templateId = "template-1",
            personalInfo = com.resumeapp.domain.model.PersonalInfo(),
            sections = listOf(
                com.resumeapp.domain.model.ResumeSection(
                    type = SectionType.EXPERIENCE,
                    title = "Experiência",
                    itemsJson = "[]",
                    order = 0
                ),
                com.resumeapp.domain.model.ResumeSection(
                    type = SectionType.EDUCATION,
                    title = "Formação",
                    itemsJson = "[]",
                    order = 1
                ),
                com.resumeapp.domain.model.ResumeSection(
                    type = SectionType.SKILLS,
                    title = "Habilidades",
                    itemsJson = "[]",
                    order = 2
                ),
                com.resumeapp.domain.model.ResumeSection(
                    type = SectionType.PROJECTS,
                    title = "Projetos",
                    itemsJson = "[]",
                    order = 3
                )
            )
        )

        coEvery { mockRepository.save(any()) } returns Result.Success(expectedResume)

        // When
        val result = useCase(CreateResumeUseCase.Params(
            templateId = "template-1",
            userId = "user-1"
        ))

        // Then
        assertTrue(result is Success)
        val resume = (result as Success<Resume>).value
        assertEquals("user-1", resume.userId)
        assertEquals("template-1", resume.templateId)
        assertEquals(4, resume.sections.size)
        assertEquals(SectionType.EXPERIENCE, resume.sections[0].type)
        assertEquals(SectionType.EDUCATION, resume.sections[1].type)
        assertEquals(SectionType.SKILLS, resume.sections[2].type)
        assertEquals(SectionType.PROJECTS, resume.sections[3].type)

        verify { mockRepository.save(any()) }
    }

    @Test
    fun `returns failure when repository fails`() = runBlockingTest {
        // Given
        val error = Exception("Database error")
        coEvery { mockRepository.save(any()) } returns Result.Failure(error)

        // When
        val result = useCase(CreateResumeUseCase.Params(
            templateId = "template-1",
            userId = "user-1"
        ))

        // Then
        assertTrue(result is Failure)
        assertEquals("Database error", (result as Failure).error.message)
    }
}

class GetResumeUseCaseTest {

    private lateinit var mockRepository: ResumeRepository
    private lateinit var useCase: GetResumeUseCase

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = GetResumeUseCase(mockRepository)
    }

    @Test
    fun `returns resume when found`() = runBlockingTest {
        // Given
        val expectedResume = Resume(
            id = "resume-1",
            userId = "user-1",
            templateId = "template-1",
            personalInfo = com.resumeapp.domain.model.PersonalInfo(fullName = "John Doe"),
            sections = emptyList()
        )
        coEvery { mockRepository.getById("resume-1") } returns Result.Success(expectedResume)

        // When
        val result = useCase(com.resumeapp.domain.usecase.IdParams("resume-1"))

        // Then
        assertTrue(result is Success)
        assertEquals("John Doe", (result as Success<Resume>).value.personalInfo.fullName)
    }

    @Test
    fun `returns failure when not found`() = runBlockingTest {
        // Given
        coEvery { mockRepository.getById("missing") } returns Result.Failure(Exception("Not found"))

        // When
        val result = useCase(com.resumeapp.domain.usecase.IdParams("missing"))

        // Then
        assertTrue(result is Result.Failure)
    }
}

class UpdateResumeUseCaseTest {

    private lateinit var mockRepository: ResumeRepository
    private lateinit var useCase: UpdateResumeUseCase

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = UpdateResumeUseCase(mockRepository)
    }

    @Test
    fun `updates template and personal info`() = runBlockingTest {
        // Given
        val original = Resume(
            id = "resume-1",
            userId = "user-1",
            templateId = "old-template",
            personalInfo = com.resumeapp.domain.model.PersonalInfo(fullName = "Old Name"),
            sections = emptyList()
        )
        val updated = original.copy(
            templateId = "new-template",
            personalInfo = com.resumeapp.domain.model.PersonalInfo(fullName = "New Name"),
            version = 2
        )

        coEvery { mockRepository.getById("resume-1") } returns Result.Success(original)
        coEvery { mockRepository.update(any()) } returns Result.Success(updated)

        // When
        val result = useCase(UpdateResumeUseCase.Params(
            resumeId = "resume-1",
            templateId = "new-template",
            personalInfo = com.resumeapp.domain.model.PersonalInfo(fullName = "New Name")
        ))

        // Then
        assertTrue(result is Success)
        val updatedResume = (result as Success<Resume>).value
        assertEquals("new-template", updatedResume.templateId)
        assertEquals("New Name", updatedResume.personalInfo.fullName)
        assertEquals(2, updatedResume.version)
    }

    @Test
    fun `returns failure when resume not found`() = runBlockingTest {
        coEvery { mockRepository.getById("missing") } returns Result.Failure(Exception("Not found"))

        val result = useCase(UpdateResumeUseCase.Params(resumeId = "missing"))

        assertTrue(result is Result.Failure)
    }
}

class ReorderSectionsUseCaseTest {

    private lateinit var mockRepository: ResumeRepository
    private lateinit var useCase: ReorderSectionsUseCase

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = ReorderSectionsUseCase(mockRepository)
    }

    @Test
    fun `reorders sections correctly`() = runBlockingTest {
        // Given
        val section1 = com.resumeapp.domain.model.ResumeSection(
            id = "section-1",
            type = SectionType.EXPERIENCE,
            title = "Experiência",
            itemsJson = "[]",
            order = 0
        )
        val section2 = com.resumeapp.domain.model.ResumeSection(
            id = "section-2",
            type = SectionType.EDUCATION,
            title = "Formação",
            itemsJson = "[]",
            order = 1
        )
        val section3 = com.resumeapp.domain.model.ResumeSection(
            id = "section-3",
            type = SectionType.SKILLS,
            title = "Habilidades",
            itemsJson = "[]",
            order = 2
        )

        val original = Resume(
            id = "resume-1",
            userId = "user-1",
            templateId = "template-1",
            personalInfo = com.resumeapp.domain.model.PersonalInfo(),
            sections = listOf(section1, section2, section3)
        )

        val reordered = original.copy(
            sections = listOf(section3, section1, section2),
            version = 2
        )

        coEvery { mockRepository.getById("resume-1") } returns Result.Success(original)
        coEvery { mockRepository.update(any()) } returns Result.Success(reordered)

        // When
        val result = useCase(ReorderSectionsUseCase.Params(
            resumeId = "resume-1",
            sectionOrders = mapOf(
                "section-3" to 0,
                "section-1" to 1,
                "section-2" to 2
            )
        ))

        // Then
        assertTrue(result is Result.Success)
        val reorderedResume = (result as Result.Success<Resume>).value
        assertEquals(SectionType.SKILLS, reorderedResume.sections[0].type)
        assertEquals(SectionType.EXPERIENCE, reorderedResume.sections[1].type)
        assertEquals(SectionType.EDUCATION, reorderedResume.sections[2].type)
    }
}