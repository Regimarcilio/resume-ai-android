package com.resumeapp.data.cache

import com.resumeapp.domain.ai.cache.AICache
import com.resumeapp.domain.ai.cache.CacheStats
import com.resumeapp.domain.ai.cache.CachedResponse
import com.resumeapp.ai.gateway.AIRequest
import com.resumeapp.ai.gateway.AIResponse
import com.resumeapp.data.db.AICacheDao
import com.resumeapp.data.db.AICacheEntity
import com.resumeapp.data.db.AppDatabase
import com.resumeapp.data.mapper.AICacheMapper

/**
 * Room-backed AI Cache implementation - Semantic and exact caching
 * Moved to core:data to avoid circular dependency with core:ai
 */
class RoomAICache constructor(
    private val dao: AICacheDao,
    private val database: AppDatabase,
    private val mapper: AICacheMapper
) : AICache {

    private val exactCache = java.util.concurrent.ConcurrentHashMap<String, CachedResponse>()
    private var totalRequests = 0L
    private var cacheHits = 0L

    override fun generateCacheKey(request: AIRequest): String {
        val content = "${request.promptType}:${request.model}:${request.prompt}"
        return hashSHA256(content)
    }

    override suspend fun get(key: String): AIResponse? {
        totalRequests++

        // Check exact cache first (fast)
        exactCache[key]?.let { cached ->
            cacheHits++
            return cached.response.copy(cacheHit = true)
        }

        // Check Room cache (persisted)
        val entry = database.runInTransaction { dao.getByPromptHash(key, "") }
        entry?.let { roomEntry ->
            val response = mapper.toDomain(roomEntry, cacheHit = true)
            val cached = CachedResponse(key, response)
            exactCache[key] = cached // Populate in-memory cache
            cacheHits++
            return response
        }

        return null
    }

    override suspend fun put(key: String, response: AIResponse) {
        val cached = CachedResponse(key, response)
        exactCache[key] = cached

        // Persist to Room
        val entry = mapper.toEntity(key, response)
        database.runInTransaction { dao.insert(entry) }
    }

    override suspend fun invalidate(key: String) {
        exactCache.remove(key)
        database.runInTransaction { dao.deleteById(key) }
    }

    override suspend fun clear() {
        exactCache.clear()
        database.runInTransaction { dao.deleteAll() }
    }

    override suspend fun getStats(): CacheStats {
        val size = exactCache.size + database.runInTransaction { dao.count() }
        val hitRate = if (totalRequests > 0) cacheHits.toDouble() / totalRequests else 0.0
        return CacheStats(size, hitRate, totalRequests, cacheHits)
    }

    private fun hashSHA256(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}