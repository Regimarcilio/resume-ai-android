package com.resumeapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.resumeapp.data.db.converters.Converters

@Entity(tableName = "resumes")
data class ResumeEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val templateId: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val location: String,
    val linkedin: String,
    val portfolio: String,
    val summary: String,
    val sectionsJson: String?, // Serialized List<ResumeSection>
    val personalInfoJson: String?, // Serialized PersonalInfo
    val atsScoreJson: String?, // Serialized ATSScore
    val createdAt: Long,
    val updatedAt: Long,
    val version: Int
)

@Entity(tableName = "resume_sections")
data class ResumeSectionEntity(
    @PrimaryKey
    val id: String,
    val resumeId: String,
    val type: String, // SectionType enum serialized
    val title: String,
    val itemsJson: String, // Serialized List<SectionItem>
    val order: Int,
    val isVisible: Boolean
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val category: String, // TemplateCategory enum serialized
    val previewImageUrl: String,
    val configJson: String,
    val isPremium: Boolean,
    val isActive: Boolean
)

@Entity(tableName = "ats_scores")
data class ATSScoreEntity(
    @PrimaryKey
    val resumeId: String,
    val overallScore: Int,
    val keywordMatch: Int,
    val formattingScore: Int,
    val sectionCompleteness: Int,
    val missingKeywordsJson: String,
    val suggestionsJson: String,
    val analyzedAt: Long
)

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

@Entity(tableName = "subscription_status")
data class SubscriptionStatusEntity(
    @PrimaryKey
    val tier: String, // Serialized SubscriptionTier enum
    val expiryDate: Long?,
    val isActive: Boolean,
    val purchaseToken: String?,
    val featuresJson: String // Serialized Set<FeatureFlag>
)