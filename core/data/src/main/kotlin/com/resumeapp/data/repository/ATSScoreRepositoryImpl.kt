package com.resumeapp.data.repository

import com.resumeapp.data.db.ATSScoreDao
import com.resumeapp.data.mapper.ATSScoreMapper
import com.resumeapp.domain.model.ATSScore
import com.resumeapp.domain.model.Result
import com.resumeapp.domain.repository.ATSScoreRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ATSScoreRepositoryImpl @Inject constructor(
    private val atsScoreDao: ATSScoreDao,
    private val mapper: ATSScoreMapper
) : ATSScoreRepository {

    override suspend fun getAll(): Result<List<com.resumeapp.domain.model.ATSScore>> {
        return try {
            // Not typically used - ATS scores are per resume
            Result.Success(emptyList())
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun getById(id: String): Result<com.resumeapp.domain.model.ATSScore> {
        return try {
            // ATSScore uses resumeId as ID
            val entity = atsScoreDao.getByResumeId(id).await()
            entity?.let { Result.Success(mapper.toDomain(it)) }
                ?: Result.Failure(Exception("ATS Score not found for resume: $id"))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun save(score: com.resumeapp.domain.model.ATSScore): Result<com.resumeapp.domain.model.ATSScore> {
        return try {
            val entity = mapper.toEntity(score)
            atsScoreDao.insert(entity).await()
            Result.Success(score)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun delete(id: String): Result<Unit> {
        return try {
            atsScoreDao.deleteByResumeId(id).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override fun observe(resumeId: String): kotlinx.coroutines.flow.Flow<com.resumeapp.domain.model.ATSScore?> {
        return atsScoreDao.observeByResumeId(resumeId)
            .map { entity -> entity?.let { mapper.toDomain(it) } }
    }
}