package com.resumeapp.ai.gateway

import com.resumeapp.ai.cache.AICache
import com.resumeapp.ai.guardrail.GuardrailPipeline
import com.resumeapp.ai.trace.TraceLogger
import com.resumeapp.ai.trace.TraceUploader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URL

class AIGatewayClientImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val guardrailPipeline: GuardrailPipeline,
    private val aiCache: AICache,
    private val traceLogger: TraceLogger,
    private val traceUploader: TraceUploader
) : AIGatewayClient {

    private val baseUrl = "http://localhost:8080" // From BuildConfig in real app
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun complete(request: AIRequest): Result<AIResponse> {
        val startTime = System.currentTimeMillis()
        val requestId = java.util.UUID.randomUUID().toString()

        // 1. Guardrail Input Sanitization
        val sanitizedPrompt = guardrailPipeline.sanitizeInput(request.prompt)
        val sanitizedRequest = request.copy(prompt = sanitizedPrompt)

        // 2. Check Cache (Semantic similarity)
        val cacheKey = aiCache.generateCacheKey(sanitizedRequest)
        val cached = aiCache.get(cacheKey)
        
        if (cached != null) {
            val latencyMs = System.currentTimeMillis() - startTime
            traceLogger.log(
                traceId = requestId,
                model = request.model,
                promptType = request.promptType,
                promptTokens = 0,
                completionTokens = 0,
                totalTokens = 0,
                costUsd = 0.0,
                latencyMs = latencyMs,
                cacheHit = true,
                promptSanitized = sanitizedPrompt
            )
            return Result.Success(cached.toAIResponse(cacheHit = true))
        }

        // 3. Call Gateway
        return try {
            val response = callGateway(sanitizedRequest, requestId)
            
            // 4. Guardrail Output Validation
            val validatedResponse = guardrailPipeline.validateOutput(response)
            
            // 5. Cache Response
            aiCache.put(cacheKey, validatedResponse)
            
            // 6. Trace Logging
            val latencyMs = System.currentTimeMillis() - startTime
            traceLogger.log(
                traceId = requestId,
                model = validatedResponse.model,
                promptType = request.promptType,
                promptTokens = validatedResponse.usage.promptTokens,
                completionTokens = validatedResponse.usage.completionTokens,
                totalTokens = validatedResponse.usage.totalTokens,
                costUsd = validatedResponse.usage.costUsd,
                latencyMs = latencyMs,
                cacheHit = false,
                promptSanitized = sanitizedRequest.prompt
            )
            
            // 7. Upload traces periodically
            traceUploader.maybeUpload()
            
            Result.Success(validatedResponse.toAIResponse(cacheHit = false))
        } catch (e: Exception) {
            val latencyMs = System.currentTimeMillis() - startTime
            traceLogger.log(
                traceId = requestId,
                model = request.model,
                promptType = request.promptType,
                promptTokens = 0,
                completionTokens = 0,
                totalTokens = 0,
                costUsd = 0.0,
                latencyMs = latencyMs,
                cacheHit = false,
                promptSanitized = sanitizedRequest.prompt
            )
            Result.Failure(e)
        }
    }

    override fun stream(request: AIRequest): Flow<AIResponseChunk> = channelFlow {
        val sanitizedPrompt = guardrailPipeline.sanitizeInput(request.prompt)
        val sanitizedRequest = request.copy(prompt = sanitizedPrompt, stream = true)
        val requestId = java.util.UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()

        val url = URL("$baseUrl/v1/stream")
        val requestBody = RequestBody.Companion.toRequestBody(
            com.resumeapp.ai.gateway.AIRequest(
                prompt = sanitizedPrompt,
                model = sanitizedRequest.model,
                maxTokens = sanitizedRequest.maxTokens,
                temperature = sanitizedRequest.temperature,
                stream = true,
                metadata = sanitizedRequest.metadata
            ).toJson(),
            "application/json; charset=utf-8".toMediaType()
        )

        val httpRequest = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${BuildConfig.AI_GATEWAY_API_KEY}")
            .header("X-Request-ID", requestId)
            .post(requestBody)
            .build()

        val call = okHttpClient.newCall(httpRequest)
        
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw RuntimeException("Stream failed: ${response.code} ${response.body?.string()}")
                }
                
                val body = response.body ?: throw RuntimeException("Empty response body")
                val source = body.source()
                
                var fullContent = ""
                var promptTokens = 0
                var completionTokens = 0
                var totalTokens = 0
                var costUsd = 0.0
                
                while (true) {
                    val line = source.readUtf8Line() ?: break
                    
                    if (line.startsWith("data: ")) {
                        val data = line.substring(6).trim()
                        
                        if (data == "[DONE]") {
                            // Stream complete
                            val latencyMs = System.currentTimeMillis() - startTime
                            traceLogger.log(
                                traceId = requestId,
                                model = request.model,
                                promptType = request.promptType,
                                promptTokens = promptTokens,
                                completionTokens = completionTokens,
                                totalTokens = totalTokens,
                                costUsd = costUsd,
                                latencyMs = latencyMs,
                                cacheHit = false,
                                promptSanitized = sanitizedPrompt
                            )
                            traceUploader.maybeUpload()
                            break
                        }
                        
                        try {
                            val chunk = com.google.gson.Gson().fromJson(data, StreamResponseChunk::class.java)
                            val content = chunk.choices.firstOrNull()?.delta?.content ?: ""
                            fullContent += content
                            
                            // Update token counts if available
                            chunk.usage?.let { u ->
                                promptTokens = u.promptTokens
                                completionTokens = u.completionTokens
                                totalTokens = u.totalTokens
                                costUsd = u.costUsd
                            }
                            
                            send(AIResponseChunk(
                                id = chunk.id,
                                model = chunk.model,
                                choices = chunk.choices.map { c ->
                                    StreamChoice(
                                        index = c.index,
                                        delta = StreamDelta(content = c.delta?.content ?: ""),
                                        finishReason = c.finishReason
                                    )
                                },
                                isDone = c.finishReason != null
                            ))
                        } catch (e: Exception) {
                            // Ignore malformed chunks
                        }
                    }
                }
            }
        } catch (e: Exception) {
            send(AIResponseChunk(
                id = "",
                model = request.model,
                choices = listOf(StreamChoice(delta = StreamDelta(content = "Error: ${e.message}"))),
                isDone = true
            ))
        } finally {
            call.cancel()
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.IO)

    override suspend fun healthCheck(): Boolean {
        val url = URL("$baseUrl/health")
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        
        return try {
            okHttpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun callGateway(request: AIRequest, requestId: String): GatewayResponse {
        val url = URL("$baseUrl/v1/complete")
        val requestBody = RequestBody.Companion.toRequestBody(
            request.toJson(),
            "application/json; charset=utf-8".toMediaType()
        )

        val httpRequest = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${BuildConfig.AI_GATEWAY_API_KEY}")
            .header("X-Request-ID", requestId)
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val call = okHttpClient.newCall(httpRequest)
        
        return call.execute().use { response ->
            val body = response.body?.string() ?: throw RuntimeException("Empty response")
            if (!response.isSuccessful) {
                throw RuntimeException("Gateway error: ${response.code} $body")
            }
            com.google.gson.Gson().fromJson(body, GatewayResponse::class.java)
        }
    }

    // Helper classes for JSON serialization
    private fun AIRequest.toJson(): String = com.google.gson.Gson().toJson(this)
    private fun GatewayResponse.toAIResponse(cacheHit: Boolean): AIResponse = AIResponse(
        id = this.id,
        model = this.model,
        choices = this.choices,
        usage = this.usage,
        latencyMs = this.latencyMs,
        cacheHit = cacheHit
    )

    private data class GatewayResponse(
        val id: String,
        val model: String,
        val choices: List<Choice>,
        val usage: Usage,
        val latencyMs: Long = 0
    )

    private data class StreamResponseChunk(
        val id: String,
        val model: String,
        val choices: List<StreamChoiceData>,
        val usage: Usage? = null
    )

    private data class StreamChoiceData(
        val index: Int = 0,
        val delta: StreamDeltaData? = null,
        val finishReason: String? = null
    )

    private data class StreamDeltaData(
        val content: String = ""
    )
}