package com.resumeapp.domain.usecase.ats

import com.resumeapp.ai.cache.AICache
import com.resumeapp.ai.gateway.AIGatewayClient
import com.resumeapp.ai.gateway.AIRequest
import com.resumeapp.ai.gateway.AIResponse
import com.resumeapp.domain.model.ATSScore
import com.resumeapp.domain.model.ATSSuggestion
import com.resumeapp.domain.model.Result
import com.resumeapp.domain.model.Result.Success
import com.resumeapp.domain.model.Result.Failure
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.model.SectionItem
import com.resumeapp.domain.model.SectionType
import com.resumeapp.domain.model.SuggestionType
import com.resumeapp.domain.repository.ATSScoreRepository
import com.resumeapp.domain.repository.ResumeRepository
import com.resumeapp.domain.usecase.AsyncUseCase
import com.resumeapp.domain.usecase.IdParams
import com.resumeapp.domain.usecase.SearchParams
import kotlinx.serialization.Serializable

/**
 * Analyze ATS UseCase
 * Analyzes a resume against a job description (optional) and returns ATS score
 */
class AnalyzeATSUseCase constructor(
    private val atsScoreRepository: ATSScoreRepository,
    private val resumeRepository: ResumeRepository,
    private val aiGatewayClient: AIGatewayClient,
    private val aiCache: AICache
) : AsyncUseCase<AnalyzeATSUseCase.Params, ATSScore> {

    @Serializable
    data class Params(
        val resumeId: String,
        val jobDescription: String? = null,
        val forceRefresh: Boolean = false
    )

    override suspend operator fun invoke(params: Params): Result<ATSScore> {
        return try {
            // Check if we have a cached score and not forcing refresh
            if (!params.forceRefresh) {
                val cached = atsScoreRepository.getByResumeId(params.resumeId)
                if (cached != null) {
                    return Success(cached)
                }
            }

            val resume = resumeRepository.getById(params.resumeId)
                ?: return Failure("Resume not found: ${params.resumeId}")

            // 1. Local analysis (fast, no AI needed)
            val localScores = analyzeLocally(resume)

            // 2. AI-powered keyword extraction and matching
            val aiAnalysis = if (params.jobDescription != null && params.jobDescription.isNotBlank()) {
                analyzeWithAI(resume, params.jobDescription!!)
            } else {
                analyzeWithAI(resume, "")
            }

            // 3. Combine scores
            val overallScore = calculateOverallScore(localScores, aiAnalysis)
            val suggestions = generateSuggestions(resume, aiAnalysis, localScores)

            val score = ATSScore(
                resumeId = params.resumeId,
                overallScore = overallScore,
                keywordMatch = aiAnalysis.keywordMatch,
                formattingScore = localScores.formatting,
                sectionCompleteness = localScores.completeness,
                missingKeywordsJson = aiAnalysis.missingKeywords.toString(),
                suggestionsJson = suggestions.toString(),
                analyzedAt = System.currentTimeMillis()
            )

            // Save to repository
            val saved = atsScoreRepository.save(score)

            Success(saved)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    private data class LocalScores(
        val formatting: Int,
        val completeness: Int
    )

    private fun analyzeLocally(resume: Resume): LocalScores {
        // Formatting score based on structure
        var formattingScore = 100

        // Check for required sections
        val requiredSections = setOf(
            SectionType.EXPERIENCE,
            SectionType.EDUCATION,
            SectionType.SKILLS
        )

        val hasSections = resume.sections.map { it.type }.toSet()
        val missingRequired = requiredSections - hasSections
        formattingScore -= missingRequired.size * 10

        // Check for empty sections
        val emptySections = resume.sections.count {
            it.itemsJson == "[]" || it.itemsJson.isBlank()
        }
        formattingScore -= emptySections * 5

        formattingScore = formattingScore.coerceAtLeast(0)

        // Completeness score
        var completeness = 100
        val personalInfo = resume.personalInfo
        if (personalInfo.fullName.isBlank()) completeness -= 20
        if (personalInfo.email.isBlank()) completeness -= 15
        if (personalInfo.phone.isBlank()) completeness -= 10
        if (personalInfo.summary.isBlank()) completeness -= 10

        val sectionCompleteness = resume.sections.filter { it.itemsJson != "[]" }.size * 100 / maxOf(1, resume.sections.size)
        completeness = (completeness + sectionCompleteness) / 2

        return LocalScores(
            formatting = formattingScore.coerceAtLeast(0),
            completeness = completeness.coerceAtLeast(0)
        )
    }

    private data class AIAnalysis(
        val keywordMatch: Int,
        val missingKeywords: List<String>
    )

    private suspend fun analyzeWithAI(resume: Resume, jobDescription: String): AIAnalysis {
        // Build prompt for AI
        val resumeText = buildResumeText(resume)
        val prompt = if (jobDescription.isNotBlank()) {
            """
            Analise este currículo contra a descrição da vaga:
            
            CURRÍCULO:
            $resumeText
            
            DESCRIÇÃO DA VAGA:
            $jobDescription
            
            Retorne JSON com:
            - keywordMatch: 0-100
            - missingKeywords: ["keyword1", "keyword2", ...]
            """
        } else {
            """
            Analise este currículo para ATS:
            
            $resumeText
            
            Retorne JSON com:
            - keywordMatch: 0-100
            - missingKeywords: ["keyword1", "keyword2", ...]
            """
        }

        val request = AIRequest(
            prompt = prompt,
            model = "gpt-4o-mini",
            maxTokens = 500,
            temperature = 0.3f,
            metadata = mapOf("prompt_type" to "ats_keywords")
        )

        val result = aiGatewayClient.complete(request)

        return when (result) {
            is Result.Success -> {
                val aiResponse = result.value
                val content = aiResponse.choices.firstOrNull()?.message?.content ?: ""
                parseAIResponse(content)
            }
            is Result.Failure -> {
                // Fallback: basic keyword extraction
                extractKeywordsFallback(resume)
            }
        }
    }

    private fun parseAIResponse(content: String): AIAnalysis {
        // Parse JSON response from AI
        try {
            val json = content.trim()
            if (json.startsWith("{") && json.endsWith("}")) {
                // Simple parsing without external deps
                val keywordMatch = extractJsonValue(json, "keywordMatch")?.toIntOrNull() ?: 50
                val missingKeywordsJson = extractJsonValue(json, "missingKeywords") ?: "[]"
                val missingKeywords = parseJsonArray(missingKeywordsJson)
                
                return AIAnalysis(
                    keywordMatch = keywordMatch.coerceIn(0, 100),
                    missingKeywords = missingKeywords
                )
            }
        } catch (e: Exception) {
            // Ignore and use fallback
        }
        
        return AIAnalysis(
            keywordMatch = 50,
            missingKeywords = emptyList()
        )
    }

    private fun extractJsonValue(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*([^,}]+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.trim()?.trim('"')
    }

    private fun parseJsonArray(json: String): List<String> {
        return json
            .removePrefix("[")
            .removeSuffix("]")
            .split(",")
            .map { it.trim().trim('"') }
            .filter { it.isNotBlank() }
    }

    private fun extractKeywordsFallback(resume: Resume): AIAnalysis {
        // Basic keyword extraction from resume content
        val allText = buildResumeText(resume).lowercase()
        val commonKeywords = listOf(
            "Kotlin", "Java", "Spring", "React", "Android", "iOS", "AWS", "Docker",
            "Kubernetes", "SQL", "NoSQL", "Microservices", "REST", "GraphQL",
            "Agile", "Scrum", "CI/CD", "Git", "Testing", "Clean Code"
        )

        val found = commonKeywords.filter { it.lowercase() in allText }
        val missing = commonKeywords - found

        return AIAnalysis(
            keywordMatch = (found.size * 100 / commonKeywords.size).coerceAtMost(100),
            missingKeywords = missing.take(10)
        )
    }

    private fun buildResumeText(resume: Resume): String {
        val sb = StringBuilder()
        sb.append("Nome: ${resume.personalInfo.fullName}\n")
        sb.append("Email: ${resume.personalInfo.email}\n")
        sb.append("Resumo: ${resume.personalInfo.summary}\n\n")

        resume.sections.forEach { section ->
            sb.append("${section.title.uppercase()}:\n")
            // Simple JSON parsing without external deps
            val items = parseSectionItems(section.itemsJson)
            
            items.forEach { item ->
                sb.append("- ${item.title} ${item.subtitle} (${item.startDate} - ${item.endDate})\n")
                item.highlights.forEach { sb.append("  * $it\n") }
            }
            sb.append("\n")
        }

        return sb.toString()
    }

    private fun parseSectionItems(json: String): List<SectionItem> {
        // Very basic JSON parsing - in real impl would use proper JSON parser
        if (json == "[]" || json.isBlank()) return emptyList()
        // This is a simplified placeholder - real implementation would use kotlinx.serialization
        return emptyList()
    }

    private fun calculateOverallScore(local: LocalScores, ai: AIAnalysis): Int {
        // Weighted average: 30% formatting, 30% completeness, 40% keyword match
        return ((local.formatting * 0.3) + (local.completeness * 0.3) + (ai.keywordMatch * 0.4)).toInt().coerceIn(0, 100)
    }

    private fun generateSuggestions(
        resume: Resume,
        ai: AIAnalysis,
        local: LocalScores
    ): List<ATSSuggestion> {
        val suggestions = mutableListOf<ATSSuggestion>()
        var priority = 1

        // Missing keywords suggestions
        ai.missingKeywords.take(5).forEach { keyword ->
            suggestions.add(ATSSuggestion(
                sectionId = "skills",
                type = SuggestionType.ADD_KEYWORD,
                priority = priority++,
                message = "Adicione a keyword '$keyword' na seção de Habilidades",
                example = "$keyword"
            ))
        }

        // Formatting suggestions
        if (local.formatting < 80) {
            suggestions.add(ATSSuggestion(
                sectionId = "general",
                type = SuggestionType.FORMAT_FIX,
                priority = priority++,
                message = "Melhore a formatação: use bullets consistentes, evite tabelas e colunas",
                example = "• Bullet points\n• Formato consistente\n• Fontes padrão"
            ))
        }

        // Completeness suggestions
        if (local.completeness < 80) {
            suggestions.add(ATSSuggestion(
                sectionId = "summary",
                type = SuggestionType.ADD_METRIC,
                priority = priority++,
                message = "Complete seu resumo profissional com 3-4 linhas destacando conquistas",
                example = "Desenvolvedor Full Stack com 5+ anos... Reduziu tempo de deploy em 40%..."
            ))
        }

        // Missing required sections
        val requiredSections = setOf(
            SectionType.EXPERIENCE,
            SectionType.EDUCATION,
            SectionType.SKILLS
        )
        val hasSections = resume.sections.map { it.type }.toSet()
        (requiredSections - hasSections).forEach { missing ->
            suggestions.add(ATSSuggestion(
                sectionId = missing.name.lowercase(),
                type = SuggestionType.FORMAT_FIX,
                priority = priority++,
                message = "Adicione seção obrigatória: ${missing.name.replace("_", " ").lowercase()}",
                example = null
            ))
        }

        return suggestions.take(10)
    }
}

/**
 * Get ATS Score UseCase
 * Retrieves existing ATS score for a resume
 */
class GetATSScoreUseCase constructor(
    private val atsScoreRepository: ATSScoreRepository
) : AsyncUseCase<IdParams, ATSScore> {

    override suspend operator fun invoke(params: IdParams): Result<ATSScore> {
        return try {
            val score = atsScoreRepository.getByResumeId(params.id)
                ?: return Failure("ATS score not found for resume: ${params.id}")
            Success(score)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }
}

/**
 * Apply ATS Suggestion UseCase
 * Applies a suggestion from ATS analysis to the resume
 */
class ApplyATSSuggestionUseCase constructor(
    private val resumeRepository: ResumeRepository
) : AsyncUseCase<ApplyATSSuggestionUseCase.Params, Resume> {

    @Serializable
    data class Params(
        val resumeId: String,
        val suggestion: ATSSuggestion,
        val userInput: String? = null // For suggestions requiring user input (e.g., keyword value)
    )

    override suspend operator fun invoke(params: Params): Result<Resume> {
        return try {
            val resume = resumeRepository.getById(params.resumeId)
                ?: return Failure("Resume not found: ${params.resumeId}")

            val suggestion = params.suggestion
            var updated = resume

            when (suggestion.type) {
                SuggestionType.ADD_KEYWORD -> {
                    // Add keyword to skills section
                    val skillsSection = updated.sections.find { it.type == SectionType.SKILLS }
                        ?: return Failure("Skills section not found")

                    val items = parseSectionItems(skillsSection.itemsJson)

                    val newItem = SectionItem(
                        title = suggestion.example ?: suggestion.message,
                        keywords = listOf(suggestion.example ?: suggestion.message)
                    )

                    val updatedItems = items + newItem
                    updated = updated.copy(
                        sections = updated.sections.map { section ->
                            if (section.id == skillsSection.id) {
                                section.copy(itemsJson = serializeSectionItems(updatedItems))
                            } else section
                        }
                    )
                }

                SuggestionType.REPHRASE -> {
                    // Rephrase section content - would integrate with AI rephrase
                }

                SuggestionType.ADD_METRIC -> {
                    // Add metrics to experience section
                }

                SuggestionType.FORMAT_FIX -> {
                    // Apply formatting fix
                }

                SuggestionType.REORDER -> {
                    // Reorder sections
                }
            }

            val saved = resumeRepository.update(updated)
            Success(saved)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun parseSectionItems(json: String): List<SectionItem> {
        if (json == "[]" || json.isBlank()) return emptyList()
        return emptyList() // Placeholder - use kotlinx.serialization in real impl
    }

    private fun serializeSectionItems(items: List<SectionItem>): String {
        // Placeholder - use kotlinx.serialization in real impl
        return "[]"
    }
}

/**
 * Refresh ATS Score UseCase
 * Forces re-analysis of ATS score
 */
class RefreshATSUseCase constructor(
    private val analyzeATSUseCase: AnalyzeATSUseCase
) : AsyncUseCase<IdParams, ATSScore> {

    override suspend operator fun invoke(params: IdParams): Result<ATSScore> {
        return analyzeATSUseCase(AnalyzeATSUseCase.Params(
            resumeId = params.id,
            forceRefresh = true
        ))
    }
}