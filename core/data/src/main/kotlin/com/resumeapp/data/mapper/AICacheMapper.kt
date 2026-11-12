package com.resumeapp.data.mapper

import javax.inject.Inject
import com.resumeapp.ai.gateway.AIResponse
import com.resumeapp.ai.gateway.AIRequest
import com.resumeapp.ai.gateway.Usage
import com.resumeapp.ai.gateway.Choice
import com.resumeapp.ai.gateway.Message
import com.resumeapp.ai.cache.CacheEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AICacheMapper @Inject constructor() {

    private val gson = Gson()

    fun toDomain(entity: com.resumeapp.data.db.AICacheEntity, cacheHit: Boolean = false): CacheEntry {
        val response = gson.fromJson(entity.response, com.resumeapp.ai.gateway.AIResponse::class.java).copy(cacheHit = cacheHit)
        return CacheEntry(
            key = entity.id,
            response = response,
            createdAt = entity.createdAt,
            accessCount = 0,
            lastAccessed = System.currentTimeMillis()
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