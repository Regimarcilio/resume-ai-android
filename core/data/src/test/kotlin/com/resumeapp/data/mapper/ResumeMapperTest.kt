package com.resumeapp.data.mapper

import com.resumeapp.data.db.ResumeEntity
import com.resumeapp.domain.model.PersonalInfo
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.model.ResumeSection
import com.resumeapp.domain.model.SectionItem
import com.resumeapp.domain.model.SectionType
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ResumeMapperTest {

    private val mapper = ResumeMapper()
    private val gson = Gson()

    @Test
    fun `converts entity to domain`() {
        // Given
        val entity = ResumeEntity(
            id = "resume-1",
            userId = "user-1",
            templateId = "template-1",
            personalInfoJson = gson.toJson(PersonalInfo(fullName = "John Doe", email = "john@example.com")),
            sectionsJson = gson.toJson(listOf(
                ResumeSection(
                    id = "section-1",
                    resumeId = "resume-1",
                    type = SectionType.EXPERIENCE,
                    title = "Experience",
                    itemsJson = gson.toJson(listOf(
                        SectionItem(title = "Senior Dev", subtitle = "Company", startDate = "2020-01", endDate = "Present", highlights = listOf("Led team"))
                    )),
                    order = 0,
                    isVisible = true
                )
            )),
            createdAt = 1000L,
            updatedAt = 2000L,
            version = 1
        )

        // When
        val domain = mapper.toDomain(entity)

        // Then
        assertEquals("resume-1", domain.id)
        assertEquals("user-1", domain.userId)
        assertEquals("template-1", domain.templateId)
        assertEquals("John Doe", domain.personalInfo.fullName)
        assertEquals(1, domain.sections.size)
        assertEquals(SectionType.EXPERIENCE, domain.sections[0].type)
        assertEquals(1, domain.sections[0].items.size)
        assertEquals("Led team", domain.sections[0].items[0].highlights[0])
    }

    @Test
    fun `converts domain to entity`() {
        // Given
        val domain = Resume(
            id = "resume-1",
            userId = "user-1",
            templateId = "template-1",
            personalInfo = PersonalInfo(fullName = "John Doe"),
            sections = listOf(
                ResumeSection(
                    id = "section-1",
                    type = SectionType.EXPERIENCE,
                    title = "Experience",
                    itemsJson = """[{"title":"Senior Dev"}]""",
                    order = 0,
                    isVisible = true
                )
            )
        )

        // When
        val entity = mapper.toEntity(domain)

        // Then
        assertEquals("resume-1", entity.id)
        assertEquals("user-1", entity.userId)
        assertEquals("template-1", entity.templateId)
        assertNotNull(entity.personalInfoJson)
        assertNotNull(entity.sectionsJson)
    }
}