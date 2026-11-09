package com.resumeapp.ai.trace

/**
 * Trace Logger Interface - Logging AI interactions for observability
 * Implemented in core:ai module with Room
 */
interface TraceLogger {

    /**
     * Log AI interaction
     */
    suspend fun log(
        traceId: String,
        model: String,
        promptType: String,
        promptTokens: Int,
        completionTokens: Int,
        totalTokens: Int,
        costUsd: Double,
        latencyMs: Long,
        cacheHit: Boolean,
        promptSanitized: String
    )
}

/**
 * Trace Uploader Interface - Upload traces to backend
 * Implemented in core:ai module
 */
interface TraceUploader {

    /**
     * Upload trace batch
     */
    suspend fun uploadTraces(traces: List<TraceRecord>): Boolean
}

/**
 * Trace Record for upload
 */
@Serializable
data class TraceRecord(
    val traceId: String,
    val timestamp: Long,
    val model: String,
    val promptType: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val costUsd: Double,
    val latencyMs: Long,
    val cacheHit: Boolean,
    val userIdHash: String
)