package com.resumeapp.data.mapper

import javax.inject.Inject
import com.resumeapp.data.db.AppDatabase
import com.resumeapp.data.db.ResumeSectionDao
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.model.ResumeSection
import com.resumeapp.domain.model.SectionItem
import com.resumeapp.domain.model.PersonalInfo
import com.resumeapp.domain.model.SectionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class ResumeMapper @Inject constructor() {

    private val gson = Gson()
    private val sectionItemsType: Type = object : TypeToken<List<SectionItem>>() {}.type
    private val stringListType: Type = object : TypeToken<List<String>>() {}.type
    private val resumeSectionListType: Type = object : TypeToken<List<ResumeSection>>() {}.type

    fun toDomain(entity: com.resumeapp.data.db.ResumeEntity): Resume {
        val sections: List<ResumeSection> = if (entity.sectionsJson.isNullOrBlank()) {
            emptyList()
        } else {
            gson.fromJson(entity.sectionsJson, resumeSectionListType) ?: emptyList()
        }

        val personalInfo: PersonalInfo = if (entity.personalInfoJson.isNullOrBlank()) {
            PersonalInfo()
        } else {
            gson.fromJson(entity.personalInfoJson, PersonalInfo::class.java) ?: PersonalInfo()
        }

        return Resume(
            id = entity.id,
            userId = entity.userId,
            templateId = entity.templateId,
            personalInfo = personalInfo,
            sections = sections,
            atsScore = null, // Loaded separately
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            version = entity.version
        )
    }

    fun toEntity(resume: Resume): com.resumeapp.data.db.ResumeEntity {
        val personalInfo = resume.personalInfo
        return com.resumeapp.data.db.ResumeEntity(
            id = resume.id,
            userId = resume.userId,
            templateId = resume.templateId,
            fullName = personalInfo.fullName,
            email = personalInfo.email,
            phone = personalInfo.phone,
            location = personalInfo.location,
            linkedin = personalInfo.linkedin,
            portfolio = personalInfo.portfolio,
            summary = personalInfo.summary,
            personalInfoJson = gson.toJson(personalInfo),
            sectionsJson = gson.toJson(resume.sections),
            atsScoreJson = resume.atsScore?.let { gson.toJson(it) } ?: "",
            createdAt = resume.createdAt,
            updatedAt = resume.updatedAt,
            version = resume.version
        )
    }

    fun toEntity(section: ResumeSection, resumeId: String, order: Int): com.resumeapp.data.db.ResumeSectionEntity {
        return com.resumeapp.data.db.ResumeSectionEntity(
            id = section.id,
            resumeId = resumeId,
            type = section.type.name,
            title = section.title,
            itemsJson = section.itemsJson, // Use the JSON string directly
            order = section.order,
            isVisible = section.isVisible
        )
    }

    fun toDomain(entity: com.resumeapp.data.db.ResumeSectionEntity): ResumeSection {
        val items: List<SectionItem> = if (entity.itemsJson.isNullOrBlank()) {
            emptyList()
        } else {
            gson.fromJson(entity.itemsJson, sectionItemsType) ?: emptyList()
        }

        return ResumeSection(
            id = entity.id,
            resumeId = entity.resumeId,
            type = SectionType.valueOf(entity.type),
            title = entity.title,
            itemsJson = entity.itemsJson,
            order = entity.order,
            isVisible = entity.isVisible
        )
    }
}