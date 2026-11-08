package com.resumeapp.domain.usecase.aiassist

import com.resumeapp.domain.model.Result
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.model.SectionItem
import com.resumeapp.domain.model.SectionType
import com.resumeapp.ai.gateway.AIGatewayClient
import com.resumeapp.ai.cache.AICache
import com.resumeapp.ai.guardrail.GuardrailPipeline
import com.resumeapp.ai.trace.TraceLogger
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

class ImproveBulletsUseCaseTest {

    private lateinit var mockAIGateway: AIGatewayClient
    private lateinit var mockAICache: AICache
    private lateinit var mockGuardrail: GuardrailPipeline
    private lateinit var mockTraceLogger: TraceLogger
    private lateinit var useCase: ImproveBulletsUseCase

    @Before
    fun setup() {
        mockAIGateway = mockk()
        mockAICache = mockk()
        mockGuardrail = mockk()
        mockTraceLogger = mockk()
        useCase = ImproveBulletsUseCase(mockAIGateway, mockAICache, mockGuardrail, mockTraceLogger)
    }

    @Test
    fun `improves bullets successfully`() = runBlockingTest {
        // Given
        val currentItems = listOf(
            SectionItem(
                title = "Senior Developer",
                subtitle = "Tech Company",
                startDate = "2020-01",
                endDate = "Present",
                highlights = listOf("Led team", "Kotlin development")
            )
        )

        val aiResponse = com.resumeapp.ai.gateway.AIResponse(
            id = "ai-1",
            model = "gpt-4o-mini",
            choices = listOf(
                com.resumeapp.ai.gateway.Choice(
                    message = com.resumeapp.ai.gateway.Message(content = "\u2022 Liderou migração de monolito para microsserviços (Kotlin/Spring), reduzindo deploy de 45min para 12min (73% mais rápido)\n\u2022 Arquitetou observabilidade com Grafana/Loki/Tempo, diminuindo MTTR de 2h para 15min")
                )
            ),
            usage = com.resumeapp.ai.gateway.Usage(promptTokens = 200, completionTokens = 100, totalTokens = 300, costUsd = 0.001),
            latencyMs = 500
        )

        coEvery { mockAICache.get(any()) } returns null
        coEvery { mockAIGateway.complete(any()) } returns Result.Success(aiResponse)
        coEvery { mockGuardrail.sanitizeInput(any()) } returns "sanitized prompt"
        coEvery { mockGuardrail.validateOutput(any()) } returns aiResponse

        // When
        val result = useCase(ImproveBulletsUseCase.Params(
            sectionType = SectionType.EXPERIENCE,
            currentItems = listOf(
                SectionItem(
                    title = "Senior Developer",
                    subtitle = "Tech Company",
                    startDate = "2020-01",
                    endDate = "Present",
                    highlights = listOf("Led team", "Kotlin development")
                )
            ),
            context = ImproveBulletsUseCase.ResumeContext(
                targetRole = "Tech Lead",
                industry = "Tech",
                experienceLevel = "Senior"
            ),
            maxBullets = 3
        ))

        // Then
        assertTrue(result is Result.Success)
        val bullets = (result as Result.Success<List<String>>).value
        assertTrue(bullets.size > 0)
        assertTrue(bullets[0].contains("microsserviços"))
        
        verify { mockAIGateway.complete(any()) }
        verify { mockGuardrail.sanitizeInput(any()) }
        verify { mockGuardrail.validateOutput(any()) }
    }

    @Test
    fun `returns cached bullets when available`() = runBlockingTest {
        // Given
        val cachedResponse = com.resumeapp.ai.gateway.AIResponse(
            id = "cached-1",
            model = "gpt-4o-mini",
            choices = listOf(
                com.resumeapp.ai.gateway.Choice(
                    message = com.resumeapp.ai.gateway.Message(content = "Cached bullet 1\nCached bullet 2")
                )
            ),
            usage = com.resumeapp.ai.gateway.Usage(promptTokens = 0, completionTokens = 0, totalTokens = 0, costUsd = 0.0),
            latencyMs = 10,
            cacheHit = true
        )

        coEvery { mockAICache.get(any()) } returns cachedResponse

        // When
        val result = useCase(ImproveBulletsUseCase.Params(
            sectionType = SectionType.EXPERIENCE,
            currentItems = listOf(),
            context = ImproveBulletsUseCase.ResumeContext()
        ))

        // Then
        assertTrue(result is Result.Success)
        val bullets = (result as Result.Success<List<String>>).value
        assertEquals(2, bullets.size)
        
        // AI Gateway should not be called
        verify(exactly = 0) { mockAIGateway.complete(any()) }
    }

    @Test
    fun `returns failure when AI gateway fails`() = runBlockingTest {
        // Given
        coEvery { mockAICache.get(any()) } returns null
        coEvery { mockAIGateway.complete(any()) } returns Result.Failure(Exception("Gateway timeout"))

        // When
        val result = useCase(ImproveBulletsUseCase.Params(
            sectionType = SectionType.EXPERIENCE,
            currentItems = listOf(),
            context = ImproveBulletsUseCase.ResumeContext()
        ))

        // Then
        assertTrue(result is Result.Failure)
    }
}

class ExtractKeywordsUseCaseTest {

    private lateinit var mockAIGateway: AIGatewayClient
    private lateinit var mockAICache: AICache
    private lateinit var mockTraceLogger: TraceLogger
    private lateinit var useCase: ExtractKeywordsUseCase

    @Before
    fun setup() {
        mockAIGateway = mockk()
        mockAICache = mockk()
        mockTraceLogger = mockk()
        useCase = ExtractKeywordsUseCase(mockAIGateway, mockAICache, mockTraceLogger)
    }

    @Test
    fun `extracts keywords from job description`() = runBlockingTest {
        // Given
        val jobDescription = """
            Vaga: Senior Kotlin Developer
            Requisitos: 5+ anos Kotlin, Spring Boot, Kubernetes, Docker, CI/CD, AWS
            Diferencial: Microservices, GraphQL, Terraform
        """

        val aiResponse = com.resumeapp.ai.gateway.AIResponse(
            id = "ai-1",
            model = "gpt-4o-mini",
            choices = listOf(
                com.resumeapp.ai.gateway.Choice(
                    message = com.resumeapp.ai.gateway.Message(content = """["Kotlin", "Spring Boot", "Kubernetes", "Docker", "CI/CD", "AWS", "Microservices", "GraphQL", "Terraform"]""")
                )
            ),
            usage = com.resumeapp.ai.gateway.Usage(promptTokens = 100, completionTokens = 50, totalTokens = 150, costUsd = 0.0005),
            latencyMs = 300
        )

        coEvery { mockAICache.get(any()) } returns null
        coEvery { mockAIGateway.complete(any()) } returns Result.Success(aiResponse)

        // When
        val result = useCase(ExtractKeywordsUseCase.Params(
            jobDescription = jobDescription.trimIndent(),
            maxKeywords = 15
        ))

        // Then
        assertTrue(result is Result.Success)
        val keywords = (result as Result.Success<List<String>>).value
        assertTrue(keywords.contains("Kotlin"))
        assertTrue(keywords.contains("Kubernetes"))
        assertTrue(keywords.contains("Spring Boot"))
        assertTrue(keywords.size <= 15)
        
        verify { mockAIGateway.complete(any()) }
    }
}

class RephraseSectionUseCaseTest {

    private lateinit var mockAIGateway: AIGatewayClient
    private lateinit var mockTraceLogger: TraceLogger
    private lateinit var useCase: RephraseSectionUseCase

    @Before
    fun setup() {
        mockAIGateway = mockk()
        mockTraceLogger = mockk()
        useCase = RephraseSectionUseCase(mockAIGateway, mockTraceLogger)
    }

    @Test
    fun `rephrases section with professional tone`() = runBlockingTest {
        // Given
        val originalContent = "Eu liderei uma equipe de 5 desenvolvedores e fiz migração para microsserviços"

        val aiResponse = com.resumeapp.ai.gateway.AIResponse(
            id = "ai-1",
            model = "gpt-4o-mini",
            choices = listOf(
                com.resumeapp.ai.gateway.Choice(
                    message = com.resumeapp.ai.gateway.Message(content = "Liderou equipe de 5 desenvolvedores na migração estratégica para arquitetura de microsserviços baseada em Kotlin/Spring Boot")
                )
            ),
            usage = com.resumeapp.ai.gateway.Usage(promptTokens = 80, completionTokens = 40, totalTokens = 120, costUsd = 0.0003),
            latencyMs = 400
        )

        coEvery { mockAIGateway.complete(any()) } returns Result.Success(aiResponse)

        // When
        val result = useCase(RephraseSectionUseCase.Params(
            sectionTitle = "Experiência",
            currentContent = "Eu liderei uma equipe de 5 desenvolvedores e fiz migração para microsserviços",
            tone = RephraseSectionUseCase.Tone.PROFESSIONAL,
            targetLength = RephraseSectionUseCase.Length.MEDIUM
        ))

        // Then
        assertTrue(result is Result.Success)
        val rephrased = (result as Result.Success<String>).value
        assertTrue(rephrased.contains("Liderou"))
        assertTrue(rephrased.contains("microsserviços"))
        
        verify { mockAIGateway.complete(any()) }
    }

    @Test
    fun `rephrases with concise length`() = runBlockingTest {
        // Given
        val originalContent = "Eu liderei uma grande equipe de desenvolvedores seniores e juniores na migração completa do sistema legado para uma moderna arquitetura de microsserviços usando Kotlin e Spring Boot"

        val aiResponse = com.resumeapp.ai.gateway.AIResponse(
            id = "ai-1",
            model = "gpt-4o-mini",
            choices = listOf(
                com.resumeapp.ai.gateway.Choice(
                    message = com.resumeapp.ai.gateway.Message(content = "Liderou migração completa para microsserviços (Kotlin/Spring Boot)")
                )
            ),
            usage = com.resumeapp.ai.gateway.Usage(promptTokens = 100, completionTokens = 20, totalTokens = 120, costUsd = 0.0004),
            latencyMs = 350
        )

        coEvery { mockAIGateway.complete(any()) } returns Result.Success(aiResponse)

        // When
        val result = useCase(RephraseSectionUseCase.Params(
            sectionTitle = "Resumo",
            currentContent = "Liderei equipe na migração para microsserviços com Kotlin/Spring Boot",
            tone = RephraseSectionUseCase.Tone.CONCISE,
            targetLength = RephraseSectionUseCase.Length.SHORT
        ))

        // Then
        assertTrue(result is Result.Success)
        val rephrased = (result as Result.Success<String>).value
        assertTrue(rephrased.length < 100) // Concise
    }
}

class SuggestTemplatesUseCaseTest {

    private lateinit var mockAIGateway: AIGatewayClient
    private lateinit var mockTemplateRepository: TemplateRepository
    private lateinit var useCase: SuggestTemplatesUseCase

    @Before
    fun setup() {
        mockAIGateway = mockk()
        mockTemplateRepository = mockk()
        useCase = SuggestTemplatesUseCase(mockAIGateway, mockTemplateRepository)
    }

    @Test
    fun `suggests templates based on resume content`() = runBlockingTest {
        // Given
        val resume = Resume(
            id = "resume-1",
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
                    type = SectionType.PROJECTS,
                    title = "Projetos",
                    itemsJson = "[]",
                    order = 1
                )
            )
        )

        val templates = listOf(
            Template(id = "1", name = "ATS Optimized", category = TemplateCategory.ATS_OPTIMIZED, isPremium = false),
            Template(id = "2", name = "Modern", category = TemplateCategory.MODERN, isPremium = false),
            Template(id = "3", name = "Creative", category = TemplateCategory.CREATIVE, isPremium = true),
            Template(id = "4", name = "Classic", category = TemplateCategory.CLASSIC, isPremium = false)
        )

        coEvery { mockTemplateRepository.getAllActive() } returns Result.Success(templates)

        // When
        val result = useCase(SuggestTemplatesUseCase.Params(
            resume = resume,
            targetRole = "Senior Developer",
            maxSuggestions = 2
        ))

        // Then
        assertTrue(result is Result.Success)
        val suggested = (result as Result.Success<List<Template>>).value
        assertTrue(suggested.size <= 2)
        // Should prioritize ATS_OPTIMIZED for experience-heavy resume
        assertEquals("ATS Optimized", suggested[0].name)
    }
}