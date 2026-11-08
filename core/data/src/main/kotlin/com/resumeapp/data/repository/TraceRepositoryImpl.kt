package com.resumeapp.data.repository

import com.resumeapp.data.db.TraceDao
import com.resumeapp.data.mapper.TraceMapper
import com.resumeapp.ai.trace.TraceEntry
import com.resumeapp.domain.model.Result
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TraceRepositoryImpl @Inject constructor(
    private val dao: TraceDao,
    private val mapper: TraceMapper
) : com.resumeapp.domain.repository.TraceRepository {

    override suspend fun getAll(): Result<List<com.resumeapp.ai.trace.TraceEntry>> {
        return try {
            val entries = dao.getRecent(100).await()
            Result.Success(entries.map { mapper.toDomain(it) })
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun getById(id: String): Result<com.resumeapp.ai.trace.TraceEntry> {
        return try {
            Result.Failure(Exception("Not implemented for traces"))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun save(entry: com.resumeapp.ai.trace.TraceEntry): Result<com.resumeapp.ai.trace.TraceEntry> {
        return try {
            val entity = com.resumeapp.data.mapper.TraceMapper.toEntity(entry)
            Result.Success(entry)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun delete(id: String): Result<Unit> {
        return try {
            Result.Failure(Exception("Not implemented for traces"))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override fun observeAll(): kotlinx.coroutines.flow.Flow<List<com.resumeapp.ai.trace.TraceEntry>> {
        return kotlinx.coroutines.flow.emptyFlow()
    }
}