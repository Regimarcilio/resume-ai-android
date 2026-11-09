package com.resumeapp.ai.guardrail

import com.resumeapp.ai.gateway.AIResponse
import jakarta.inject.Inject

/**
 * Guardrail Pipeline - Input sanitization + Output validation
 * EngAI Pattern: Sanitize PII from prompts, validate outputs for safety
 */
interface GuardrailPipeline {
    fun sanitizeInput(prompt: String): String
    fun validateOutput(response: AIResponse): AIResponse
}

/**
 * Input Sanitizer - Removes PII from prompts before sending to gateway
 */
class InputSanitizer {
    
    // Regex patterns for PII detection
    private val cpfPattern = """\b\d{3}\.?\d{3}\.?\d{3}-?\d{2}\b""".toRegex()
    private val emailPattern = """\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b""".toRegex()
    private val phonePattern = """\b(?:\+55\s?)?(?:\(?\d{2}\)?\s?)?(?:9\s?)?\d{4}-?\d{4}\b""".toRegex()
    private val namePattern = """\b([A-Z][a-z]+(?:\s+[A-Z][a-z]+){1,3})\b""".toRegex()
    private val creditCardPattern = """\b\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4}\b""".toRegex()
    private val cnpjPattern = """\b\d{2}\.?\d{3}\.?\d{3}/?\d{4}-?\d{2}\b""".toRegex()

    fun sanitizeInput(prompt: String): String {
        var sanitized = prompt
        
        // Replace CPF
        sanitized = cpfPattern.replace(sanitized) { "***CPF***" }
        
        // Replace Email
        sanitized = emailPattern.replace(sanitized) { "***EMAIL***" }
        
        // Replace Phone
        sanitized = phonePattern.replace(sanitized) { "***PHONE***" }
        
        // Replace Credit Card
        sanitized = creditCardPattern.replace(sanitized) { "***CREDIT_CARD***" }
        
        // Replace CNPJ
        sanitized = cnpjPattern.replace(sanitized) { "***CNPJ***" }
        
        // Replace Names (heuristic - replace capitalized sequences)
        // Be careful not to replace common words
        sanitized = namePattern.replace(sanitized) { match ->
            val name = match.groupValues[1]
            // Skip common words that might match
            if (name.lowercase() in setOf("the", "and", "or", "but", "for", "with", "from", "to", "in", "on", "at", "by", "a", "i", "me", "my", "we", "our", "you", "your", "he", "his", "she", "her", "it", "its", "they", "them", "their")) {
                match.input.substring(match.range)
            } else {
                "***NAME***"
            }
        }
        
        return sanitized
    }
}

/**
 * Output Validator - Validates AI responses for safety
 */
class OutputValidator {
    
    // Patterns for detecting refusals and unsafe content
    private val refusalPatterns = listOf(
        """(?i)I cannot""",
        """(?i)I'm unable""",
        """(?i)I am unable""",
        """(?i)I can't""",
        """(?i)I won't""",
        """(?i)I will not""",
        """(?i)As an AI""",
        """(?i)As a language model""",
        """(?i)I'm not allowed""",
        """(?i)I am not allowed""",
        """(?i)This violates""",
        """(?i)This goes against""",
        """(?i)I must decline""",
        """(?i)I cannot comply""",
    ).map { it.toRegex() }
    
    private val injectionPatterns = listOf(
        """(?i)ignore previous instructions""",
        """(?i)forget everything""",
        """(?i)system prompt""",
        """(?i)you are now""",
        """(?i)pretend to be""",
        """(?i)act as""",
        """(?i)roleplay""",
        """(?i)simulate""",
    ).map { it.toRegex() }

    fun validateOutput(response: AIResponse): AIResponse {
        val content = response.content
        
        // Check for refusal
        val isRefusal = refusalPatterns.any { it.containsMatchIn(content) }
        if (isRefusal) {
            throw GuardrailException("Output validation failed: AI refusal detected", GuardrailException.Type.REFUSAL)
        }
        
        // Check for prompt injection in output
        val hasInjection = injectionPatterns.any { it.containsMatchIn(content) }
        if (hasInjection) {
            throw GuardrailException("Output validation failed: Potential prompt injection detected", GuardrailException.Type.INJECTION)
        }
        
        // Check for PII leakage (should not happen if input sanitized, but double-check)
        val piiPatterns = listOf(
            """\b\d{3}\.?\d{3}\.?\d{3}-?\d{2}\b""".toRegex(), // CPF
            """\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b""".toRegex(), // Email
            """\b(?:\+55\s?)?(?:\(?\d{2}\)?\s?)?(?:9\s?)?\d{4}-?\d{4}\b""".toRegex(), // Phone
        )
        
        val hasPiiLeak = piiPatterns.any { it.containsMatchIn(content) }
        if (hasPiiLeak) {
            throw GuardrailException("Output validation failed: PII leakage detected", GuardrailException.Type.PII_LEAK)
        }
        
        return response
    }
}

/**
 * Guardrail Pipeline Implementation - Combines sanitizer and validator
 */
class GuardrailPipelineImpl @Inject constructor(
    private val inputSanitizer: InputSanitizer,
    private val outputValidator: OutputValidator
) : GuardrailPipeline {
    
    override fun sanitizeInput(prompt: String): String {
        return inputSanitizer.sanitizeInput(prompt)
    }
    
    override fun validateOutput(response: AIResponse): AIResponse {
        return outputValidator.validateOutput(response)
    }
}

class GuardrailException(
    message: String,
    val type: Type
) : Exception(message) {
    enum class Type {
        REFUSAL,
        INJECTION,
        PII_LEAK,
        UNKNOWN
    }
}