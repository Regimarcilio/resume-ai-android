package com.resumeapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.resumeapp.data.db.converters.Converters

@Entity(tableName = "ai_cache")
data class AICacheEntity(
    @PrimaryKey
    val id: String,
    val promptHash: String,
    val promptType: String,
    val embeddingJson: String,
    val response: String,
    val model: String,
    val tokensUsed: Int,
    val costUsd: Double,
    val latencyMs: Long,
    val createdAt: Long
)

@Entity(tableName = "traces")
data class TraceEntity(
    @PrimaryKey
    val traceId: String,
    val timestamp: String,
    val model: String,
    val promptType: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val costUsd: Double,
    val latencyMs: Long,
    val cacheHit: Boolean,
    val promptSanitized: String
)

@Entity(tableName = "pdf_cache")
data class PdfCacheEntity(
    @PrimaryKey
    val id: String,
    val resumeId: String,
    val template: String,
    val watermark: Boolean,
    val pdfData: ByteArray,
    val createdAt: Long
)