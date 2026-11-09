package com.resumeapp.ai.di

import com.resumeapp.ai.cache.AICache
import com.resumeapp.ai.gateway.AIGatewayClient
import com.resumeapp.ai.guardrail.GuardrailPipeline
import com.resumeapp.ai.guardrail.GuardrailPipelineImpl
import com.resumeapp.ai.guardrail.InputSanitizer
import com.resumeapp.ai.guardrail.OutputValidator
import com.resumeapp.ai.trace.TraceLogger
import com.resumeapp.ai.trace.TraceLoggerImpl
import com.resumeapp.ai.trace.TraceUploader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

/**
 * AI Module - Hilt module for AI dependencies
 * Provides all AI-related dependencies: gateways, guardrails, tracing
 * AICache is provided by core:data module (RoomAICache)
 * AIGatewayClient is provided by app module
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

    @Provides
    @Singleton
    fun provideTraceLogger(): TraceLogger = TraceLoggerImpl()

    @Provides
    @Singleton
    fun provideTraceUploader(traceLogger: TraceLogger): TraceUploader = TraceUploader(traceLogger)
}