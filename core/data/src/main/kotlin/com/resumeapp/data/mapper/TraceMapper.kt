package com.resumeapp.data.mapper

import com.resumeapp.data.db.TraceEntity
import com.resumeapp.domain.model.TraceEntry

class TraceMapper {

    fun toDomain(entity: TraceEntity): TraceEntry {
        return TraceEntry(
            id = entity.traceId,
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

    fun toEntity(entry: TraceEntry): TraceEntity {
        return TraceEntity(
            traceId = entry.id,
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