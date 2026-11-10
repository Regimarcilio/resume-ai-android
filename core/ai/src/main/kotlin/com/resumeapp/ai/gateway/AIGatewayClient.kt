package com.resumeapp.ai.gateway

import com.resumeapp.ai.gateway.AIRequest
import com.resumeapp.ai.gateway.AIResponse
import com.resumeapp.ai.gateway.AIResponseChunk
import com.resumeapp.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * AI Gateway Client Interface
 * Defines the contract for communicating with AI gateway
 * Implementation provided by app module (AIGatewayClientImpl)
 */
interface AIGatewayClient {
    suspend fun complete(request: AIRequest): Result<AIResponse>
    fun stream(request: AIRequest): Flow<AIResponseChunk>
}