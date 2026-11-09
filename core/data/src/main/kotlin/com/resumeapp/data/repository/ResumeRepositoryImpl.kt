package com.resumeapp.data.repository

import com.resumeapp.data.db.AppDatabase
import com.resumeapp.data.db.ResumeDao
import com.resumeapp.data.db.ResumeSectionDao
import com.resumeapp.data.mapper.ResumeMapper
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.model.Result
import com.resumeapp.domain.repository.ResumeRepository
import kotlinx.coroutines.tasks.await
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class ResumeRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val resumeDao: ResumeDao,
    private val sectionDao: ResumeSectionDao,
    private val mapper: ResumeMapper
) : ResumeRepository {

    override suspend fun getAll(): Result<List<Resume>> {
        return try {
            val resumes = resumeDao.getAllByUserId(getCurrentUserId()).await()
            val domainResumes = resumes.map { mapper.toDomain(it) }
            Result.Success(domainResumes)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun getById(id: String): Result<Resume> {
        return try {
            val entity = resumeDao.getById(id).await()
            entity?.let { Result.Success(mapper.toDomain(it)) }
                ?: Result.Failure(Exception("Resume not found: $id"))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun save(resume: Resume): Result<Resume> {
        return try {
            val entity = mapper.toEntity(resume)
            database.runInTransaction {
                val id = resumeDao.insert(entity).await()
                val savedEntity = resumeDao.getById(entity.id).await()!!
                
                // Save sections
                resume.sections.forEachIndexed { index, section ->
                    val sectionEntity = mapper.toEntity(section, entity.id, index)
                    sectionDao.insert(sectionEntity).await()
                }
                
                mapper.toDomain(resumeDao.getById(entity.id).await()!!)
            }
            
            Result.Success(mapper.toDomain(resumeDao.getById(resume.id).await()!!))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun delete(id: String): Result<Unit> {
        return try {
            resumeDao.deleteById(id).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override fun observeAll(): kotlinx.coroutines.flow.Flow<List<Resume>> {
        return resumeDao.observeAllByUserId(getCurrentUserId())
            .map { entities -> entities.map { mapper.toDomain(it) } }
    }

    private fun getCurrentUserId(): String {
        // TODO: Get from UserPrefs or auth
        return "current_user"
    }
}