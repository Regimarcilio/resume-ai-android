package com.resumeapp.data.repository

import com.resumeapp.data.db.AICacheDao
import com.resumeapp.data.db.AppDatabase
import com.resumeapp.data.mapper.AICacheMapper
import com.resumeapp.ai.cache.AICache
import com.resumeapp.ai.gateway.AIResponse
import com.resumeapp.ai.gateway.AIRequest
import com.resumeapp.domain.model.Result
import kotlinx.coroutines.tasks.await
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class AICacheRepositoryImpl @Inject constructor(
    private val dao: AICacheDao,
    private val database: AppDatabase,
    private val mapper: AICacheMapper
) : AICache {

    override fun generateCacheKey(request: com.resumeapp.ai.gateway.AIRequest): String {
        val content = "${request.promptType}:${request.model}:${request.prompt}"
        return hashSHA256(content)
    }

    override fun get(key: String): AIResponse? {
        val entry = database.runInTransaction { dao.getByPromptHash(key, "") }
        return entry?.let { mapper.toDomain(it, cacheHit = true) }
    }

    override fun put(key: String, response: com.resumeapp.ai.gateway.AIResponse) {
        val entry = mapper.toEntity(key, response)
        database.runInTransaction { dao.insert(entry).await() }
    }

    override fun invalidate(key: String) {
        database.runInTransaction { dao.deleteById(key) }
    }

    override fun clear() {
        database.runInTransaction { dao.deleteAll() }
    }

    override fun getStats(): com.resumeapp.ai.cache.CacheStats {
        val size = database.runInTransaction { dao.count() }
        return com.resumeapp.ai.cache.CacheStats(
            size = size,
            hitRate = 0.0, // Would need tracking
            totalRequests = 0,
            cacheHits = 0
        )
    }

    private fun hashSHA256(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}