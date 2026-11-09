package com.resumeapp.ai.cache

import kotlinx.serialization.Serializable

/**
 * AI Cache Interface - Semantic cache for AI responses
 * Implemented in core:ai module with Room
 */
interface AICache {

    /**
     * Generate cache key from request
     */
    fun generateCacheKey(request: com.resumeapp.ai.gateway.AIRequest): String

    /**
     * Get cached response
     */
    fun get(key: String): CachedResponse?

    /**
     * Put response in cache
     */
    fun put(key: String, response: CachedResponse)

    /**
     * Invalidate cache entries
     */
    fun invalidate(pattern: String)
}

/**
 * Cached AI Response
 */
@Serializable
data class CachedResponse(
    val response: com.resumeapp.ai.gateway.AIResponse,
    val cachedAt: Long = System.currentTimeMillis(),
    val hitCount: Int = 0
) {
    fun toAIResponse(cacheHit: Boolean): com.resumeapp.ai.gateway.AIResponse = response.copy(cacheHit = cacheHit)
}