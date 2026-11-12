package com.resumeapp.ai.gateway

import com.resumeapp.ai.cache.AICache
import com.resumeapp.ai.guardrail.GuardrailPipeline
import com.resumeapp.ai.trace.TraceLogger
import com.resumeapp.ai.trace.TraceRequest
import com.resumeapp.ai.trace.TraceResponse
import com.resumeapp.ai.trace.TraceUploader
import com.resumeapp.ai.trace.TraceUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import javax.inject.Inject

/**
 * AI Gateway Client Implementation - EngAI Pattern.
 *
 * Translates the domain-facing [AIRequest]/[AIResponse] contract into the
 * OpenAI-compatible wire format exposed by the mock gateway
 * (`POST /v1/chat/completions`, SSE streaming).
 *
 * Pipeline: guardrail(sanitize) -> semantic cache -> HTTP -> guardrail(validate)
 *           -> cache store -> trace logging.
 */
class AIGatewayClientImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val guardrailPipeline: GuardrailPipeline,
    private val aiCache: AICache,
    private val traceLogger: TraceLogger,
    private val traceUploader: TraceUploader
) : AIGatewayClient {

    private val baseUrl = "http://10.0.2.2:8081" // Android emulator -> host localhost
    private val apiKey = "dev-key-change-in-prod"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ==================== Non-streaming ====================

    override suspend fun complete(request: AIRequest): Result<AIResponse> {
        val startTime = System.currentTimeMillis()

        // 1. Guardrail - input sanitization
        val sanitizedPrompt = guardrailPipeline.sanitizeInput(request.prompt)
        val sanitizedRequest = request.copy(prompt = sanitizedPrompt)

        // 2. Semantic cache lookup
        val cacheKey = generateCacheKey(sanitizedRequest)
        val cached = aiCache.get(cacheKey)
        if (cached != null) {
            val latencyMs = System.currentTimeMillis() - startTime
            logTrace(sanitizedRequest, cached, latencyMs, cacheHit = true)
            return Result.Success(cached.copy(cacheHit = true))
        }

        // 3. Gateway call
        return try {
            val wire = callGateway(sanitizedRequest)
            val latencyMs = System.currentTimeMillis() - startTime

            val response = AIResponse(
                id = wire.id,
                model = wire.model,
                choices = wire.choices.map { choice ->
                    Choice(
                        index = choice.index,
                        message = Message(role = choice.message.role, content = choice.message.content),
                        finishReason = choice.finishReason
                    )
                },
                usage = Usage(
                    promptTokens = wire.usage.promptTokens,
                    completionTokens = wire.usage.completionTokens,
                    totalTokens = wire.usage.totalTokens,
                    costUsd = 0.0
                ),
                latencyMs = latencyMs,
                cacheHit = false
            )

            // 4. Guardrail - output validation
            val validated = guardrailPipeline.validateOutput(response)

            // 5. Cache store
            aiCache.put(cacheKey, validated)

            // 6. Trace logging
            logTrace(sanitizedRequest, validated, latencyMs, cacheHit = false)
            traceUploader.upload(
                buildTraceEntry(sanitizedRequest, validated, latencyMs, cacheHit = false)
            )

            Result.Success(validated)
        } catch (e: Exception) {
            val latencyMs = System.currentTimeMillis() - startTime
            logTrace(sanitizedRequest, null, latencyMs, cacheHit = false)
            Result.Failure(e.message ?: "AI gateway call failed")
        }
    }

    // ==================== Streaming ====================

    override fun stream(request: AIRequest): Flow<AIResponseChunk> = channelFlow {
        val startTime = System.currentTimeMillis()
        val sanitizedPrompt = guardrailPipeline.sanitizeInput(request.prompt)
        val sanitizedRequest = request.copy(prompt = sanitizedPrompt, stream = true)

        val httpRequest = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .header("X-API-Key", apiKey)
            .header("Content-Type", "application/json")
            .post(buildWireBody(sanitizedRequest).toRequestBody(jsonMediaType))
            .build()

        val call = okHttpClient.newCall(httpRequest)
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw RuntimeException("Gateway error: ${response.code} ${response.body?.string()}")
                }
                val source = (response.body ?: throw RuntimeException("Empty response body")).source()

                while (true) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data: ")) continue

                    val data = line.substring(6).trim()
                    if (data == "[DONE]") break

                    val chunk = runCatching {
                        json.decodeFromString(WireChunk.serializer(), data)
                    }.getOrNull() ?: continue

                    send(
                        AIResponseChunk(
                            id = chunk.id,
                            model = chunk.model,
                            choices = chunk.choices.map { choice ->
                                StreamChoice(
                                    index = choice.index,
                                    delta = StreamDelta(content = choice.delta?.content ?: ""),
                                    finishReason = choice.finishReason
                                )
                            },
                            isDone = chunk.choices.any { it.finishReason != null }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            send(
                AIResponseChunk(
                    id = "",
                    model = request.model,
                    choices = listOf(StreamChoice(delta = StreamDelta(content = "Erro: ${e.message}"))),
                    isDone = true
                )
            )
        } finally {
            call.cancel()
        }

        logTrace(sanitizedRequest, null, System.currentTimeMillis() - startTime, cacheHit = false)
    }.flowOn(Dispatchers.IO)

    // ==================== Internals ====================

    private suspend fun callGateway(request: AIRequest): WireResponse {
        val httpRequest = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .header("X-API-Key", apiKey)
            .header("Content-Type", "application/json")
            .post(buildWireBody(request).toRequestBody(jsonMediaType))
            .build()

        okHttpClient.newCall(httpRequest).execute().use { response ->
            val body = response.body?.string() ?: throw RuntimeException("Empty response body")
            if (!response.isSuccessful) {
                throw RuntimeException("Gateway error: ${response.code} $body")
            }
            return json.decodeFromString(WireResponse.serializer(), body)
        }
    }

    private fun buildWireBody(request: AIRequest): String {
        val wire = WireRequest(
            messages = listOf(WireMessage(role = "user", content = request.prompt)),
            model = request.model,
            temperature = request.temperature,
            maxTokens = request.maxTokens,
            stream = request.stream,
            metadata = request.metadata
        )
        return json.encodeToString(WireRequest.serializer(), wire)
    }

    private fun generateCacheKey(request: AIRequest): String {
        val raw = "${request.model}:${request.prompt}:${request.temperature}:${request.maxTokens}"
        return MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun buildTraceEntry(
        request: AIRequest,
        response: AIResponse?,
        latencyMs: Long,
        cacheHit: Boolean
    ) = com.resumeapp.ai.trace.TraceEntry(
        request = TraceRequest(
            prompt = request.prompt,
            model = request.model,
            maxTokens = request.maxTokens,
            temperature = request.temperature,
            promptType = request.promptType,
            metadata = request.metadata.orEmpty()
        ),
        response = response?.let {
            com.resumeapp.ai.trace.TraceResponse(
                id = it.id,
                model = it.model,
                content = it.choices.firstOrNull()?.message?.content.orEmpty(),
                usage = TraceUsage(
                    promptTokens = it.usage.promptTokens,
                    completionTokens = it.usage.completionTokens,
                    totalTokens = it.usage.totalTokens,
                    costUsd = it.usage.costUsd
                ),
                latencyMs = it.latencyMs,
                cacheHit = cacheHit
            )
        },
        metadata = mapOf("cacheHit" to cacheHit.toString())
    )

    private suspend fun logTrace(
        request: AIRequest,
        response: AIResponse?,
        latencyMs: Long,
        cacheHit: Boolean
    ) {
        runCatching {
            traceLogger.log(buildTraceEntry(request, response, latencyMs, cacheHit))
        }
    }
}

// ==================== Wire format (OpenAI-compatible) ====================

@Serializable
private data class WireRequest(
    val messages: List<WireMessage>,
    val model: String,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val stream: Boolean = false,
    val metadata: Map<String, String>? = null
)

@Serializable
private data class WireMessage(
    val role: String,
    val content: String
)

@Serializable
private data class WireResponse(
    val id: String = "",
    val model: String = "",
    val choices: List<WireChoice> = emptyList(),
    val usage: WireUsage = WireUsage()
)

@Serializable
private data class WireChoice(
    val index: Int = 0,
    val message: WireMessage = WireMessage(role = "assistant", content = ""),
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
private data class WireUsage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0
)

@Serializable
private data class WireChunk(
    val id: String = "",
    val model: String = "",
    val choices: List<WireChunkChoice> = emptyList()
)

@Serializable
private data class WireChunkChoice(
    val index: Int = 0,
    val delta: WireDelta? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
private data class WireDelta(
    val content: String = ""
)
