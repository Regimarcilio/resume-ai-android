package com.resumeapp.data.mapper

import com.resumeapp.ai.trace.TraceEntry

class TraceMapper {

    fun toDomain(entity: com.resumeapp.data.db.TraceEntity): com.resumeapp.ai.trace.TraceEntry {
        return com.resumeapp.ai.trace.TraceEntry(
            traceId = entity.traceId,
            timestamp = entity.timestamp,
            model = entity.model,
            promptType = entity.promptType,
            promptTokens = entity.promptTokens,
            completionTokens = entity.completionTokens,
            totalTokens = entity.totalTokens,
            costUsd = entity.costUsd,
            latencyMs = entity.latencyMs,
            cacheHit = entity.cacheHit,
            promptSanitized = entity.promptSanitized
        )
    }

    fun toEntity(entry: com.resumeapp.ai.trace.TraceEntry): com.resumeapp.data.db.TraceEntity {
        return com.resumeapp.data.db.TraceEntity(
            traceId = entry.traceId,
            timestamp = entry.timestamp,
            model = entry.model,
            promptType = entry.promptType,
            promptTokens = entry.promptTokens,
            completionTokens = entry.completionTokens,
            totalTokens = entry.totalTokens,
            costUsd = entry.costUsd,
            latencyMs = entry.latencyMs,
            cacheHit = entry.cacheHit,
            promptSanitized = entry.promptSanitized
        )
    }
}