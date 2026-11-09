package com.resumeapp.data.cache

import com.resumeapp.ai.cache.AICache
import com.resumeapp.ai.gateway.AIRequest
import com.resumeapp.ai.gateway.AIResponse
import com.resumeapp.data.db.AICacheDao
import com.resumeapp.data.db.AICacheEntity
import com.resumeapp.data.db.AppDatabase
import com.resumeapp.data.mapper.AICacheMapper
import jakarta.inject.Inject
import jakarta.inject.Singleton

/**
 * Room-backed AI Cache implementation - Semantic and exact caching
 * Moved to core:data to avoid circular dependency with core:ai
 */
@Singleton
class RoomAICache @Inject constructor(
    private val dao: AICacheDao,
    private val database: AppDatabase,
    private val mapper: AICacheMapper
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
            val response = mapper.toDomain(roomEntry, cacheHit = true)
            exactCache[key] = response // Populate in-memory cache
            cacheHits++
            return response
        }
        
        return null
    }

    override fun put(key: String, response: AIResponse) {
        exactCache[key] = response
        
        // Persist to Room
        val entry = mapper.toEntity(key, response)
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

    override fun getStats(): com.resumeapp.ai.cache.CacheStats {
        val size = exactCache.size + database.runInTransaction { dao.count() }
        val hitRate = if (totalRequests > 0) cacheHits.toDouble() / totalRequests else 0.0
        return com.resumeapp.ai.cache.CacheStats(size, hitRate, totalRequests, cacheHits)
    }

    private fun hashSHA256(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}