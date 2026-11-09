package com.resumeapp.data.repository

import com.resumeapp.data.db.TemplateDao
import com.resumeapp.data.mapper.TemplateMapper
import com.resumeapp.domain.model.Template
import com.resumeapp.domain.model.Result
import com.resumeapp.domain.repository.TemplateRepository
import kotlinx.coroutines.tasks.await
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val templateDao: TemplateDao,
    private val mapper: TemplateMapper
) : TemplateRepository {

    override suspend fun getAll(): Result<List<Template>> {
        return try {
            val templates = templateDao.getAllActive().await()
            Result.Success(templates.map { mapper.toDomain(it) })
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun getById(id: String): Result<Template> {
        return try {
            val entity = templateDao.getById(id).await()
            entity?.let { Result.Success(mapper.toDomain(it)) }
                ?: Result.Failure(Exception("Template not found: $id"))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun save(template: Template): Result<Template> {
        return try {
            val entity = mapper.toEntity(template)
            templateDao.insert(entity).await()
            Result.Success(template)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun delete(id: String): Result<Unit> {
        return try {
            templateDao.deleteById(id).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override fun observeAll(): kotlinx.coroutines.flow.Flow<List<Template>> {
        return templateDao.observeAllActive()
            .map { entities -> entities.map { mapper.toDomain(it) } }
    }
}