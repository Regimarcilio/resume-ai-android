package com.resumeapp.domain.usecase.ats

import com.resumeapp.domain.model.Result
import com.resumeapp.domain.model.ATSScore
import com.resumeapp.domain.model.ATSScoreRepository
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.repository.ATSScoreRepository as ATSScoreRepositoryInterface
import com.resumeapp.domain.repository.ResumeRepository
import com.resumeapp.domain.usecase.AsyncUseCase
import com.resumeapp.domain.usecase.IdParams
import com.resumeapp.domain.usecase.SearchParams
import com.resumeapp.ai.gateway.AIGatewayClient
import com.resumeapp.ai.cache.AICache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Analyze ATS UseCase
 * Analyzes a resume against a job description (optional) and returns ATS score
 */
class AnalyzeATSUseCase @Inject constructor(
    private val atsScoreRepository: ATSScoreRepositoryInterface,
    private val resumeRepository: ResumeRepository,
    private val aiGatewayClient: com.resumeapp.ai.gateway.AIGatewayClient,
    private val aiCache: com.resumeapp.ai.cache.AICache
) : AsyncUseCase<AnalyzeATSUseCase.Params, com.resumeapp.domain.model.ATSScore> {

    @Serializable
    data class Params(
        val resumeId: String,
        val jobDescription: String? = null,
        val forceRefresh: Boolean = false
    )

    override fun invoke(params: Params): Result<com.resumeapp.domain.model.ATSScore> {
        return try {
            // Check if we have a cached score and not forcing refresh
            if (!params.forceRefresh) {
                val cached = atsScoreRepository.getByResumeId(params.resumeId)
                if (cached != null) {
                    return Result.Success(cached)
                }
            }

            val resume = com.resumeapp.domain.repository.ResumeRepository.getById(params.resumeId)
                ?: return Result.Failure(Exception("Resume not found: ${params.resumeId}"))

            // 1. Local analysis (fast, no AI needed)
            val localScores = analyzeLocally(params.resume)

            // 2. AI-powered keyword extraction and matching
            val aiAnalysis = if (params.jobDescription != null && params.jobDescription.isNotBlank()) {
                analyzeWithAI(resume, params.jobDescription!!)
            } else {
                analyzeWithAI(resume, "")
            }

            // 3. Combine scores
            val overallScore = calculateOverallScore(localScores, aiAnalysis)
            val suggestions = generateSuggestions(resume, aiAnalysis, localScores)

            val score = com.resumeapp.domain.model.ATSScore(
                resumeId = params.resumeId,
                overallScore = overallScore,
                keywordMatch = aiAnalysis.keywordMatch,
                formattingScore = localScores.formatting,
                sectionCompleteness = localScores.completeness,
                missingKeywords = aiAnalysis.missingKeywords,
                suggestions = suggestions,
                analyzedAt = System.currentTimeMillis()
            )

            // Save to repository
            val saved = atsScoreRepository.save(score)

            Result.Success(saved)
        } catch (e: Exception) {
            Result.failure(e.message ?: e.javaClass.simpleName)
        }
    }

    private data class LocalScores(
        val formatting: Int,
        val completeness: Int
    )

    private fun analyzeLocally(resume: com.resumeapp.domain.model.Resume): LocalScores {
        // Formatting score based on structure
        var formattingScore = 100
        
        // Check for required sections
        val requiredSections = setOf(
            com.resumeapp.domain.model.SectionType.EXPERIENCE,
            com.resumeapp.domain.model.SectionType.EDUCATION,
            com.resumeapp.domain.model.SectionType.SKILLS
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
        
        val sectionCompleteness = resume.sections.filter { it.itemsJson != "[]" }.size * 100 / max(1, resume.sections.size)
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

    private fun analyzeWithAI(resume: com.resumeapp.domain.model.Resume, jobDescription: String): com.resumeapp.domain.usecase.ats.AnalyzeATSUseCase.AIAnalysis {
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

        val request = com.resumeapp.ai.gateway.AIRequest(
            prompt = prompt,
            model = "gpt-4o-mini",
            maxTokens = 500,
            temperature = 0.3f,
            metadata = mapOf("prompt_type" to "ats_keywords")
        )

        val result = com.resumeapp.ai.gateway.AIGatewayClient.complete(request)
        
        return when (result) {
            is Result.Success -> parseAIResponse(result.value.content)
            is Result.Failure -> {
                // Fallback: basic keyword extraction
                extractKeywordsFallback(resume)
            }
        }
    }

    private fun parseAIResponse(content: String): com.resumeapp.domain.usecase.ats.AnalyzeATSUseCase.AIAnalysis {
        // Parse JSON response from AI
        try {
            val json = com.google.gson.JsonParser.parseString(content).asJsonObject
            val keywordMatch = json.get("keywordMatch")?.asInt ?: 50
            val missingKeywords = json.get("missingKeywords")?.asJsonArray
                ?.map { it.asString }
                ?.toList() ?: emptyList()
            
            return com.resumeapp.domain.usecase.ats.AnalyzeATSUseCase.AIAnalysis(
                keywordMatch = keywordMatch.coerceIn(0, 100),
                missingKeywords = missingKeywords
            )
        } catch (e: Exception) {
            return com.resumeapp.domain.usecase.ats.AnalyzeATSUseCase.AIAnalysis(
                keywordMatch = 50,
                missingKeywords = emptyList()
            )
        }
    }

    private fun extractKeywordsFallback(resume: com.resumeapp.domain.model.Resume): com.resumeapp.domain.usecase.ats.AnalyzeATSUseCase.AIAnalysis {
        // Basic keyword extraction from resume content
        val allText = buildResumeText(resume)
        val commonKeywords = listOf(
            "Kotlin", "Java", "Spring", "React", "Android", "iOS", "AWS", "Docker",
            "Kubernetes", "SQL", "NoSQL", "Microservices", "REST", "GraphQL",
            "Agile", "Scrum", "CI/CD", "Git", "Testing", "Clean Code"
        )
        
        val found = commonKeywords.filter { it.lowercase() in resume.toString().lowercase() }
        val missing = commonKeywords - found
        
        return com.resumeapp.domain.usecase.ats.AnalyzeATSUseCase.AIAnalysis(
            keywordMatch = (found.size * 100 / commonKeywords.size).coerceAtMost(100),
            missingKeywords = missing.take(10)
        )
    }

    private fun buildResumeText(resume: com.resumeapp.domain.model.Resume): String {
        val sb = StringBuilder()
        sb.append("Nome: ${resume.personalInfo.fullName}\n")
        sb.append("Email: ${resume.personalInfo.email}\n")
        sb.append("Resumo: ${resume.personalInfo.summary}\n\n")
        
        resume.sections.forEach { section ->
            sb.append("${section.title.uppercase()}:\n")
            val items = com.google.gson.Gson().fromJson(
                section.itemsJson, 
                com.google.gson.reflect.TypeToken.getParameterized(List::class.java, com.resumeapp.domain.model.SectionItem::class.java).type
            ) as List<com.resumeapp.domain.model.SectionItem>
            
            items.forEach { item ->
                sb.append("- ${item.title} ${item.subtitle} (${item.startDate} - ${item.endDate})\n")
                item.highlights.forEach { sb.append("  * $it\n") }
            }
            sb.append("\n")
        }
        
        return sb.toString()
    }

    private fun calculateOverallScore(local: com.resumeapp.domain.usecase.ats.AnalyzeATSUseCase.LocalScores, ai: com.resumeapp.domain.usecase.ats.AnalyzeATSUseCase.AIAnalysis): Int {
        // Weighted average: 30% formatting, 30% completeness, 40% keyword match
        return ((local.formatting * 0.3) + (local.completeness * 0.3) + (ai.keywordMatch * 0.4)).toInt().coerceIn(0, 100)
    }

    private fun generateSuggestions(
        resume: com.resumeapp.domain.model.Resume,
        ai: com.resumeapp.domain.usecase.ats.AnalyzeATSUseCase.AIAnalysis,
        local: com.resumeapp.domain.usecase.ats.AnalyzeATSUseCase.LocalScores
    ): List<com.resumeapp.domain.model.ATSSuggestion> {
        val suggestions = mutableListOf<com.resumeapp.domain.model.ATSSuggestion>()
        var priority = 1

        // Missing keywords suggestions
        ai.missingKeywords.take(5).forEach { keyword ->
            suggestions.add(com.resumeapp.domain.model.ATSSuggestion(
                sectionId = "skills",
                type = com.resumeapp.domain.model.SuggestionType.ADD_KEYWORD,
                priority = priority++,
                message = "Adicione a keyword '$keyword' na seção de Habilidades",
                example = "$keyword"
            ))
        }

        // Formatting suggestions
        if (local.formatting < 80) {
            suggestions.add(com.resumeapp.domain.model.ATSSuggestion(
                sectionId = "general",
                type = com.resumeapp.domain.model.SuggestionType.FORMAT_FIX,
                priority = priority++,
                message = "Melhore a formatação: use bullets consistentes, evite tabelas e colunas",
                example = "• Bullet points\n• Formato consistente\n• Fontes padrão"
            ))
        }

        // Completeness suggestions
        if (local.completeness < 80) {
            suggestions.add(com.resumeapp.domain.model.ATSSuggestion(
                sectionId = "summary",
                type = com.resumeapp.domain.model.SuggestionType.ADD_METRIC,
                priority = priority++,
                message = "Complete seu resumo profissional com 3-4 linhas destacando conquistas",
                example = "Desenvolvedor Full Stack com 5+ anos... Reduziu tempo de deploy em 40%..."
            ))
        }

        // Missing required sections
        val requiredSections = setOf(
            com.resumeapp.domain.model.SectionType.EXPERIENCE,
            com.resumeapp.domain.model.SectionType.EDUCATION,
            com.resumeapp.domain.model.SectionType.SKILLS
        )
        val hasSections = com.resumeapp.domain.model.Resume.sections.map { it.type }.toSet()
        (requiredSections - hasSections).forEach { missing ->
            suggestions.add(com.resumeapp.domain.model.ATSSuggestion(
                sectionId = missing.name.lowercase(),
                type = com.resumeapp.domain.model.SuggestionType.FORMAT_FIX,
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
class GetATSScoreUseCase @Inject constructor(
    private val atsScoreRepository: com.resumeapp.domain.model.ATSScoreRepository
) : AsyncUseCase<com.resumeapp.domain.usecase.IdParams, com.resumeapp.domain.model.ATSScore> {

    override fun invoke(params: com.resumeapp.domain.usecase.IdParams): Result<com.resumeapp.domain.model.ATSScore> {
        return try {
            val score = com.resumeapp.domain.model.ATSScoreRepository.getByResumeId(params.id)
                ?: return Result.Failure(Exception("ATS score not found for resume: ${params.id}"))
            Result.Success(score)
        } catch (e: Exception) {
            Result.failure(e.message ?: e.javaClass.simpleName)
        }
    }
}

/**
 * Apply ATS Suggestion UseCase
 * Applies a suggestion from ATS analysis to the resume
 */
class ApplyATSSuggestionUseCase @Inject constructor(
    private val resumeRepository: com.resumeapp.domain.repository.ResumeRepository
) : AsyncUseCase<ApplyATSSuggestionUseCase.Params, com.resumeapp.domain.model.Resume> {

    @Serializable
    data class Params(
        val resumeId: String,
        val suggestion: com.resumeapp.domain.model.ATSSuggestion,
        val userInput: String? = null // For suggestions requiring user input (e.g., keyword value)
    )

    override fun invoke(params: Params): Result<com.resumeapp.domain.model.Resume> {
        return try {
            val resume = com.resumeapp.domain.repository.ResumeRepository.getById(params.resumeId)
                ?: return Result.Failure(Exception("Resume not found: ${params.resumeId}"))

            val suggestion = params.suggestion
            var updated = resume

            when (suggestion.type) {
                com.resumeapp.domain.model.SuggestionType.ADD_KEYWORD -> {
                    // Add keyword to skills section
                    val skillsSection = updated.sections.find { it.type == com.resumeapp.domain.model.SectionType.SKILLS }
                        ?: return Result.Failure(Exception("Skills section not found"))
                    
                    val items = com.google.gson.Gson().fromJson(
                        skillsSection.itemsJson,
                        com.google.gson.reflect.TypeToken.getParameterized(List::class.java, com.resumeapp.domain.model.SectionItem::class.java).type
                    ) as List<com.resumeapp.domain.model.SectionItem>
                    
                    val newItem = com.resumeapp.domain.model.SectionItem(
                        title = suggestion.example ?: suggestion.message,
                        keywords = listOf(suggestion.example ?: suggestion.message)
                    )
                    
                    val updatedItems = items + newItem
                    updated = updated.copy(
                        sections = updated.sections.map { section ->
                            if (section.id == skillsSection.id) {
                                section.copy(itemsJson = com.google.gson.Gson().toJson(updatedItems))
                            } else section
                        }
                    )
                }
                
                com.resumeapp.domain.model.SuggestionType.REPHRASE -> {
                    // Rephrase section content - would integrate with AI rephrase
                }
                
                com.resumeapp.domain.model.SuggestionType.ADD_METRIC -> {
                    // Add metrics to experience section
                }
                
                com.resumeapp.domain.model.SuggestionType.FORMAT_FIX -> {
                    // Apply formatting fix
                }
                
                com.resumeapp.domain.model.SuggestionType.REORDER -> {
                    // Reorder sections
                }
            }

            val saved = com.resumeapp.domain.repository.ResumeRepository.update(updated)
            Result.Success(saved)
        } catch (e: Exception) {
            Result.failure(e.message ?: e.javaClass.simpleName)
        }
    }
}

/**
 * Refresh ATS Score UseCase
 * Forces re-analysis of ATS score
 */
class RefreshATSUseCase @Inject constructor(
    private val analyzeATSUseCase: AnalyzeATSUseCase
) : AsyncUseCase<com.resumeapp.domain.usecase.IdParams, com.resumeapp.domain.model.ATSScore> {

    override fun invoke(params: com.resumeapp.domain.usecase.IdParams): Result<com.resumeapp.domain.model.ATSScore> {
        return analyzeATSUseCase(AnalyzeATSUseCase.Params(
            resumeId = params.id,
            forceRefresh = true
        ))
    }
}