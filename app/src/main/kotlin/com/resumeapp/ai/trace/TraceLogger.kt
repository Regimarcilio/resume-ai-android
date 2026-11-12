package com.resumeapp.ai.trace

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Trace Logger Implementation - EngAI Pattern
 * Logs all AI interactions to JSONL files for auditability and FinOps.
 *
 * Contract lives in core:ai (com.resumeapp.ai.trace.TraceLogger).
 */
class TraceLoggerImpl @Inject constructor(
    private val context: Context
) : TraceLogger {

    private val traceDir = File(context.filesDir, "traces")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    init {
        traceDir.mkdirs()
    }

    override suspend fun log(entry: TraceEntry) {
        val line = json.encodeToString(TraceEntry.serializer(), entry) + "\n"
        FileOutputStream(getTodaysTraceFile(), true).use { it.write(line.toByteArray()) }
    }

    override suspend fun getTraces(limit: Int): List<TraceEntry> {
        val file = getTodaysTraceFile()
        if (!file.exists()) return emptyList()
        return file.useLines { lines ->
            lines.toList().asReversed().take(limit).mapNotNull { line ->
                runCatching { json.decodeFromString(TraceEntry.serializer(), line) }.getOrNull()
            }
        }
    }

    override suspend fun clearTraces() {
        val file = getTodaysTraceFile()
        if (file.exists()) file.delete()
    }

    /**
     * Total cost accumulated today (USD), derived from persisted traces.
     */
    suspend fun getTodaysCost(): Double =
        getTraces(limit = Int.MAX_VALUE).sumOf { it.response?.usage?.costUsd ?: 0.0 }

    private fun getTodaysTraceFile(): File {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return File(traceDir, "trace-$dateStr.jsonl")
    }
}

/**
 * Trace Uploader Implementation - batches traces to an observability backend.
 * No backend is configured for the offline-first build, so entries are staged
 * locally and acknowledged; a future Loki/PostHog sink can replace this body.
 */
class TraceUploaderImpl @Inject constructor(
    private val traceLogger: TraceLogger
) : TraceUploader {

    override suspend fun upload(entry: TraceEntry) {
        // Staging is a no-op while no remote sink is configured.
        traceLogger.log(entry)
    }

    override suspend fun uploadBatch(entries: List<TraceEntry>) {
        entries.forEach { upload(it) }
    }
}
