package com.resumeapp.ai.trace

import android.content.Context
import com.google.gson.Gson
import com.resumeapp.domain.model.TraceEntry
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Trace Logger Implementation - EngAI Pattern
 * Logs all AI interactions to JSONL files for auditability and FinOps
 * Interface is in core:ai module
 */
class TraceLoggerImpl @Inject constructor(
    private val context: Context
) : com.resumeapp.ai.trace.TraceLogger {

    private val traceDir = File(context.filesDir, "traces")
    private val gson = Gson()
    private val ioExecutor = Executors.newSingleThreadExecutor()
    
    init {
        traceDir.mkdirs()
    }

    override fun log(
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
    ) {
        val entry = TraceEntry(
            id = traceId,
            timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
            model = model,
            promptType = promptType,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = totalTokens,
            costUsd = costUsd,
            latencyMs = latencyMs,
            cacheHit = cacheHit,
            promptSanitized = promptSanitized
        )
        
        ioExecutor.execute {
            val file = getTodaysTraceFile()
            val jsonLine = entry.toJson() + "\n"
            FileOutputStream(file, true).use { it.write(jsonLine.toByteArray()) }
        }
    }

    override fun getTraceFlow(): Flow<TraceEntry> {
        // Not implemented - would need to read files
        return kotlinx.coroutines.flow.emptyFlow()
    }

    override fun rotateLogs() {
        ioExecutor.execute {
            // Move current log to dated archive
            val currentFile = File(traceDir, "trace-current.jsonl")
            if (currentFile.exists()) {
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val archiveFile = File(traceDir, "trace-$dateStr.jsonl")
                currentFile.renameTo(archiveFile)
            }
        }
    }

    override fun getTodaysCost(): Double {
        val file = getTodaysTraceFile()
        if (!file.exists()) return 0.0
        
        var totalCost = 0.0
        file.forEachLine { line ->
            try {
                val entry = Gson().fromJson(line, TraceEntry::class.java)
                totalCost += entry.costUsd
            } catch (e: Exception) {
                // Skip malformed lines
            }
        }
        return totalCost
    }

    private fun getTodaysTraceFile(): File {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return File(traceDir, "trace-$dateStr.jsonl")
    }
}

/**
 * Trace Uploader - Batch uploads traces to observability backend
 */
class TraceUploader @Inject constructor(
    private val traceLogger: com.resumeapp.ai.trace.TraceLogger,
    private val context: Context
) {
    
    private val uploadThreshold = 100 // Upload after 100 traces
    private var traceCount = 0
    private val ioExecutor = Executors.newSingleThreadExecutor()
    
    fun maybeUpload() {
        traceCount++
        if (traceCount >= uploadThreshold) {
            upload()
        }
    }
    
    private fun upload() {
        ioExecutor.execute {
            // In production, upload to observability backend (PostHog, Grafana Loki, etc.)
            // For now, just rotate logs
            traceLogger.rotateLogs()
            traceCount = 0
        }
    }
}

private fun TraceEntry.toJson(): String = com.google.gson.Gson().toJson(this)