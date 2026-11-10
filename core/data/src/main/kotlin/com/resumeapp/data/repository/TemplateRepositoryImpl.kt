package com.resumeapp.data.repository

import com.resumeapp.data.db.TemplateDao
import com.resumeapp.data.mapper.TemplateMapper
import com.resumeapp.domain.model.Template
import com.resumeapp.domain.model.TemplateCategory
import com.resumeapp.domain.repository.TemplateRepository

class TemplateRepositoryImpl constructor(
    private val templateDao: TemplateDao,
    private val mapper: TemplateMapper
) : TemplateRepository {

    override suspend fun getAllActive(): List<Template> {
        val templates = templateDao.getAllActive()
        return templates.map { mapper.toDomain(it) }
    }

    override suspend fun getById(id: String): Template? {
        val entity = templateDao.getById(id)
        return entity?.let { mapper.toDomain(it) }
    }

    override suspend fun getByCategory(category: TemplateCategory): List<Template> {
        val templates = templateDao.getByCategory(category.name)
        return templates.map { mapper.toDomain(it) }
    }

    override suspend fun getPremiumTemplates(): List<Template> {
        val templates = templateDao.getPremiumTemplates()
        return templates.map { mapper.toDomain(it) }
    }

    override suspend fun getFreeTemplates(): List<Template> {
        val templates = templateDao.getFreeTemplates()
        return templates.map { mapper.toDomain(it) }
    }

    override suspend fun save(template: Template): Template {
        val entity = mapper.toEntity(template)
        templateDao.insert(entity)
        return template
    }

    override suspend fun update(template: Template): Template {
        val entity = mapper.toEntity(template)
        templateDao.update(entity)
        return template
    }
}