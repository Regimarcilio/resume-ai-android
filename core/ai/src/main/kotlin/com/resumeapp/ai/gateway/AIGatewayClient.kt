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

sealed interface Result<out T> {
    data class Success<out T>(val value: T) : Result<T>
    data class Failure(val exception: Throwable) : Result<Nothing>
}

fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success<T>
fun <T> Result<T>.isFailure(): Boolean = this is Result.Failure