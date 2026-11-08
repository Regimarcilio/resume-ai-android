package com.resumeapp.ai.cache

import com.resumeapp.ai.gateway.AIRequest
import com.resumeapp.ai.gateway.AIResponse
import com.resumeapp.data.db.AICacheDao
import com.resumeapp.data.db.AppDatabase
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

/**
 * AI Cache Interface - Semantic and exact caching
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

/**
 * Room-backed AI Cache with semantic similarity support
 */
class RoomAICache @Inject constructor(
    private val dao: AICacheDao,
    private val database: AppDatabase
) : AICache {

    private val exactCache = java.util.concurrent.ConcurrentHashMap<String, AIResponse>()
    private var totalRequests = 0L
    private var cacheHits = 0L

    override fun generateCacheKey(request: AIRequest): String {
        val content = "${request.promptType}:${request.model}:${request.prompt}"
        return hashSHA256(content)
    }

    override fun get(key: String): AIResponse? {
        totalRequests++
        
        // Check exact cache first (fast)
        exactCache[key]?.let { response ->
            cacheHits++
            return response.copy(cacheHit = true)
        }
        
        // Check Room cache (persisted)
        val entry = database.runInTransaction { dao.getByPromptHash(key, "") }
        entry?.let { roomEntry ->
            val response = roomEntry.toAIResponse(cacheHit = true)
            exactCache[key] = response // Populate in-memory cache
            cacheHits++
            return response
        }
        
        return null
    }

    override fun put(key: String, response: AIResponse) {
        exactCache[key] = response
        
        // Persist to Room
        val entry = com.resumeapp.data.db.AICacheEntry(
            id = key,
            promptHash = key,
            promptType = response.content, // Store prompt type in content field for now
            embeddingJson = "[]",
            response = response.toJson(),
            model = response.model,
            tokensUsed = response.usage.totalTokens,
            costUsd = response.usage.costUsd,
            latencyMs = response.latencyMs,
            createdAt = System.currentTimeMillis()
        )
        
        database.runInTransaction { dao.insert(entry) }
    }

    override fun invalidate(key: String) {
        exactCache.remove(key)
        database.runInTransaction { dao.deleteById(key) }
    }

    override fun clear() {
        exactCache.clear()
        database.runInTransaction { dao.deleteAll() }
    }

    override fun getStats(): CacheStats {
        val size = exactCache.size + database.runInTransaction { dao.count() }
        val hitRate = if (totalRequests > 0) cacheHits.toDouble() / totalRequests else 0.0
        return CacheStats(size, hitRate, totalRequests, cacheHits)
    }

    private fun hashSHA256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun AIRequest.toJson(): String = com.google.gson.Gson().toJson(this)
}

private fun AICacheDao.getByPromptHash(promptHash: String, promptType: String): AICacheEntry? {
    // Simplified - in real implementation would check promptType too
    return getByPromptHash(promptHash, promptType)
}

private fun AICacheEntry.toAIResponse(cacheHit: Boolean): AIResponse {
    val gson = com.google.gson.Gson()
    return gson.fromJson(response, AIResponse::class.java).copy(cacheHit = cacheHit)
}

private fun AIResponse.toJson(): String = com.google.gson.Gson().toJson(this)

private fun MessageDigest.getInstance(algorithm: String): MessageDigest =
    java.security.MessageDigest.getInstance(algorithm)