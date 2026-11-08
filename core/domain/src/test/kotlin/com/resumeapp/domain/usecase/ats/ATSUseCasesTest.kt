package com.resumeapp.domain.usecase.ats

import com.resumeapp.domain.model.Result
import com.resumeapp.domain.model.ATSScore
import com.resumeapp.domain.model.ATSSuggestion
import com.resumeapp.domain.model.SuggestionType
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.model.SectionType
import com.resumeapp.domain.repository.ATSScoreRepository
import com.resumeapp.domain.repository.ResumeRepository
import com.resumeapp.ai.gateway.AIGatewayClient
import com.resumeapp.ai.cache.AICache
import com.resumeapp.domain.usecase.Result.Success
import com.resumeapp.domain.usecase.Result.Failure
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

class AnalyzeATSUseCaseTest {

    private lateinit var mockATSRepository: ATSScoreRepository
    private lateinit var mockResumeRepository: ResumeRepository
    private lateinit var mockAIGateway: AIGatewayClient
    private lateinit var mockAICache: AICache
    private lateinit var useCase: AnalyzeATSUseCase

    @Before
    fun setup() {
        mockATSRepository = mockk()
        mockResumeRepository = mockk()
        mockAIGateway = mockk()
        mockAICache = mockk()
        useCase = AnalyzeATSUseCase(mockATSRepository, mockResumeRepository, mockAIGateway, mockAICache)
    }

    @Test
    fun `returns cached score when available and not forcing refresh`() = runBlockingTest {
        // Given
        val cachedScore = com.resumeapp.domain.model.ATSScore(
            resumeId = "resume-1",
            overallScore = 85,
            keywordMatch = 80,
            formattingScore = 90,
            sectionCompleteness = 85,
            missingKeywords = listOf("Kubernetes"),
            suggestions = emptyList(),
            analyzedAt = System.currentTimeMillis()
        )
        coEvery { mockATSRepository.getByResumeId("resume-1") } returns Result.Success(cachedScore)
        coEvery { mockResumeRepository.getById("resume-1") } returns Result.Success(Resume(
            id = "resume-1",
            userId = "user-1",
            templateId = "template-1",
            personalInfo = com.resumeapp.domain.model.PersonalInfo(fullName = "John Doe"),
            sections = listOf(
                com.resumeapp.domain.model.ResumeSection(
                    type = SectionType.EXPERIENCE,
                    title = "Experience",
                    itemsJson = "[]",
                    order = 0
                )
            )
        ))

        // When
        val result = useCase(AnalyzeATSUseCase.Params(
            resumeId = "resume-1",
            forceRefresh = false
        ))

        // Then
        assertTrue(result is Result.Success)
        val score = (result as Result.Success<ATSScore>).value
        assertEquals(85, score.overallScore)
        assertEquals("Kubernetes", score.missingKeywords[0])
        
        // Verify cache was used (no AI gateway call)
        verify(exactly = 0) { mockAIGateway.complete(any()) }
    }

    @Test
    fun `analyzes with AI when no cache or force refresh`() = runBlockingTest {
        // Given
        val resume = Resume(
            id = "resume-1",
            userId = "user-1",
            templateId = "template-1",
            personalInfo = com.resumeapp.domain.model.PersonalInfo(
                fullName = "John Doe",
                email = "john@example.com",
                summary = "Senior Kotlin Developer"
            ),
            sections = listOf(
                com.resumeapp.domain.model.ResumeSection(
                    type = SectionType.EXPERIENCE,
                    title = "Experience",
                    itemsJson = """[{"title":"Senior Developer","subtitle":"Company","startDate":"2020-01","endDate":"Present","highlights":["Led team of 5","Kotlin/Spring Boot"]}]""",
                    order = 0,
                    type = SectionType.EXPERIENCE
                ),
                com.resumeapp.domain.model.ResumeSection(
                    type = SectionType.SKILLS,
                    title = "Skills",
                    itemsJson = """[{"title":"Kotlin","highlights":[]},{"title":"Spring Boot","highlights":[]}]""",
                    order = 1,
                    type = SectionType.SKILLS
                )
            )
        )

        coEvery { mockATSRepository.getByResumeId("resume-1") } returns Result.Success(null)
        coEvery { mockResumeRepository.getById("resume-1") } returns Result.Success(resume)
        
        // Mock AI response
        val aiResponse = com.resumeapp.ai.gateway.AIResponse(
            id = "ai-1",
            model = "gpt-4o-mini",
            choices = listOf(
                com.resumeapp.ai.gateway.Choice(
                    message = com.resumeapp.ai.gateway.Message(content = """{"keywordMatch":75,"missingKeywords":["Kubernetes","Docker"]}""")
                )
            ),
            usage = com.resumeapp.ai.gateway.Usage(promptTokens = 100, completionTokens = 50, totalTokens = 150, costUsd = 0.001),
            latencyMs = 500
        )
        coEvery { mockAIGateway.complete(any()) } returns Result.Success(aiResponse)

        // When
        val result = useCase(AnalyzeATSUseCase.Params(
            resumeId = "resume-1",
            jobDescription = "Senior Kotlin Developer with Kubernetes experience"
        ))

        // Then
        assertTrue(result is Result.Success)
        val score = (result as Result.Success<ATSScore>).value
        assertNotNull(score)
        assertEquals("resume-1", score.resumeId)
        assertTrue(score.overallScore > 0)
        assertTrue(score.overallScore <= 100)
        assertTrue(score.missingKeywords.contains("Kubernetes"))
        
        // Verify AI was called
        verify { mockAIGateway.complete(any()) }
    }

    @Test
    fun `returns failure when resume not found`() = runBlockingTest {
        // Given
        coEvery { mockATSRepository.getByResumeId("missing") } returns Result.Success(null)
        coEvery { mockResumeRepository.getById("missing") } returns Result.Failure(Exception("Not found"))

        // When
        val result = useCase(AnalyzeATSUseCase.Params(resumeId = "missing"))

        // Then
        assertTrue(result is Result.Failure)
    }
}

class GetATSScoreUseCaseTest {

    private lateinit var mockRepository: ATSScoreRepository
    private lateinit var useCase: GetATSScoreUseCase

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = GetATSScoreUseCase(mockRepository)
    }

    @Test
    fun `returns score when found`() = runBlockingTest {
        // Given
        val score = ATSScore(
            resumeId = "resume-1",
            overallScore = 90,
            keywordMatch = 85,
            formattingScore = 95,
            sectionCompleteness = 90,
            missingKeywords = listOf(),
            suggestions = emptyList(),
            analyzedAt = System.currentTimeMillis()
        )
        coEvery { mockRepository.getByResumeId("resume-1") } returns Result.Success(score)

        // When
        val result = useCase(com.resumeapp.domain.usecase.IdParams("resume-1"))

        // Then
        assertTrue(result is Result.Success)
        assertEquals(90, (result as Result.Success<ATSScore>).value.overallScore)
    }
}

class ApplyATSSuggestionUseCaseTest {

    private lateinit var mockRepository: ResumeRepository
    private lateinit var useCase: ApplyATSSuggestionUseCase

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = ApplyATSSuggestionUseCase(mockRepository)
    }

    @Test
    fun `applies add keyword suggestion to skills section`() = runBlockingTest {
        // Given
        val skillsSection = com.resumeapp.domain.model.ResumeSection(
            id = "skills-section",
            type = SectionType.SKILLS,
            title = "Skills",
            itemsJson = """[{"title":"Kotlin","keywords":["Kotlin"]},{"title":"Spring Boot","keywords":["Spring Boot"]}]""",
            order = 2,
            type = SectionType.SKILLS
        )

        val resume = Resume(
            id = "resume-1",
            userId = "user-1",
            templateId = "template-1",
            personalInfo = com.resumeapp.domain.model.PersonalInfo(),
            sections = listOf(skillsSection)
        )

        val suggestion = ATSSuggestion(
            sectionId = "skills-section",
            type = SuggestionType.ADD_KEYWORD,
            priority = 1,
            message = "Add Kubernetes keyword",
            example = "Kubernetes"
        )

        val updatedResume = resume.copy(
            sections = listOf(skillsSection.copy(
                itemsJson = """[{"title":"Kotlin","keywords":["Kotlin"]},{"title":"Spring Boot","keywords":["Spring Boot"]},{"title":"Kubernetes","keywords":["Kubernetes"]}]"""
            )),
            version = 2
        )

        coEvery { mockRepository.getById("resume-1") } returns Result.Success(
            Resume(
                id = "resume-1",
                userId = "user-1",
                templateId = "template-1",
                personalInfo = com.resumeapp.domain.model.PersonalInfo(),
                sections = listOf(skillsSection)
            )
        )
        coEvery { mockRepository.update(any()) } returns Result.Success(resume)

        // When
        val result = useCase(ApplyATSSuggestionUseCase.Params(
            resumeId = "resume-1",
            suggestion = ATSSuggestion(
                sectionId = "skills-section",
                type = SuggestionType.ADD_KEYWORD,
                priority = 1,
                message = "Add Kubernetes",
                example = "Kubernetes"
            )
        ))

        // Then
        assertTrue(result is Result.Success)
    }
}