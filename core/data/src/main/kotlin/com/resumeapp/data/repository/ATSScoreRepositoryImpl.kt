package com.resumeapp.data.repository

import javax.inject.Inject
import com.resumeapp.data.db.ATSScoreDao
import com.resumeapp.data.mapper.ATSScoreMapper
import com.resumeapp.domain.model.ATSScore
import com.resumeapp.domain.repository.ATSScoreRepository

class ATSScoreRepositoryImpl @Inject constructor(
    private val atsScoreDao: ATSScoreDao,
    private val mapper: ATSScoreMapper
) : ATSScoreRepository {

    override suspend fun save(score: ATSScore): ATSScore {
        val entity = mapper.toEntity(score)
        atsScoreDao.insert(entity)
        return score
    }

    override suspend fun getByResumeId(resumeId: String): ATSScore? {
        val entity = atsScoreDao.getByResumeId(resumeId)
        return entity?.let { mapper.toDomain(it) }
    }

    override suspend fun getAllByUserId(userId: String): List<ATSScore> {
        val entities = atsScoreDao.getAllByUserId(userId)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun delete(score: ATSScore) {
        atsScoreDao.deleteByResumeId(score.resumeId)
    }
}