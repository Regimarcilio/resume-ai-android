package com.resumeapp.domain.ai.cache

import com.resumeapp.ai.gateway.AIRequest
import com.resumeapp.ai.gateway.AIResponse
import kotlinx.serialization.Serializable

/**
 * AI Cache Interface - Semantic and exact caching
 * Implementation (RoomAICache) is in core:data module to avoid circular dependency
 */
interface AICache {
    fun generateCacheKey(request: AIRequest): String
    suspend fun get(key: String): AIResponse?
    suspend fun put(key: String, response: AIResponse)
    suspend fun invalidate(key: String)
    suspend fun clear()
    suspend fun getStats(): CacheStats
}

data class CacheStats(
    val size: Int,
    val hitRate: Double,
    val totalRequests: Long,
    val cacheHits: Long
)

/**
 * Cache entry for Room persistence
 */
@Serializable
data class CachedResponse(
    val key: String,
    val response: AIResponse,
    val createdAt: Long = System.currentTimeMillis()
)