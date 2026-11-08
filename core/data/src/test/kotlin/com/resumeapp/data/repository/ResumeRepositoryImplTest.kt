package com.resumeapp.data.repository

import com.resumeapp.data.db.ResumeDao
import com.resumeapp.data.mapper.ResumeMapper
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.model.Result
import com.resumeapp.domain.model.SectionType
import com.resumeapp.domain.repository.ResumeRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ResumeRepositoryImplTest {

    private lateinit var mockDatabase: AppDatabase
    private lateinit var mockResumeDao: ResumeDao
    private lateinit var mockSectionDao: ResumeSectionDao
    private lateinit var mockMapper: ResumeMapper
    private lateinit var repository: ResumeRepositoryImpl

    @Before
    fun setup() {
        mockDatabase = mockk()
        mockResumeDao = mockk()
        mockSectionDao = mockk()
        mockMapper = mockk()
        repository = ResumeRepositoryImpl(mockDatabase, mockResumeDao, mockSectionDao, mockMapper)
    }

    @Test
    fun `saves resume with sections`() = runBlockingTest {
        // Given
        val resume = Resume(
            id = "resume-1",
            userId = "user-1",
            templateId = "template-1",
            personalInfo = com.resumeapp.domain.model.PersonalInfo(fullName = "John Doe"),
            sections = listOf(
                com.resumeapp.domain.model.ResumeSection(
                    id = "section-1",
                    type = SectionType.EXPERIENCE,
                    title = "Experience",
                    itemsJson = """[{"title":"Senior Dev"}]""",
                    order = 0,
                    isVisible = true
                )
            )
        )

        val entity = com.resumeapp.data.db.ResumeEntity(
            id = "resume-1",
            userId = "user-1",
            templateId = "template-1",
            personalInfoJson = "{}",
            sectionsJson = "[]",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            version = 1
        )

        val savedEntity = entity.copy(id = "resume-1")

        coEvery { mockMapper.toEntity(any()) } returns entity
        coEvery { mockDatabase.runInTransaction(any()) } returns Result.Success(Unit)
        coEvery { mockResumeDao.insert(any()) } returns 1
        coEvery { mockResumeDao.getById("resume-1") } returns com.resumeapp.data.db.ResumeEntity(
            id = "resume-1",
            userId = "user-1",
            templateId = "template-1",
            personalInfoJson = "{}",
            sectionsJson = "[]",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            version = 1
        )
        coEvery { mockMapper.toDomain(any()) } returns Resume(
            id = "resume-1",
            userId = "user-1",
            templateId = "template-1",
            personalInfo = com.resumeapp.domain.model.PersonalInfo(),
            sections = emptyList()
        )

        // When
        val result = repository.save(Resume(
            id = "resume-1",
            userId = "user-1",
            templateId = "template-1",
            personalInfo = com.resumeapp.domain.model.PersonalInfo(),
            sections = listOf()
        ))

        // Then
        assertTrue(result is Result.Success)
        verify { mockResumeDao.insert(any()) }
        verify { mockSectionDao.insert(any()) }
    }
}