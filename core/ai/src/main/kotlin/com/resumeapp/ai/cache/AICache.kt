package com.resumeapp.ai.cache

import com.resumeapp.ai.gateway.AIRequest
import com.resumeapp.ai.gateway.AIResponse

/**
 * AI Cache Interface - Semantic and exact caching
 * Implementation (RoomAICache) is in core:data module to avoid circular dependency
 */
interface AICache {
    fun generateCacheKey(request: AIRequest): String
    fun get(key: String): AIResponse?
    fun put(key: String, response: AIResponse)
    fun invalidate(key: String)
    fun clear()
    fun getStats(): CacheStats
}

data class CacheStats(
    val size: Int,
    val hitRate: Double,
    val totalRequests: Long,
    val cacheHits: Long
)