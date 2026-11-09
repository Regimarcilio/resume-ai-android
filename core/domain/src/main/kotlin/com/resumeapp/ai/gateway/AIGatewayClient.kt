package com.resumeapp.ai.gateway

import com.resumeapp.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * AI Gateway Client Interface - EngAI Pattern
 * Routes all LLM calls through internal gateway with cache, guardrail, trace
 * Defined in domain module to avoid Android dependency
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
@Serializable
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
        get() = metadata["session_id"] ?: java.util.UUID.randomUUID().toString()
}

/**
 * AI Response DTO
 */
@Serializable
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

@Serializable
data class Choice(
    val index: Int = 0,
    val message: Message,
    val finishReason: String = "stop"
)

@Serializable
data class Message(
    val role: String = "assistant",
    val content: String
)

@Serializable
data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val costUsd: Double
)

/**
 * Streaming Response Chunk
 */
@Serializable
data class AIResponseChunk(
    val id: String,
    val model: String,
    val choices: List<StreamChoice>,
    val isDone: Boolean = false
) {
    val content: String
        get() = choices.firstOrNull()?.delta?.content ?: ""
}

@Serializable
data class StreamChoice(
    val index: Int = 0,
    val delta: StreamDelta,
    val finishReason: String? = null
)

@Serializable
data class StreamDelta(
    val content: String = ""
)