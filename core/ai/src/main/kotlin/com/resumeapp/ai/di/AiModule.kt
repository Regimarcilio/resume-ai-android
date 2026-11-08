package com.resumeapp.ai.di

import com.resumeapp.ai.cache.AICache
import com.resumeapp.ai.cache.RoomAICache
import com.resumeapp.ai.gateway.AIGatewayClient
import com.resumeapp.ai.gateway.AIGatewayClientImpl
import com.resumeapp.ai.guardrail.GuardrailPipeline
import com.resumeapp.ai.guardrail.InputSanitizer
import com.resumeapp.ai.guardrail.OutputValidator
import com.resumeapp.ai.trace.TraceLogger
import com.resumeapp.ai.trace.TraceLoggerImpl
import com.resumeapp.ai.trace.TraceUploader
import dagger.Module
import dagger.Provides
import dagger.hilt.android.InstallIn
import dagger.hilt.android.components.SingletonComponent
import dagger.hilt.android.scopes.Singleton
import javax.inject.Inject

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

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
    ): GuardrailPipeline = GuardrailPipeline(inputSanitizer, outputValidator)

    @Provides
    @Singleton
    fun provideAICache(roomAICache: RoomAICache): AICache = roomAICache

    @Provides
    @Singleton
    fun provideTraceLogger(): TraceLogger = TraceLoggerImpl()

    @Provides
    @Singleton
    fun provideTraceUploader(traceLogger: TraceLogger): TraceUploader = TraceUploader(traceLogger)

    @Provides
    @Singleton
    fun provideAIGatewayClient(
        guardrailPipeline: GuardrailPipeline,
        aiCache: AICache,
        traceLogger: TraceLogger,
        traceUploader: TraceUploader
    ): AIGatewayClient = AIGatewayClientImpl(
        guardrailPipeline = guardrailPipeline,
        aiCache = aiCache,
        traceLogger = traceLogger,
        traceUploader = traceUploader
    )
}