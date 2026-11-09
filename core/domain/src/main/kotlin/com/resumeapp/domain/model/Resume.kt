package com.resumeapp.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Resume(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val templateId: String,
    val personalInfo: PersonalInfo = PersonalInfo(),
    val sections: List<ResumeSection> = emptyList(),
    val atsScore: ATSScore? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Int = 1
) {
    fun copyWithUpdatedSections(sections: List<ResumeSection>): Resume = copy(
        sections = sections,
        updatedAt = System.currentTimeMillis(),
        version = version + 1
    )
}

@Serializable
data class PersonalInfo(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val linkedin: String = "",
    val portfolio: String = "",
    val summary: String = ""
)

@Serializable
data class ResumeSection(
    val id: String = UUID.randomUUID().toString(),
    val resumeId: String,
    val type: SectionType = SectionType.CUSTOM,
    val title: String = "",
    val itemsJson: String = "[]", // Serialized List<SectionItem>
    val order: Int = 0,
    val isVisible: Boolean = true
) {
    // Helper to get items as typed list (requires Converters)
    // This is handled by TypeConverters in Room
}

@Serializable
enum class SectionType {
    EXPERIENCE, EDUCATION, SKILLS, PROJECTS, CERTIFICATIONS, LANGUAGES, CUSTOM
}

@Serializable
data class SectionItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val subtitle: String = "",
    val description: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val highlights: List<String> = emptyList(),
    val keywords: List<String> = emptyList()
)

@Serializable
data class Template(
    val id: String,
    val name: String,
    val description: String,
    val category: TemplateCategory = TemplateCategory.MODERN,
    val previewImageUrl: String = "",
    val configJson: String = "{}",
    val isPremium: Boolean = false,
    val isActive: Boolean = true
)

@Serializable
enum class TemplateCategory {
    MODERN, CLASSIC, CREATIVE, ATS_OPTIMIZED
}

@Serializable
data class ATSScore(
    val resumeId: String,
    val overallScore: Int = 0,
    val keywordMatch: Int = 0,
    val formattingScore: Int = 0,
    val sectionCompleteness: Int = 0,
    val missingKeywordsJson: String = "[]",
    val suggestionsJson: String = "[]",
    val analyzedAt: Long = System.currentTimeMillis()
)

@Serializable
data class ATSSuggestion(
    val sectionId: String,
    val type: SuggestionType,
    val priority: Int, // 1=high, 2=medium, 3=low
    val message: String,
    val example: String? = null
)

@Serializable
enum class SuggestionType {
    ADD_KEYWORD, REPHRASE, ADD_METRIC, FORMAT_FIX, REORDER
}

@Serializable
data class AICacheEntry(
    val id: String = UUID.randomUUID().toString(),
    val promptHash: String,
    val promptType: String,
    val embeddingJson: String = "[]", // FloatArray serialized as JSON
    val response: String = "",
    val model: String = "",
    val tokensUsed: Int = 0,
    val costUsd: Double = 0.0,
    val latencyMs: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class TraceEntry(
    val id: String = UUID.randomUUID().toString(),
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

@Serializable
data class ConsentRecord(
    val version: String,
    val timestamp: Long,
    val policyHash: String,
    val accepted: Boolean,
    val purposes: Set<ConsentPurpose>
)

@Serializable
enum class ConsentPurpose {
    GENERATE_RESUME, AI_ASSIST, ATS_SCORING, ANALYTICS
}

@Serializable
enum class SubscriptionTier {
    FREE, PRO, LIFETIME, ENTERPRISE
}

@Serializable
enum class FeatureFlag {
    UNLIMITED_RESUMES,
    UNLIMITED_AI_ASSIST,
    ATS_FULL,
    PREMIUM_TEMPLATES,
    NO_WATERMARK,
    ENTERPRISE_DASHBOARD,
    API_ACCESS,
    PRIORITY_SUPPORT
}

@Serializable
data class SubscriptionStatus(
    val tier: SubscriptionTier = SubscriptionTier.FREE,
    val expiryDate: Long? = null,
    val isActive: Boolean = false,
    val purchaseToken: String? = null,
    val features: Set<FeatureFlag> = setOf()
)

@Serializable
data class SubscriptionPlan(
    val productId: String,
    val name: String,
    val description: String,
    val price: String,
    val tier: SubscriptionTier,
    val isSubscription: Boolean, // true for recurring, false for lifetime
    val trialDays: Int = 0
)