package com.resumeapp.data.mapper

import com.resumeapp.ai.gateway.AIResponse
import com.resumeapp.ai.gateway.AIRequest
import com.resumeapp.ai.gateway.Usage
import com.resumeapp.ai.gateway.Choice
import com.resumeapp.ai.gateway.Message
import com.resumeapp.domain.ai.cache.CachedResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AICacheMapper {

    private val gson = Gson()

    fun toDomain(entity: com.resumeapp.data.db.AICacheEntity, cacheHit: Boolean = false): com.resumeapp.domain.ai.cache.CachedResponse {
        val response = gson.fromJson(entity.response, com.resumeapp.ai.gateway.AIResponse::class.java).copy(cacheHit = cacheHit)
        return CachedResponse(
            key = entity.id,
            response = response,
            createdAt = entity.createdAt
        )
    }

    fun toEntity(key: String, response: com.resumeapp.ai.gateway.AIResponse): com.resumeapp.data.db.AICacheEntity {
        return com.resumeapp.data.db.AICacheEntity(
            id = key,
            promptHash = key,
            promptType = "general", // Would be extracted from request metadata
            embeddingJson = "[]",
            response = gson.toJson(response),
            model = response.model,
            tokensUsed = response.usage.totalTokens,
            costUsd = response.usage.costUsd,
            latencyMs = response.latencyMs,
            createdAt = System.currentTimeMillis()
        )
    }
}