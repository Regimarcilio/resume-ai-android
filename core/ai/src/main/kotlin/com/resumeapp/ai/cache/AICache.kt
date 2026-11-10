package com.resumeapp.ai.cache

import com.resumeapp.ai.gateway.AIRequest
import com.resumeapp.ai.gateway.AIResponse
import kotlinx.coroutines.flow.Flow

/**
 * AI Cache Interface
 * Defines the contract for semantic caching of AI responses
 * Implementation provided by core:data module (RoomAICache)
 */
interface AICache {
    suspend fun get(key: String): AIResponse?
    suspend fun put(key: String, response: AIResponse)
    suspend fun remove(key: String)
    suspend fun clear()
    fun stats(): CacheStats
    fun stream(): Flow<CacheEntry>
}

data class CacheStats(
    val hits: Long = 0,
    val misses: Long = 0,
    val size: Long = 0,
    val evictions: Long = 0
) {
    val hitRate: Double
        get() = if (hits + misses > 0) hits.toDouble() / (hits + misses) else 0.0
}

@Serializable
data class CacheEntry(
    val key: String,
    val response: AIResponse,
    val createdAt: Long,
    val accessCount: Long,
    val lastAccessed: Long
)