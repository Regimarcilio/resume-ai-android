package com.resumeapp.data.repository

import com.resumeapp.data.db.TraceDao
import com.resumeapp.data.mapper.TraceMapper
import com.resumeapp.domain.model.TraceEntry
import com.resumeapp.domain.repository.TraceRepository
import kotlinx.coroutines.tasks.await

class TraceRepositoryImpl constructor(
    private val dao: TraceDao,
    private val mapper: TraceMapper
) : TraceRepository {

    override suspend fun getAll(): List<TraceEntry> {
        val entries = dao.getRecent(100).await()
        return entries.map { mapper.toDomain(it) }
    }

    override suspend fun getById(id: String): TraceEntry? {
        // Not typically used for traces - return null
        return null
    }

    override suspend fun save(entry: TraceEntry): TraceEntry {
        val entity = TraceMapper.toEntity(entry)
        dao.insert(entity).await()
        return entry
    }

    override suspend fun delete(id: String) {
        // Not typically used for traces
    }

    override fun observeAll(): kotlinx.coroutines.flow.Flow<List<TraceEntry>> {
        return kotlinx.coroutines.flow.emptyFlow()
    }
}