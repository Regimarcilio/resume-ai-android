package com.resumeapp.data.repository

import com.resumeapp.data.db.AppDatabase
import com.resumeapp.data.db.ResumeDao
import com.resumeapp.data.db.ResumeSectionDao
import com.resumeapp.data.mapper.ResumeMapper
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.repository.ResumeRepository
import kotlinx.coroutines.tasks.await

class ResumeRepositoryImpl constructor(
    private val database: AppDatabase,
    private val resumeDao: ResumeDao,
    private val sectionDao: ResumeSectionDao,
    private val mapper: ResumeMapper
) : ResumeRepository {

    override fun save(resume: Resume): Resume {
        val entity = mapper.toEntity(resume)
        val resumeId = database.runInTransaction { resumeDao.insert(entity) }
        
        // Save sections
        resume.sections.forEachIndexed { index, section ->
            val sectionEntity = mapper.toEntity(section, entity.id, index)
            sectionDao.insert(sectionEntity).await()
        }
        return resume
    }

    override fun update(resume: Resume): Resume {
        val entity = mapper.toEntity(resume)
        // Update resume first
        resumeDao.update(entity).await()
        
        // Update sections - delete and re-insert
        sectionDao.deleteByResumeId(entity.id).await()
        resume.sections.forEachIndexed { index, section ->
            val sectionEntity = mapper.toEntity(section, entity.id, index)
            sectionDao.insert(sectionEntity).await()
        }
        return resume
    }

    override fun getById(id: String): Resume? {
        val entity = resumeDao.getById(id).await()
        return entity?.let { mapper.toDomain(it) }
    }

    override fun getAllByUserId(userId: String): List<Resume> {
        val resumes = resumeDao.getAllByUserId(userId).await()
        return resumes.map { mapper.toDomain(it) }
    }

    override fun search(params: com.resumeapp.domain.usecase.SearchParams): List<Resume> {
        // Delegate to DAO with search params
        val resumes = resumeDao.search(userId = params.userId, query = params.query, templateId = params.templateId).await()
        return resumes.map { mapper.toDomain(it) }
    }

    override fun deleteById(id: String) {
        resumeDao.deleteById(id).await()
    }

    override fun countByUserId(userId: String): Int {
        return resumeDao.countByUserId(userId).await()
    }
}