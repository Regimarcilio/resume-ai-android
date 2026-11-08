package com.resumeapp.domain.usecase.template

import com.resumeapp.domain.model.Result
import com.resumeapp.domain.model.Template
import com.resumeapp.domain.model.TemplateCategory
import com.resumeapp.domain.repository.TemplateRepository
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

class GetTemplatesUseCaseTest {

    private val testDispatcher = TestDispatcher()
    private lateinit var mockRepository: TemplateRepository
    private lateinit var useCase: GetTemplatesUseCase

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = GetTemplatesUseCase(mockRepository)
    }

    @Test
    fun `returns all active templates`() = runBlockingTest {
        // Given
        val templates = listOf(
            Template(id = "1", name = "Modern", category = TemplateCategory.MODERN, isPremium = false, isActive = true),
            Template(id = "2", name = "Classic", category = TemplateCategory.CLASSIC, isPremium = true, isActive = true),
            Template(id = "3", name = "Creative", category = TemplateCategory.CREATIVE, isPremium = false, isActive = true)
        )
        coEvery { mockRepository.getAllActive() } returns Result.Success(templates)

        // When
        val result = useCase(GetTemplatesUseCase.Params())

        // Then
        assertTrue(result is Result.Success)
        assertEquals(3, (result as Result.Success<List<Template>>).value.size)
    }

    @Test
    fun `filters by category`() = runBlockingTest {
        // Given
        val templates = listOf(
            Template(id = "1", name = "Modern", category = TemplateCategory.MODERN, isActive = true),
            Template(id = "2", name = "Classic", category = TemplateCategory.CLASSIC, isActive = true)
        )
        coEvery { mockRepository.getByCategory(com.resumeapp.domain.model.TemplateCategory.MODERN) } returns Result.Success(listOf(templates[0]))

        // When
        val result = useCase(GetTemplatesUseCase.Params(category = TemplateCategory.MODERN))

        // Then
        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success<List<Template>>).value.size)
        assertEquals("Modern", (result as Result.Success<List<Template>>).value[0].name)
    }

    @Test
    fun `filters premium only`() = runBlockingTest {
        // Given
        val templates = listOf(
            Template(id = "1", name = "Premium Template", category = TemplateCategory.MODERN, isPremium = true, isActive = true),
            Template(id = "2", name = "Free Template", category = TemplateCategory.CLASSIC, isPremium = false, isActive = true)
        )
        coEvery { mockRepository.getPremiumTemplates() } returns Result.Success(listOf(templates[0]))

        // When
        val result = useCase(GetTemplatesUseCase.Params(premiumOnly = true))

        // Then
        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success<List<Template>>).value.size)
        assertTrue((result as Result.Success<List<Template>>).value[0].isPremium)
    }
}

class GetTemplateByIdUseCaseTest {

    private lateinit var mockRepository: TemplateRepository
    private lateinit var useCase: GetTemplateByIdUseCase

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = GetTemplateByIdUseCase(mockRepository)
    }

    @Test
    fun `returns template when found`() = runBlockingTest {
        // Given
        val template = Template(id = "template-1", name = "Test Template", category = TemplateCategory.MODERN)
        coEvery { mockRepository.getById("template-1") } returns Result.Success(template)

        // When
        val result = useCase(com.resumeapp.domain.usecase.IdParams("template-1"))

        // Then
        assertTrue(result is Result.Success)
        assertEquals("Test Template", (result as Result.Success<Template>).value.name)
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