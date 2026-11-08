package com.resumeapp.data.mapper

import com.resumeapp.data.db.AppDatabase
import com.resumeapp.data.db.ResumeSectionDao
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.model.ResumeSection
import com.resumeapp.domain.model.SectionItem
import com.resumeapp.domain.model.PersonalInfo
import com.resumeapp.domain.model.SectionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ResumeMapper {

    private val gson = Gson()
    private val sectionItemsType = object : TypeToken<List<SectionItem>>() {}.type
    private val stringListType = object : TypeToken<List<String>>() {}.type

    fun toDomain(entity: com.resumeapp.data.db.ResumeEntity): Resume {
        val sections = gson.fromJson(
            entity.sectionsJson, 
            TypeToken.getParameterized(List::class.java, ResumeSection::class.java).type
        ) ?: emptyList()

        val personalInfo = gson.fromJson(
            entity.personalInfoJson,
            PersonalInfo::class.java
        ) ?: PersonalInfo()

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
        return com.resumeapp.data.db.ResumeEntity(
            id = resume.id,
            userId = resume.userId,
            templateId = resume.templateId,
            personalInfoJson = gson.toJson(resume.personalInfo),
            sectionsJson = gson.toJson(resume.sections),
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
            itemsJson = gson.toJson(section.items),
            order = section.order,
            isVisible = section.isVisible
        )
    }

    fun toDomain(entity: com.resumeapp.data.db.ResumeSectionEntity): ResumeSection {
        val items = gson.fromJson(
            entity.itemsJson,
            TypeToken.getParameterized(List::class.java, SectionItem::class.java).type
        ) ?: emptyList()

        return ResumeSection(
            id = entity.id,
            resumeId = entity.resumeId,
            type = SectionType.valueOf(entity.type),
            title = entity.title,
            items = items,
            order = entity.order,
            isVisible = entity.isVisible
        )
    }
}