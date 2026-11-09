package com.resumeapp.ai.di

import com.resumeapp.ai.guardrail.GuardrailPipeline
import com.resumeapp.ai.guardrail.GuardrailPipelineImpl
import com.resumeapp.ai.guardrail.InputSanitizer
import com.resumeapp.ai.guardrail.OutputValidator

/**
 * AI Module - Factory for AI dependencies (core:ai)
 * Provides only pure Kotlin dependencies: guardrails
 * AICache is provided by core:data module (RoomAICache)
 * AIGatewayClient, TraceLogger, TraceUploader are provided by app module
 * This is a pure JVM module with manual factory pattern (no annotation processing)
 */
class AiModule {

    fun provideInputSanitizer(): InputSanitizer = InputSanitizer()

    fun provideOutputValidator(): OutputValidator = OutputValidator()

    fun provideGuardrailPipeline(
        inputSanitizer: InputSanitizer,
        outputValidator: OutputValidator
    ): GuardrailPipeline = GuardrailPipelineImpl(inputSanitizer, outputValidator)
}