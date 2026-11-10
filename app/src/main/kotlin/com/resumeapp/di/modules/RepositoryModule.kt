package com.resumeapp.di.modules

import com.resumeapp.data.repository.ResumeRepositoryImpl
import com.resumeapp.data.repository.TemplateRepositoryImpl
import com.resumeapp.data.repository.ATSScoreRepositoryImpl
import com.resumeapp.data.repository.TraceRepositoryImpl
import com.resumeapp.data.repository.PdfRepositoryImpl
import com.resumeapp.domain.repository.ResumeRepository
import com.resumeapp.domain.repository.TemplateRepository
import com.resumeapp.domain.repository.ATSScoreRepository
import com.resumeapp.domain.repository.TraceRepository
import com.resumeapp.domain.repository.PdfRepository
import com.resumeapp.data.cache.RoomAICache
import dagger.hilt.android.InstallIn
import dagger.hilt.android.components.SingletonComponent
import dagger.hilt.android.scopes.Singleton
import dagger.Module
import dagger.Provides

/**
 * Repository Module - Repository implementations
 */
@InstallIn(SingletonComponent::class)
@Module
object RepositoryModule {

    @Provides
    @Singleton
    fun provideResumeRepository(impl: ResumeRepositoryImpl): ResumeRepository = impl

    @Provides
    @Singleton
    fun provideTemplateRepository(impl: TemplateRepositoryImpl): TemplateRepository = impl

    @Provides
    @Singleton
    fun provideATSScoreRepository(impl: ATSScoreRepositoryImpl): ATSScoreRepository = impl

    @Provides
    @Singleton
    fun provideTraceRepository(impl: TraceRepositoryImpl): TraceRepository = impl

    @Provides
    @Singleton
    fun providePdfRepository(impl: PdfRepositoryImpl): PdfRepository = impl

    @Provides
    @Singleton
    fun provideAICacheRepository(impl: RoomAICache): com.resumeapp.ai.cache.AICache = impl
}