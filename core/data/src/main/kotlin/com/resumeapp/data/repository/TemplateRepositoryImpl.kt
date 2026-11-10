package com.resumeapp.data.repository

import com.resumeapp.data.db.TemplateDao
import com.resumeapp.data.mapper.TemplateMapper
import com.resumeapp.domain.model.Template
import com.resumeapp.domain.model.TemplateCategory
import com.resumeapp.domain.repository.TemplateRepository
import kotlinx.coroutines.tasks.await

class TemplateRepositoryImpl constructor(
    private val templateDao: TemplateDao,
    private val mapper: TemplateMapper
) : TemplateRepository {

    override fun getAllActive(): List<Template> {
        val templates = templateDao.getAllActive().await()
        return templates.map { mapper.toDomain(it) }
    }

    override fun getById(id: String): Template? {
        val entity = templateDao.getById(id).await()
        return entity?.let { mapper.toDomain(it) }
    }

    override fun getByCategory(category: TemplateCategory): List<Template> {
        val templates = templateDao.getByCategory(category.name).await()
        return templates.map { mapper.toDomain(it) }
    }

    override fun getPremiumTemplates(): List<Template> {
        val templates = templateDao.getPremiumTemplates().await()
        return templates.map { mapper.toDomain(it) }
    }

    override fun getFreeTemplates(): List<Template> {
        val templates = templateDao.getFreeTemplates().await()
        return templates.map { mapper.toDomain(it) }
    }

    override fun save(template: Template): Template {
        val entity = mapper.toEntity(template)
        templateDao.insert(entity).await()
        return template
    }

    override fun update(template: Template): Template {
        val entity = mapper.toEntity(template)
        templateDao.update(entity).await()
        return template
    }
}