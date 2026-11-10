package com.resumeapp.domain.repository

import com.resumeapp.domain.ai.cache.AICache
import com.resumeapp.ai.gateway.AIRequest
import com.resumeapp.ai.gateway.AIResponse

/**
 * AI Cache Repository Interface
 * Domain layer contract for AI caching
 */
interface AICacheRepository : AICache {
    // All methods inherited from AICache
}