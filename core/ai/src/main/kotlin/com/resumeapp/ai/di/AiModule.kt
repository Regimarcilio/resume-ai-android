package com.resumeapp.ai.di

import com.resumeapp.ai.guardrail.GuardrailPipeline
import com.resumeapp.ai.guardrail.GuardrailPipelineImpl
import com.resumeapp.ai.guardrail.InputSanitizer
import com.resumeapp.ai.guardrail.OutputValidator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

/**
 * AI Module - Hilt module for AI dependencies (core:ai)
 * Provides only pure Kotlin dependencies: guardrails
 * AICache is provided by core:data module (RoomAICache)
 * AIGatewayClient, TraceLogger, TraceUploader are provided by app module
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Provides
    @Singleton
    fun provideInputSanitizer(): InputSanitizer = InputSanitizer()

    @Provides
    @Singleton
    fun provideOutputValidator(): OutputValidator = OutputValidator()

    @Provides
    @Singleton
    fun provideGuardrailPipeline(
        inputSanitizer: InputSanitizer,
        outputValidator: OutputValidator
    ): GuardrailPipeline = GuardrailPipelineImpl(inputSanitizer, outputValidator)
}