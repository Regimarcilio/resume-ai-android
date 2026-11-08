package com.resumeapp.data.mapper

import com.resumeapp.data.db.TemplateEntity
import com.resumeapp.domain.model.Template
import com.resumeapp.domain.model.TemplateCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TemplateMapperTest {

    private val mapper = TemplateMapper()

    @Test
    fun `converts entity to domain`() {
        // Given
        val entity = TemplateEntity(
            id = "template-1",
            name = "Modern Template",
            description = "Clean and modern design",
            category = TemplateCategory.MODERN.name,
            previewImageUrl = "https://example.com/preview.png",
            configJson = "{}",
            isPremium = false,
            isActive = true
        )

        // When
        val domain = mapper.toDomain(entity)

        // Then
        assertEquals("template-1", domain.id)
        assertEquals("Modern Template", domain.name)
        assertEquals("Clean and modern design", domain.description)
        assertEquals(TemplateCategory.MODERN, domain.category)
        assertEquals("https://example.com/preview.png", domain.previewImageUrl)
        assertEquals("{}", domain.configJson)
        assertEquals(false, domain.isPremium)
        assertEquals(true, domain.isActive)
    }

    @Test
    fun `converts domain to entity`() {
        // Given
        val domain = Template(
            id = "template-1",
            name = "Modern Template",
            description = "Clean and modern design",
            category = TemplateCategory.MODERN,
            previewImageUrl = "https://example.com/preview.png",
            configJson = "{}",
            isPremium = false,
            isActive = true
        )

        // When
        val entity = mapper.toEntity(domain)

        // Then
        assertEquals("template-1", entity.id)
        assertEquals("Modern Template", entity.name)
        assertEquals("Clean and modern design", entity.description)
        assertEquals(TemplateCategory.MODERN.name, entity.category)
        assertEquals("https://example.com/preview.png", entity.previewImageUrl)
        assertEquals("{}", entity.configJson)
        assertEquals(false, entity.isPremium)
        assertEquals(true, entity.isActive)
    }
}