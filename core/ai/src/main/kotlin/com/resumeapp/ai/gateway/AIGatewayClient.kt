package com.resumeapp.ai.gateway

import com.resumeapp.ai.gateway.AIRequest
import com.resumeapp.ai.gateway.AIResponse
import com.resumeapp.ai.gateway.AIResponseChunk
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

/**
 * Result wrapper for AI gateway operations
 * Local to core:ai to avoid circular dependency with core:domain
 */
sealed interface Result<out T> {
    data class Success<out T>(val value: T) : Result<T>
    data class Failure(val errorMessage: String) : Result<Nothing>

    companion object {
        inline fun <T> success(value: T): Result<T> = Success(value)
        inline fun <T> failure(errorMessage: String): Result<T> = Failure(errorMessage)
    }

    fun getOrThrow(): T = when (this) {
        is Success<*> -> value as T
        is Failure -> throw IllegalStateException(errorMessage)
    }
}