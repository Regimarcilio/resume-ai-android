package com.resumeapp.domain.repository

import com.resumeapp.ai.trace.TraceEntry
import com.resumeapp.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Trace operations (AI interaction logging)
 * Implemented in core:data module with Room + file-based logging
 */
interface TraceRepository {

    /**
     * Get all traces (recent first)
     */
    suspend fun getAll(): Result<List<TraceEntry>>

    /**
     * Get trace by ID
     */
    suspend fun getById(id: String): Result<TraceEntry>

    /**
     * Save a trace entry
     */
    suspend fun save(entry: TraceEntry): Result<TraceEntry>

    /**
     * Delete trace by ID
     */
    suspend fun delete(id: String): Result<Unit>

    /**
     * Observe all traces as flow
     */
    fun observeAll(): Flow<List<TraceEntry>>
}