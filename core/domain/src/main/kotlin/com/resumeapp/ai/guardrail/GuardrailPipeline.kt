package com.resumeapp.ai.guardrail

/**
 * Guardrail Pipeline Interface - Input sanitization and output validation
 * Implemented in core:ai module
 */
interface GuardrailPipeline {

    /**
     * Sanitize user input to prevent prompt injection
     */
    fun sanitizeInput(prompt: String): String

    /**
     * Validate AI output for safety/compliance
     */
    fun validateOutput(response: com.resumeapp.ai.gateway.AIResponse): com.resumeapp.ai.gateway.AIResponse

    /**
     * Check if prompt contains PII
     */
    fun containsPii(prompt: String): Boolean
}