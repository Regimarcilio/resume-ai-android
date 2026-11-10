package com.resumeapp.di.modules

import com.resumeapp.domain.repository.ResumeRepository
import com.resumeapp.domain.repository.TemplateRepository
import com.resumeapp.domain.repository.ATSScoreRepository
import com.resumeapp.domain.repository.ConsentRepository
import com.resumeapp.domain.usecase.ats.AnalyzeATSUseCase
import com.resumeapp.domain.usecase.ats.ApplyATSSuggestionUseCase
import com.resumeapp.domain.usecase.ats.GetATSScoreUseCase
import com.resumeapp.domain.usecase.ats.RefreshATSUseCase
import com.resumeapp.domain.usecase.aiassist.ExtractKeywordsUseCase
import com.resumeapp.domain.usecase.aiassist.ImproveBulletsUseCase
import com.resumeapp.domain.usecase.aiassist.RephraseSectionUseCase
import com.resumeapp.domain.usecase.aiassist.SuggestTemplatesUseCase
import com.resumeapp.domain.usecase.onboarding.RecordConsentUseCase
import com.resumeapp.domain.usecase.onboarding.CompleteOnboardingUseCase
import com.resumeapp.domain.usecase.resume.CreateResumeUseCase
import com.resumeapp.domain.usecase.resume.DeleteResumeUseCase
import com.resumeapp.domain.usecase.resume.DuplicateResumeUseCase
import com.resumeapp.domain.usecase.resume.GetAllResumesUseCase
import com.resumeapp.domain.usecase.resume.GetResumeUseCase
import com.resumeapp.domain.usecase.resume.UpdateResumeUseCase
import com.resumeapp.domain.usecase.resume.ReorderSectionsUseCase
import com.resumeapp.domain.usecase.resume.UpdateSectionUseCase
import com.resumeapp.domain.usecase.resume.AddSectionUseCase
import com.resumeapp.domain.usecase.resume.DeleteSectionUseCase
import com.resumeapp.domain.usecase.resume.SaveResumeUseCase
import com.resumeapp.ai.gateway.AIGatewayClient
import com.resumeapp.ai.cache.AICache
import com.resumeapp.ai.guardrail.GuardrailPipeline
import com.resumeapp.ai.trace.TraceLogger
import dagger.hilt.android.InstallIn
import dagger.hilt.android.components.SingletonComponent
import dagger.hilt.android.scopes.Singleton
import dagger.Module
import dagger.Provides

/**
 * UseCase Module - All UseCase bindings for Hilt
 */
@InstallIn(SingletonComponent::class)
@Module
object UseCaseModule {

    // Resume UseCases
    @Provides
    @Singleton
    fun provideCreateResumeUseCase(resumeRepository: ResumeRepository): CreateResumeUseCase = CreateResumeUseCase(resumeRepository)

    @Provides
    @Singleton
    fun provideGetResumeUseCase(resumeRepository: ResumeRepository): GetResumeUseCase = GetResumeUseCase(resumeRepository)

    @Provides
    @Singleton
    fun provideGetAllResumesUseCase(resumeRepository: ResumeRepository): GetAllResumesUseCase = GetAllResumesUseCase(resumeRepository)

    @Provides
    @Singleton
    fun provideUpdateResumeUseCase(resumeRepository: ResumeRepository): UpdateResumeUseCase = UpdateResumeUseCase(resumeRepository)

    @Provides
    @Singleton
    fun provideDeleteResumeUseCase(resumeRepository: ResumeRepository): DeleteResumeUseCase = DeleteResumeUseCase(resumeRepository)

    @Provides
    @Singleton
    fun provideDuplicateResumeUseCase(resumeRepository: ResumeRepository): DuplicateResumeUseCase = DuplicateResumeUseCase(resumeRepository)

    @Provides
    @Singleton
    fun provideReorderSectionsUseCase(resumeRepository: ResumeRepository): ReorderSectionsUseCase = ReorderSectionsUseCase(resumeRepository)

    @Provides
    @Singleton
    fun provideUpdateSectionUseCase(resumeRepository: ResumeRepository): UpdateSectionUseCase = UpdateSectionUseCase(resumeRepository)

    @Provides
    @Singleton
    fun provideAddSectionUseCase(resumeRepository: ResumeRepository): AddSectionUseCase = AddSectionUseCase(resumeRepository)

    @Provides
    @Singleton
    fun provideDeleteSectionUseCase(resumeRepository: ResumeRepository): DeleteSectionUseCase = DeleteSectionUseCase(resumeRepository)

    @Provides
    @Singleton
    fun provideSaveResumeUseCase(resumeRepository: ResumeRepository): SaveResumeUseCase = SaveResumeUseCase(resumeRepository)

    // AI Assist UseCases
    @Provides
    @Singleton
    fun provideImproveBulletsUseCase(
        aiGatewayClient: AIGatewayClient,
        aiCache: AICache,
        guardrailPipeline: GuardrailPipeline,
        traceLogger: TraceLogger
    ): ImproveBulletsUseCase = ImproveBulletsUseCase(aiGatewayClient, aiCache, guardrailPipeline, traceLogger)

    @Provides
    @Singleton
    fun provideExtractKeywordsUseCase(
        aiGatewayClient: AIGatewayClient,
        aiCache: AICache,
        traceLogger: TraceLogger
    ): ExtractKeywordsUseCase = ExtractKeywordsUseCase(aiGatewayClient, aiCache, traceLogger)

    @Provides
    @Singleton
    fun provideRephraseSectionUseCase(
        aiGatewayClient: AIGatewayClient,
        traceLogger: TraceLogger
    ): RephraseSectionUseCase = RephraseSectionUseCase(aiGatewayClient, traceLogger)

    @Provides
    @Singleton
    fun provideSuggestTemplatesUseCase(
        aiGatewayClient: AIGatewayClient,
        templateRepository: TemplateRepository
    ): SuggestTemplatesUseCase = SuggestTemplatesUseCase(aiGatewayClient, templateRepository)

    // Onboarding UseCases
    @Provides
    @Singleton
    fun provideRecordConsentUseCase(consentRepository: ConsentRepository): RecordConsentUseCase = RecordConsentUseCase(consentRepository)

    @Provides
    @Singleton
    fun provideCompleteOnboardingUseCase(resumeRepository: ResumeRepository): CompleteOnboardingUseCase = CompleteOnboardingUseCase(resumeRepository)

    // ATS UseCases
    @Provides
    @Singleton
    fun provideAnalyzeATSUseCase(
        atsScoreRepository: ATSScoreRepository,
        resumeRepository: ResumeRepository,
        aiGatewayClient: AIGatewayClient,
        aiCache: AICache
    ): AnalyzeATSUseCase = AnalyzeATSUseCase(atsScoreRepository, resumeRepository, aiGatewayClient, aiCache)

    @Provides
    @Singleton
    fun provideGetATSScoreUseCase(atsScoreRepository: ATSScoreRepository): GetATSScoreUseCase = GetATSScoreUseCase(atsScoreRepository)

    @Provides
    @Singleton
    fun provideApplyATSSuggestionUseCase(resumeRepository: ResumeRepository): ApplyATSSuggestionUseCase = ApplyATSSuggestionUseCase(resumeRepository)

    @Provides
    @Singleton
    fun provideRefreshATSUseCase(analyzeATSUseCase: AnalyzeATSUseCase): RefreshATSUseCase = RefreshATSUseCase(analyzeATSUseCase)
}