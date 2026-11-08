package com.resumeapp.ai.gateway

import com.resumeapp.ai.cache.AICache
import com.resumeapp.ai.guardrail.GuardrailPipeline
import com.resumeapp.ai.trace.TraceLogger
import com.resumeapp.ai.trace.TraceUploader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * AI Gateway Client Interface - EngAI Pattern
 * Routes all LLM calls through internal gateway with cache, guardrail, trace
 */
interface AIGatewayClient {

    /**
     * Non-streaming completion
     */
    suspend fun complete(request: AIRequest): Result<AIResponse>

    /**
     * Streaming completion (Server-Sent Events)
     */
    fun stream(request: AIRequest): Flow<AIResponseChunk>

    /**
     * Health check
     */
    suspend fun healthCheck(): Boolean
}

/**
 * AI Request DTO
 */
data class AIRequest(
    val prompt: String,
    val model: String = "gpt-4o-mini",
    val maxTokens: Int = 1000,
    val temperature: Float = 0.7f,
    val stream: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
) {
    val promptType: String
        get() = metadata["prompt_type"] ?: "general"
    
    val userIdHash: String
        get() = metadata["user_id"] ?: "anonymous"
    
    val sessionId: String
        get() = metadata["session_id"] ?: UUID.randomUUID().toString()
}

/**
 * AI Response DTO
 */
data class AIResponse(
    val id: String,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage,
    val latencyMs: Long,
    val cacheHit: Boolean = false
) {
    val content: String
        get() = choices.firstOrNull()?.message?.content ?: ""
    
    val promptType: String
        get() = "general" // Set by caller
}

data class Choice(
    val index: Int = 0,
    val message: Message,
    val finishReason: String = "stop"
)

data class Message(
    val role: String = "assistant",
    val content: String
)

data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val costUsd: Double
)

/**
 * Streaming Response Chunk
 */
data class AIResponseChunk(
    val id: String,
    val model: String,
    val choices: List<StreamChoice>,
    val isDone: Boolean = false
) {
    val content: String
        get() = choices.firstOrNull()?.delta?.content ?: ""
}

data class StreamChoice(
    val index: Int = 0,
    val delta: StreamDelta,
    val finishReason: String? = null
)

data class StreamDelta(
    val content: String = ""
)

/**
 * Result wrapper for error handling
 */
sealed interface Result<out T> {
    data class Success<out T>(val value: T) : Result<T>
    data class Failure(val error: Throwable) : Result<Nothing>
    
    fun isSuccess(): Boolean = this is Success
    fun isFailure(): Boolean = this is Failure
    
    fun getOrNull(): T? = when(this) {
        is Success -> value
        is Failure -> null
    }
    
    fun getOrThrow(): T = when(this) {
        is Success -> value
        is Failure -> throw error
    }
    
    fun onSuccess(action: (T) -> Unit): Result<T> = when(this) {
        is Success -> action(value)
        is Failure -> Unit
    }.also { return this }
    
    fun onFailure(action: (Throwable) -> Unit): Result<T> = when(this) {
        is Failure -> action(error)
        is Success -> Unit
    }.also { return this }
}

extension <T> Result<T>.map<U>(transform: (T) -> U): Result<U> = when(this) {
    is Success -> Result.Success(transform(value))
    is Failure -> this
}

extension <T> Result<T>.flatMap<U>(transform: (T) -> Result<U>): Result<U> = when(this) {
    is Success -> transform(value)
    is Failure -> this
}