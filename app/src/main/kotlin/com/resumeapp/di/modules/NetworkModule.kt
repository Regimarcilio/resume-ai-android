package com.resumeapp.di.modules

import android.content.Context
import com.resumeapp.ai.gateway.AIGatewayClient
import com.resumeapp.ai.gateway.AIGatewayClientImpl
import com.resumeapp.ai.guardrail.GuardrailPipeline
import com.resumeapp.ai.guardrail.InputSanitizer
import com.resumeapp.ai.guardrail.OutputValidator
import com.resumeapp.ai.guardrail.GuardrailPipelineImpl
import com.resumeapp.ai.trace.TraceLogger
import com.resumeapp.ai.trace.TraceLoggerImpl
import com.resumeapp.ai.trace.TraceUploader
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.KotlinJsonAdapterFactory
import dagger.hilt.android.InstallIn
import dagger.hilt.android.components.SingletonComponent
import dagger.hilt.android.scopes.ApplicationContext
import dagger.hilt.android.scopes.Singleton
import dagger.Module
import dagger.Provides
import java.util.concurrent.TimeUnit

/**
 * Network Module - HTTP client, Retrofit, AI Gateway client
 */
@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/v1/") // Default, overridden by build config
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideInputSanitizer(): InputSanitizer = InputSanitizer()

    @Provides
    @Singleton
    fun provideOutputValidator(): OutputValidator = OutputValidator()

    @Provides
    @Singleton
    fun provideGuardrailPipeline(): GuardrailPipeline = GuardrailPipelineImpl(
        provideInputSanitizer(),
        provideOutputValidator()
    )

    @Provides
    @Singleton
    fun provideAIGatewayClient(
        okHttpClient: OkHttpClient,
        guardrailPipeline: GuardrailPipeline,
        aiCache: com.resumeapp.ai.cache.AICache,
        traceLogger: TraceLogger,
        traceUploader: TraceUploader
    ): AIGatewayClient = AIGatewayClientImpl(
        okHttpClient = okHttpClient,
        guardrailPipeline = guardrailPipeline,
        aiCache = aiCache,
        traceLogger = traceLogger,
        traceUploader = traceUploader
    )

    @Provides
    @Singleton
    fun provideTraceLogger(@ApplicationContext context: Context): TraceLogger = TraceLoggerImpl(context)

    @Provides
    @Singleton
    fun provideTraceUploader(@ApplicationContext context: Context, traceLogger: TraceLogger): TraceUploader = TraceUploader(traceLogger, context)
}