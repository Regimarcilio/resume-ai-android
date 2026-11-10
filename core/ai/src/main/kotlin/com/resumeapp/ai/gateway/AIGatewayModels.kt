package com.resumeapp.ai.gateway

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * AI Gateway Data Models
 * Shared between core:ai, core:data, and app modules
 */

@Serializable
data class AIRequest(
    val prompt: String,
    val model: String = "gpt-3.5-turbo",
    val maxTokens: Int? = 2048,
    val temperature: Double? = 0.7,
    val stream: Boolean = false,
    val promptType: String = "general",
    val metadata: Map<String, String>? = null
) {
    fun toJson(): String = kotlinx.serialization.json.Json.encodeToString(AIRequest.serializer(), this)
}

@Serializable
data class AIResponse(
    val id: String,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage,
    val latencyMs: Long = 0,
    val cacheHit: Boolean = false
) {
    fun toJson(): String = kotlinx.serialization.json.Json.encodeToString(AIResponse.serializer(), this)
}

@Serializable
data class Choice(
    val index: Int = 0,
    val message: Message,
    val finishReason: String? = null
)

@Serializable
data class Message(
    val role: String = "assistant",
    val content: String = ""
)

@Serializable
data class Usage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val costUsd: Double = 0.0
)

@Serializable
data class AIResponseChunk(
    val id: String,
    val model: String,
    val choices: List<StreamChoice>,
    val isDone: Boolean = false
)

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

/**
 * Result wrapper for AI operations
 */
sealed interface Result<out T> {
    data class Success<out T>(val value: T) : Result<T>
    data class Failure(val exception: Throwable) : Result<Nothing>
}

fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success<T>
fun <T> Result<T>.isFailure(): Boolean = this is Result.Failure

/**
 * Extension to convert GatewayResponse to AIResponse
 * (Internal to AIGatewayClientImpl, kept here for type sharing)
 */
@Serializable
internal data class GatewayResponse(
    val id: String,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage,
    val latencyMs: Long = 0
)

@Serializable
internal data class StreamResponseChunk(
    val id: String,
    val model: String,
    val choices: List<StreamChoiceData>,
    val usage: Usage? = null
)

@Serializable
internal data class StreamChoiceData(
    val index: Int = 0,
    val delta: StreamDeltaData? = null,
    val finishReason: String? = null
)

@Serializable
internal data class StreamDeltaData(
    val content: String = ""
)