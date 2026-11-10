package com.resumeapp.ai.trace

/**
 * Trace Logger Interface
 * Defines the contract for logging AI traces
 * Implementation provided by app module (TraceLoggerImpl)
 */
interface TraceLogger {
    suspend fun log(entry: TraceEntry)
    suspend fun getTraces(limit: Int = 100): List<TraceEntry>
    suspend fun clearTraces()
}

@Serializable
data class TraceEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val request: TraceRequest,
    val response: TraceResponse?,
    val metadata: Map<String, String> = emptyMap()
) {
    fun toJson(): String = kotlinx.serialization.json.Json.encodeToString(TraceEntry.serializer(), this)
}

@Serializable
data class TraceRequest(
    val prompt: String,
    val model: String,
    val maxTokens: Int?,
    val temperature: Double?,
    val promptType: String,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class TraceResponse(
    val id: String,
    val model: String,
    val content: String,
    val usage: TraceUsage,
    val latencyMs: Long,
    val cacheHit: Boolean
)

@Serializable
data class TraceUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val costUsd: Double
)

/**
 * Trace Uploader Interface
 * Defines the contract for uploading traces to external systems
 * Implementation provided by app module (TraceUploader)
 */
interface TraceUploader {
    suspend fun upload(entry: TraceEntry)
    suspend fun uploadBatch(entries: List<TraceEntry>)
}