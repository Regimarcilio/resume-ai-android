package com.resumeapp.data.di

import com.resumeapp.data.cache.RoomAICache
import com.resumeapp.data.prefs.ConsentRepositoryImpl
import com.resumeapp.data.prefs.SubscriptionPrefsImpl
import com.resumeapp.data.prefs.UserPrefs
import com.resumeapp.data.prefs.UserPrefsImpl
import com.resumeapp.data.repository.ATSScoreRepositoryImpl
import com.resumeapp.data.repository.PdfRepositoryImpl
import com.resumeapp.data.repository.ResumeRepositoryImpl
import com.resumeapp.data.repository.TemplateRepositoryImpl
import com.resumeapp.data.repository.TraceRepositoryImpl
import com.resumeapp.data.sync.OfflineFirstRepository
import com.resumeapp.data.sync.SyncManager
import com.resumeapp.domain.ai.cache.AICache
import com.resumeapp.domain.ai.cache.CacheStats
import com.resumeapp.domain.repository.ATSScoreRepository
import com.resumeapp.domain.repository.AICacheRepository
import com.resumeapp.domain.repository.ConsentRepository
import com.resumeapp.domain.repository.PdfRepository
import com.resumeapp.domain.repository.ResumeRepository
import com.resumeapp.domain.repository.TemplateRepository
import com.resumeapp.domain.repository.TraceRepository
import com.resumeapp.domain.usecase.onboarding.CompleteOnboardingUseCase
import com.resumeapp.domain.usecase.onboarding.RecordConsentUseCase
import com.resumeapp.domain.usecase.resume.AddSectionUseCase
import com.resumeapp.domain.usecase.resume.CreateResumeUseCase
import com.resumeapp.domain.usecase.resume.DeleteResumeUseCase
import com.resumeapp.domain.usecase.resume.DeleteSectionUseCase
import com.resumeapp.domain.usecase.resume.DuplicateResumeUseCase
import com.resumeapp.domain.usecase.resume.GetAllResumesUseCase
import com.resumeapp.domain.usecase.resume.GetResumeUseCase
import com.resumeapp.domain.usecase.resume.ReorderSectionsUseCase
import com.resumeapp.domain.usecase.resume.SaveResumeUseCase
import com.resumeapp.domain.usecase.resume.UpdateResumeUseCase
import com.resumeapp.domain.usecase.resume.UpdateSectionUseCase
import com.resumeapp.domain.usecase.ats.AnalyzeATSUseCase
import com.resumeapp.domain.usecase.ats.ApplyATSSuggestionUseCase
import com.resumeapp.domain.usecase.ats.GetATSScoreUseCase
import com.resumeapp.domain.usecase.ats.RefreshATSUseCase
import com.resumeapp.prefs.SubscriptionPrefs
import dagger.hilt.android.InstallIn
import dagger.hilt.android.components.SingletonComponent
import dagger.Module
import dagger.Provides
import jakarta.inject.Singleton

/**
 * Core Data Module - Provides all data layer dependencies
 * This module is included in the AppModule and provides all repository implementations
 * and UseCases for the data layer
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    // Preferences
    @Provides
    @Singleton
    fun provideUserPrefs(): UserPrefs = TODO("Provided by AppModule")

    @Provides
    @Singleton
    fun provideConsentRepository(userPrefs: UserPrefs): ConsentRepository = ConsentRepositoryImpl(userPrefs)

    @Provides
    @Singleton
    fun provideSubscriptionPrefs(userPrefs: UserPrefs): SubscriptionPrefs = SubscriptionPrefsImpl(userPrefs)

    // Repository implementations
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
    fun provideAICacheRepository(impl: RoomAICache): AICacheRepository = impl

    // Sync
    @Provides
    @Singleton
    fun provideSyncManager(offlineFirstRepository: OfflineFirstRepository): SyncManager = SyncManager(offlineFirstRepository)

    @Provides
    @Singleton
    fun provideOfflineFirstRepository(): OfflineFirstRepository = TODO("Provided by SyncModule")

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
        aiGatewayClient: com.resumeapp.ai.gateway.AIGatewayClient,
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