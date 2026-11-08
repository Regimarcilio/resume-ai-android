package com.resumeapp.domain.usecase.aiassist

import com.resumeapp.domain.model.Result
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.model.SectionItem
import com.resumeapp.domain.repository.ResumeRepository
import com.resumeapp.ai.gateway.AIGatewayClient
import com.resumeapp.ai.cache.AICache
import com.resumeapp.ai.guardrail.GuardrailPipeline
import com.resumeapp.ai.trace.TraceLogger
import com.resumeapp.domain.usecase.AsyncUseCase
import com.resumeapp.domain.usecase.IdParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Improve Bullets UseCase
 * Enhances bullet points in experience/project sections using AI
 */
class ImproveBulletsUseCase @Inject constructor(
    private val aiGatewayClient: com.resumeapp.ai.gateway.AIGatewayClient,
    private val aiCache: com.resumeapp.ai.cache.AICache,
    private val guardrailPipeline: com.resumeapp.ai.guardrail.GuardrailPipeline,
    private val traceLogger: com.resumeapp.ai.trace.TraceLogger
) : AsyncUseCase<ImproveBulletsUseCase.Params, List<String>> {

    @Serializable
    data class Params(
        val sectionType: com.resumeapp.domain.model.SectionType,
        val currentItems: List<com.resumeapp.domain.model.SectionItem>,
        val context: ResumeContext,
        val maxBullets: Int = 5
    )

    @Serializable
    data class ResumeContext(
        val targetRole: String? = null,
        val industry: String? = null,
        val experienceLevel: String? = null // Junior, Mid, Senior, Lead
    )

    override fun invoke(params: Params): Result<List<String>> {
        return try {
            // Build prompt for bullet improvement
            val prompt = buildImproveBulletsPrompt(params)
            
            val request = com.resumeapp.ai.gateway.AIRequest(
                prompt = prompt,
                model = "gpt-4o-mini",
                maxTokens = 800,
                temperature = 0.7f,
                metadata = mapOf(
                    "prompt_type" to "improve_bullets",
                    "section_type" to params.sectionType.name
                )
            )

            val result = com.resumeapp.ai.gateway.AIGatewayClient.complete(request)
            
            return when (result) {
                is Result.Success -> {
                    val bullets = parseBullets(result.value.content, params.maxBullets)
                    Result.Success(bullets)
                }
                is Result.Failure -> {
                    Result.Failure(result.error)
                }
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    private fun buildImproveBulletsPrompt(params: Params): String {
        val itemsText = params.currentItems.mapIndexed { index, item ->
            "${index + 1}. ${item.title} - ${item.subtitle} (${item.startDate} - ${item.endDate})\n" +
            item.highlights.joinToString("\n") { "  \u2022 $it" }
        }.joinToString("\n\n")

        val contextInfo = buildContextInfo(params.context)

        return """
            Melhore os seguintes bullets para ${params.sectionType.name.replace("_", " ").lowercase()}:
            
            ITENS ATUAIS:
            $itemsText
            
            CONTEXTO:
            $contextInfo
            
            INSTRUÇÕES:
            1. Reescreva cada bullet começando com verbo de ação forte
            2. Adicione métricas quantificáveis onde possível (%, R\$, tempo, quantidade)
            3. Use palavras-chave ATS relevantes para a área
            4. Máximo ${params.maxBullets} bullets por item
            5. Seja conciso e impactante
            6. Formato: apenas a lista de bullets melhorados, um por linha, sem numeração
            
            EXEMPLO DE SAÍDA:
            \u2022 Liderou migração de monolito para microsserviços (Kotlin/Spring), reduzindo deploy de 45min para 12min (73% mais rápido)
            \u2022 Arquitetou observabilidade com Grafana/Loki/Tempo, diminuindo MTTR de 2h para 15min
        """
    }

    private fun buildContextInfo(context: com.resumeapp.domain.usecase.aiassist.ImproveBulletsUseCase.ResumeContext): String {
        val parts = mutableListOf<String>()
        context.targetRole?.let { parts.add("Cargo alvo: $it") }
        context.industry?.let { parts.add("Indústria: $it") }
        context.experienceLevel?.let { parts.add("Nível: $it") }
        return if (parts.isEmpty()) "Não especificado" else parts.joinToString("\n")
    }

    private fun parseBullets(content: String, maxBullets: Int): List<String> {
        return content.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.replace("^[\u2022\-\*\d\.\s]+".toRegex(), "").trim() }
            .filter { it.isNotBlank() }
            .take(maxBullets)
    }
}

/**
 * Extract Keywords UseCase
 * Extracts relevant keywords from job description for ATS optimization
 */
class ExtractKeywordsUseCase @Inject constructor(
    private val aiGatewayClient: com.resumeapp.ai.gateway.AIGatewayClient,
    private val aiCache: com.resumeapp.ai.cache.AICache,
    private val traceLogger: com.resumeapp.ai.trace.TraceLogger
) : AsyncUseCase<ExtractKeywordsUseCase.Params, List<String>> {

    @Serializable
    data class Params(
        val jobDescription: String,
        val maxKeywords: Int = 20
    )

    override fun invoke(params: Params): Result<List<String>> {
        return try {
            val prompt = """
                Extraia as ${params.maxKeywords} palavras-chave mais relevantes desta descrição de vaga para otimização ATS:
                
                DESCRIÇÃO DA VAGA:
                ${params.jobDescription}
                
                Retorne APENAS uma lista JSON de strings:
                ["keyword1", "keyword2", "keyword3", ...]
                
                Foque em:
                - Tecnologias (linguagens, frameworks, ferramentas)
                - Metodologias (Agile, Scrum, CI/CD)
                - Soft skills mencionadas
                - Certificações exigidas
                - Anos de experiência
            """
            
            val request = com.resumeapp.ai.gateway.AIRequest(
                prompt = prompt.trimIndent(),
                model = "gpt-4o-mini",
                maxTokens = 500,
                temperature = 0.3f,
                metadata = mapOf("prompt_type" to "extract_keywords")
            )

            val result = com.resumeapp.ai.gateway.AIGatewayClient.complete(request)
            
            return when (result) {
                is Result.Success -> {
                    val keywords = parseKeywords(result.value.content)
                    Result.Success(keywords)
                }
                is Result.Failure -> {
                    Result.Failure(result.error)
                }
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    private fun parseKeywords(content: String): List<String> {
        try {
            val json = com.google.gson.JsonParser.parseString(content.trim())
            if (json.isJsonArray) {
                return json.asJsonArray.map { it.asString }.toList()
            }
        } catch (e: Exception) {
            // Fallback: parse line by line
        }
        
        // Fallback: split by lines/comma
        return content.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.replace("^[\"\-\*\d\.\s]+".toRegex(), "").replace("[\s]+$".toRegex(), "") }
            .filter { it.isNotBlank() }
            .distinct()
            .take(20)
    }
}

/**
 * Rephrase Section UseCase
 * Rewrites a section with different tone/style
 */
class RephraseSectionUseCase @Inject constructor(
    private val aiGatewayClient: com.resumeapp.ai.gateway.AIGatewayClient,
    private val traceLogger: com.resumeapp.ai.trace.TraceLogger
) : AsyncUseCase<RephraseSectionUseCase.Params, String> {

    @Serializable
    data class Params(
        val sectionTitle: String,
        val currentContent: String,
        val tone: Tone = Tone.PROFESSIONAL,
        val targetLength: Length = Length.MEDIUM
    )

    enum class Tone {
        PROFESSIONAL, CONCISE, IMPACTFUL, TECHNICAL, LEADERSHIP
    }

    enum class Length {
        SHORT, MEDIUM, LONG
    }

    override fun invoke(params: Params): Result<String> {
        return try {
            val toneInstructions = when (params.tone) {
                Tone.PROFESSIONAL -> "Tom profissional e corporativo"
                Tone.CONCISE -> "Ultra conciso, direto ao ponto"
                Tone.IMPACTFUL -> "Focado em resultados e impacto"
                Tone.TECHNICAL -> "Tom técnico, com terminologia específica"
                Tone.LEADERSHIP -> "Foco em liderança, estratégia e resultados de equipe"
            }

            val lengthInstructions = when (params.targetLength) {
                Length.SHORT -> "Máximo 2 frases"
                Length.MEDIUM -> "3-5 frases"
                Length.LONG -> "Parágrafo completo, detalhado"
            }

            val prompt = """
                Reescreva o seguinte texto para um currículo:
                
                TEXTO ORIGINAL:
                ${params.sectionTitle}
                ${params.currentContent}
                
                INSTRUÇÕES:
                - ${when (params.tone) { Tone.PROFESSIONAL -> "Tom profissional e corporativo" Tone.CONCISE -> "Ultra conciso, direto ao ponto" Tone.IMPACTFUL -> "Focado em resultados e impacto" Tone.TECHNICAL -> "Tom técnico, com terminologia específica" Tone.LEADERSHIP -> "Foco em liderança, estratégia e resultados de equipe" }}
                - ${when (params.targetLength) { Length.SHORT -> "Máximo 2 frases" Length.MEDIUM -> "3-5 frases" Length.LONG -> "Parágrafo completo, detalhado" }}
                - Use verbos de ação forte
                - Inclua métricas se aplicável
                - Otimizado para ATS
                - Retorne APENAS o texto reescrito, sem explicações
            """
            
            val request = com.resumeapp.ai.gateway.AIRequest(
                prompt = prompt.trimIndent(),
                model = "gpt-4o-mini",
                maxTokens = 500,
                temperature = 0.5f,
                metadata = mapOf("prompt_type" to "rephrase")
            )

            val result = com.resumeapp.ai.gateway.AIGatewayClient.complete(request)
            
            return when (result) {
                is Result.Success -> Result.Success(result.value.content.trim())
                is Result.Failure -> Result.Failure(result.error)
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}

/**
 * Suggest Templates UseCase
 * Recommends templates based on resume content and target role
 */
class SuggestTemplatesUseCase @Inject constructor(
    private val aiGatewayClient: com.resumeapp.ai.gateway.AIGatewayClient,
    private val templateRepository: com.resumeapp.domain.repository.TemplateRepository
) : AsyncUseCase<SuggestTemplatesUseCase.Params, List<com.resumeapp.domain.model.Template>> {

    @Serializable
    data class Params(
        val resume: com.resumeapp.domain.model.Resume,
        val targetRole: String? = null,
        val maxSuggestions: Int = 3
    )

    override fun invoke(params: Params): Result<List<com.resumeapp.domain.model.Template>> {
        return try {
            val allTemplates = com.resumeapp.domain.repository.TemplateRepository.getAllActive()
            
            // Simple scoring based on category and premium status
            val scored = allTemplates.map { template ->
                var score = 0
                
                // Category matching
                when (params.resume.sections.firstOrNull()?.type) {
                    com.resumeapp.domain.model.SectionType.EXPERIENCE -> {
                        if (template.category == com.resumeapp.domain.model.TemplateCategory.ATS_OPTIMIZED) score += 10
                        if (template.category == com.resumeapp.domain.model.TemplateCategory.MODERN) score += 5
                    }
                    com.resumeapp.domain.model.SectionType.PROJECTS -> {
                        if (template.category == com.resumeapp.domain.model.TemplateCategory.CREATIVE) score += 10
                    }
                }
                
                // Premium bonus for Pro users (would check subscription)
                if (!it.isPremium) score += 5
                
                template to score
            }.sortedByDescending { it.second }.take(3).map { it.first }
            
            Result.Success(scored)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}