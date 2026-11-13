package com.resumeapp.di.modules

import com.google.gson.Gson
import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.resumeapp.ai.cache.AICache
import com.resumeapp.ai.gateway.AIGatewayClient
import com.resumeapp.ai.gateway.AIGatewayClientImpl
import com.resumeapp.ai.guardrail.GuardrailPipeline
import com.resumeapp.ai.guardrail.InputSanitizer
import com.resumeapp.ai.guardrail.OutputValidator
import com.resumeapp.ai.guardrail.GuardrailPipelineImpl
import com.resumeapp.ai.trace.TraceLogger
import com.resumeapp.ai.trace.TraceLoggerImpl
import com.resumeapp.ai.trace.TraceUploader
import com.resumeapp.ai.trace.TraceUploaderImpl
import com.resumeapp.data.cache.RoomAICache
import com.resumeapp.BuildConfig
import com.resumeapp.data.db.AppDatabase
import com.resumeapp.data.db.ResumeDao
import com.resumeapp.data.db.ResumeSectionDao
import com.resumeapp.data.db.TemplateDao
import com.resumeapp.data.db.ATSScoreDao
import com.resumeapp.data.db.AICacheDao
import com.resumeapp.data.db.TraceDao
import com.resumeapp.data.prefs.ConsentRepositoryImpl
import com.resumeapp.data.prefs.SubscriptionPrefs
import com.resumeapp.data.prefs.SubscriptionPrefsImpl
import com.resumeapp.domain.usecase.resume.ExportPdfUseCase
import com.resumeapp.domain.repository.PaymentGateway
import com.resumeapp.domain.usecase.subscription.StartCheckoutUseCase
import com.resumeapp.domain.usecase.subscription.AwaitPaymentUseCase
import com.resumeapp.data.prefs.UserPrefs
import com.resumeapp.data.prefs.UserPrefsImpl
import com.resumeapp.data.repository.ATSScoreRepositoryImpl
import com.resumeapp.data.repository.PdfRepositoryImpl
import com.resumeapp.data.repository.ResumeRepositoryImpl
import com.resumeapp.data.repository.TemplateRepositoryImpl
import com.resumeapp.data.repository.TraceRepositoryImpl
import com.resumeapp.data.sync.OfflineFirstRepository
import com.resumeapp.data.sync.SyncManager
import com.resumeapp.domain.repository.ATSScoreRepository
import com.resumeapp.domain.repository.ConsentRepository
import com.resumeapp.domain.repository.PdfRepository
import com.resumeapp.domain.repository.ResumeRepository
import com.resumeapp.domain.repository.TemplateRepository
import com.resumeapp.domain.repository.TraceRepository
import com.resumeapp.domain.usecase.ats.AnalyzeATSUseCase
import com.resumeapp.domain.usecase.ats.ApplyATSSuggestionUseCase
import com.resumeapp.domain.usecase.ats.GetATSScoreUseCase
import com.resumeapp.domain.usecase.ats.RefreshATSUseCase
import com.resumeapp.domain.usecase.aiassist.ExtractKeywordsUseCase
import com.resumeapp.domain.usecase.aiassist.ImproveBulletsUseCase
import com.resumeapp.domain.usecase.aiassist.RephraseSectionUseCase
import com.resumeapp.domain.usecase.aiassist.SuggestTemplatesUseCase
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
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.KotlinJsonAdapterFactory
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton
import dagger.Module
import dagger.Provides
import javax.inject.Inject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers

/**
 * Main Application Module - All application-level dependencies consolidated
 * Single module to avoid KSP/Hilt processing issues with multiple module files
 */
@InstallIn(SingletonComponent::class)
@Module
object AppModule {

    // ============================================================
    // Application Context & Security
    // ============================================================

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideMasterKey(@ApplicationContext context: Context): MasterKey {
        val spec = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(false)
            .build()
        return spec
    }

    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context,
        masterKey: MasterKey
    ): SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("user_prefs") }
        )

    // ============================================================
    // Room Database
    // ============================================================

    @Provides
    @Singleton
    fun provideRoomDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        // O callback de seeding roda na primeira abertura do banco, quando a
        // instancia ja esta atribuida.
        lateinit var database: AppDatabase
        database = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "resume_db"
        )
            .addCallback(AppDatabase.seedCallback { database })
            .fallbackToDestructiveMigration()
            .build()
        return database
    }

    // ============================================================
    // Room DAOs
    // ============================================================

    @Provides
    @Singleton
    fun provideResumeDao(database: AppDatabase): ResumeDao = database.resumeDao()

    @Provides
    @Singleton
    fun provideResumeSectionDao(database: AppDatabase): ResumeSectionDao = database.resumeSectionDao()

    @Provides
    @Singleton
    fun provideTemplateDao(database: AppDatabase): TemplateDao = database.templateDao()

    @Provides
    @Singleton
    fun provideATSScoreDao(database: AppDatabase): ATSScoreDao = database.atsScoreDao()

    @Provides
    @Singleton
    fun provideAICacheDao(database: AppDatabase): AICacheDao = database.aiCacheDao()

    @Provides
    @Singleton
    fun provideTraceDao(database: AppDatabase): TraceDao = database.traceDao()

    // ============================================================
    // DataStore Preferences
    // ============================================================

    @Provides
    @Singleton
    fun provideUserPrefs(
        @ApplicationContext context: Context,
        dataStore: DataStore<Preferences>
    ): UserPrefs = UserPrefsImpl(context, dataStore)

    @Provides
    @Singleton
    fun provideConsentRepository(userPrefs: UserPrefs): ConsentRepository = ConsentRepositoryImpl(userPrefs)

    @Provides
    @Singleton
    fun provideSubscriptionPrefs(userPrefs: UserPrefs): SubscriptionPrefs = SubscriptionPrefsImpl(userPrefs)

    // ============================================================
    // Repository Implementations
    // ============================================================

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
    fun providePdfRepository(
        @ApplicationContext context: Context,
        database: AppDatabase
    ): PdfRepository = PdfRepositoryImpl(context, database)

    @Provides
    @Singleton
    fun provideAICacheRepository(impl: RoomAICache): AICache = impl

    // ============================================================
    // Network / HTTP / AI Gateway
    // ============================================================

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
    fun provideGson(): Gson = Gson()

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
        .baseUrl("https://api.openai.com/v1/")
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
        aiCache: AICache,
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
    fun provideTraceUploader(traceLogger: TraceLogger): TraceUploader = TraceUploaderImpl(traceLogger)

    // ============================================================
    // Sync / Offline-First
    // ============================================================

    @Provides
    @Singleton
    fun provideSyncManager(
        database: AppDatabase,
        resumeRepository: ResumeRepository,
        pdfRepository: PdfRepository,
        aiCache: AICache,
        aiGatewayClient: AIGatewayClient,
        ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
    ): SyncManager {
        return SyncManager(database, resumeRepository, pdfRepository, aiCache, aiGatewayClient, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideOfflineFirstRepository(
        syncManager: SyncManager,
        resumeRepository: ResumeRepository,
        pdfRepository: PdfRepository,
        aiCache: AICache,
        aiGatewayClient: AIGatewayClient,
        ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
    ): OfflineFirstRepository {
        return OfflineFirstRepository(syncManager, resumeRepository, pdfRepository, aiCache, aiGatewayClient, ioDispatcher)
    }

    // ============================================================
    // UseCases - Resume
    // ============================================================

    @Provides
    @Singleton
    fun provideCreateResumeUseCase(
        resumeRepository: ResumeRepository,
        subscriptionPrefs: SubscriptionPrefs
    ): CreateResumeUseCase = CreateResumeUseCase(resumeRepository) { subscriptionPrefs.currentStatus }

    @Provides
    @Singleton
    fun providePaymentGateway(gateway: com.resumeapp.ai.billing.PayMasterBackendGateway): PaymentGateway = gateway

    @Provides
    @Singleton
    fun provideStartCheckoutUseCase(
        paymentGateway: PaymentGateway,
        subscriptionPrefs: SubscriptionPrefs
    ): StartCheckoutUseCase = StartCheckoutUseCase(paymentGateway) { purchaseToken ->
        subscriptionPrefs.saveSubscriptionStatus(
            com.resumeapp.domain.model.SubscriptionStatus(
                tier = com.resumeapp.domain.model.SubscriptionTier.PRO,
                isActive = true,
                purchaseToken = purchaseToken
            )
        )
    }

    @Provides
    @Singleton
    fun provideAwaitPaymentUseCase(paymentGateway: PaymentGateway): AwaitPaymentUseCase =
        AwaitPaymentUseCase(paymentGateway)

    @Provides
    @Singleton
    fun provideExportPdfUseCase(
        pdfRepository: PdfRepository,
        templateRepository: TemplateRepository,
        subscriptionPrefs: SubscriptionPrefs
    ): ExportPdfUseCase = ExportPdfUseCase(pdfRepository, templateRepository) {
        subscriptionPrefs.currentStatus
    }

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

    // ============================================================
    // UseCases - AI Assist
    // ============================================================

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

    // ============================================================
    // UseCases - Onboarding
    // ============================================================

    @Provides
    @Singleton
    fun provideRecordConsentUseCase(consentRepository: ConsentRepository): RecordConsentUseCase = RecordConsentUseCase(consentRepository)

    @Provides
    @Singleton
    fun provideCompleteOnboardingUseCase(resumeRepository: ResumeRepository): CompleteOnboardingUseCase = CompleteOnboardingUseCase(resumeRepository)

    // ============================================================
    // UseCases - ATS
    // ============================================================

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