package com.resumeapp.data.cache

import com.resumeapp.ai.cache.AICache
import com.resumeapp.ai.cache.CacheStats
import com.resumeapp.ai.cache.CacheEntry
import com.resumeapp.ai.gateway.AIRequest
import com.resumeapp.ai.gateway.AIResponse
import com.resumeapp.data.db.AICacheDao
import com.resumeapp.data.db.AICacheEntity
import com.resumeapp.data.db.AppDatabase
import com.resumeapp.data.mapper.AICacheMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Room-backed AI Cache implementation - Semantic and exact caching
 * Moved to core:data to avoid circular dependency with core:ai
 */
class RoomAICache constructor(
    private val dao: AICacheDao,
    private val database: AppDatabase,
    private val mapper: AICacheMapper
) : AICache {

    private val exactCache = java.util.concurrent.ConcurrentHashMap<String, AIResponse>()
    private var totalRequests = 0L
    private var cacheHits = 0L

    fun generateCacheKey(request: AIRequest): String {
        val content = "${request.promptType}:${request.model}:${request.prompt}"
        return hashSHA256(content)
    }

    override suspend fun get(key: String): AIResponse? {
        totalRequests++

        // Check exact cache first (fast)
        exactCache[key]?.let { cached ->
            cacheHits++
            return cached.copy(cacheHit = true)
        }

        // Check Room cache (persisted)
        val entry = withContext(Dispatchers.IO) { dao.getByPromptHash(key, "") }
        entry?.let { roomEntry ->
            val response = mapper.toDomain(roomEntry, cacheHit = true).response
            exactCache[key] = response.copy(cacheHit = true) // Populate in-memory cache
            cacheHits++
            return response.copy(cacheHit = true)
        }

        return null
    }

    override suspend fun put(key: String, response: AIResponse) {
        exactCache[key] = response

        // Persist to Room
        val entry = mapper.toEntity(key, response)
        withContext(Dispatchers.IO) { dao.insert(entry) }
    }

    override suspend fun remove(key: String) {
        exactCache.remove(key)
        withContext(Dispatchers.IO) { dao.deleteById(key) }
    }

    override suspend fun clear() {
        exactCache.clear()
        withContext(Dispatchers.IO) { dao.deleteAll() }
    }

    override suspend fun stats(): CacheStats {
        val dbCount = withContext(Dispatchers.IO) { dao.count() }
        val size = exactCache.size.toLong() + dbCount
        val misses = totalRequests - cacheHits
        return CacheStats(
            hits = cacheHits,
            misses = misses,
            size = size,
            evictions = 0
        )
    }

    override fun stream(): Flow<CacheEntry> = flow {
        // Emit all cached entries from memory
        exactCache.forEach { (key, response) ->
            emit(CacheEntry(
                key = key,
                response = response,
                createdAt = System.currentTimeMillis(),
                accessCount = 0,
                lastAccessed = System.currentTimeMillis()
            ))
        }
        // Also emit from Room (asynchronously)
        val roomEntries = withContext(Dispatchers.IO) { dao.getAllCached() }
        roomEntries.forEach { entity ->
            val entry = mapper.toDomain(entity)
            emit(entry)
        }
    }

    private fun hashSHA256(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}