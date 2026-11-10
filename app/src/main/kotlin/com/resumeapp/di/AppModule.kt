package com.resumeapp.di

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.hilt.lifecycle.ViewModelInject
import androidx.hilt.work.HiltWorkerFactory
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.Configuration
import com.resumeapp.data.db.AppDatabase
import com.resumeapp.data.db.ResumeDao
import com.resumeapp.data.db.TemplateDao
import com.resumeapp.data.db.ATSScoreDao
import com.resumeapp.data.db.AICacheDao
import com.resumeapp.data.db.TraceDao
import com.resumeapp.data.prefs.UserPrefs
import com.resumeapp.data.prefs.UserPrefsImpl
import com.resumeapp.data.prefs.ConsentRepository
import com.resumeapp.data.prefs.ConsentRepositoryImpl
import com.resumeapp.data.prefs.SubscriptionPrefs
import com.resumeapp.data.prefs.SubscriptionPrefsImpl
import com.resumeapp.data.repository.ResumeRepositoryImpl
import com.resumeapp.data.repository.TemplateRepositoryImpl
import com.resumeapp.data.repository.ATSScoreRepositoryImpl
import com.resumeapp.data.repository.TraceRepositoryImpl
import com.resumeapp.data.repository.PdfRepositoryImpl
import com.resumeapp.domain.repository.ResumeRepository
import com.resumeapp.domain.repository.TemplateRepository
import com.resumeapp.domain.repository.ATSScoreRepository
import com.resumeapp.domain.repository.AICacheRepository
import com.resumeapp.domain.repository.TraceRepository
import com.resumeapp.domain.repository.PdfRepository
import dagger.hilt.android.EntryPoint
import dagger.hilt.android.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.components.SingletonComponent
import dagger.hilt.android.scopes.ApplicationContext
import dagger.hilt.android.scopes.Singleton
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import com.resumeapp.ai.gateway.AIGatewayClient
import com.resumeapp.ai.gateway.AIGatewayClientImpl
import com.resumeapp.domain.ai.cache.AICache
import com.resumeapp.ai.guardrail.GuardrailPipeline
import com.resumeapp.ai.guardrail.InputSanitizer
import com.resumeapp.ai.guardrail.OutputValidator
import com.resumeapp.ai.guardrail.GuardrailPipelineImpl
import com.resumeapp.ai.trace.TraceLogger
import com.resumeapp.ai.trace.TraceLoggerImpl
import com.resumeapp.ai.trace.TraceUploader
import com.resumeapp.data.sync.SyncManager
import com.resumeapp.data.sync.OfflineFirstRepository
import com.resumeapp.data.cache.RoomAICache
import com.resumeapp.data.repository.ResumeRepositoryImpl
import com.resumeapp.data.repository.PdfRepositoryImpl
import com.resumeapp.domain.repository.PdfRepository
import com.resumeapp.domain.repository.ResumeRepository

/**
 * Main Application Module - Provides application-level dependencies
 * All singletons that live for the entire app lifecycle
 */
@Module(includes = [SyncModule::class])
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideMasterKey(@ApplicationContext context: Context): MasterKey {
        val spec = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(false) // Device credential optional for better UX
            .build()
        return spec
    }

    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context,
        masterKey: MasterKey
    ) = EncryptedSharedPreferences.create(
        "secure_prefs",
        masterKey,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context) = context.preferencesDataStore("user_prefs")

    @Provides
    @Singleton
    fun provideRoomDatabase(
        @ApplicationContext context: Context,
        masterKey: MasterKey
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "resume_db"
    )
        .fallbackToDestructiveMigration() // Only for dev, use proper migrations in prod
        .build()

    @Provides
    @Singleton
    fun provideHiltWorkerFactory(): HiltWorkerFactory = HiltWorkerFactory()

    @Provides
    @Singleton
    fun provideWorkManagerConfiguration(): Configuration = Configuration.Builder()
        .setWorkerFactory(HiltWorkerFactory())
        .build()
}

/**
 * Network Module - HTTP client, Retrofit, AI Gateway client
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): okhttp3.OkHttpClient = okhttp3.OkHttpClient.Builder()
        .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) okhttp3.logging.HttpLoggingInterceptor.Level.BODY else okhttp3.logging.HttpLoggingInterceptor.Level.NONE
        })
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideMoshi(): com.squareup.moshi.Moshi = com.squareup.moshi.Moshi.Builder()
        .add(com.squareup.moshi.kotlin.KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: okhttp3.OkHttpClient,
        moshi: com.squareup.moshi.Moshi
    ): retrofit2.Retrofit = retrofit2.Retrofit.Builder()
        .baseUrl(BuildConfig.AI_GATEWAY_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideGuardrailPipeline(): GuardrailPipeline = com.resumeapp.ai.di.AiModule().provideGuardrailPipeline(
        com.resumeapp.ai.di.AiModule().provideInputSanitizer(),
        com.resumeapp.ai.di.AiModule().provideOutputValidator()
    )

    @Provides
    @Singleton
    fun provideAIGatewayClient(
        okHttpClient: okhttp3.OkHttpClient,
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
    fun provideTraceUploader(@ApplicationContext context: Context, traceLogger: TraceLogger): TraceUploader = TraceUploader(traceLogger, context)
}

/**
 * Database Module - Room DAOs
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideResumeDao(database: AppDatabase): ResumeDao = database.resumeDao()

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
}

/**
 * Repository Module - Repository implementations
 */
@Module
@InstallIn(SingletonComponent::class)
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
    fun provideAICacheRepository(impl: RoomAICache): AICacheRepository = impl

    @Provides
    @Singleton
    fun provideTraceRepository(impl: TraceRepositoryImpl): TraceRepository = impl

    @Provides
    @Singleton
    fun providePdfRepository(impl: PdfRepositoryImpl): PdfRepository = impl
}

/**
 * Preferences Module - DataStore backed preferences
 */
@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun provideUserPrefs(
        @ApplicationContext context: Context,
        dataStore: androidx.datastore.preferences.DataStore<androidx.datastore.preferences.core.Preferences>,
        masterKey: androidx.security.crypto.MasterKey
    ): UserPrefs = UserPrefsImpl(context, dataStore, masterKey)

    @Provides
    @Singleton
    fun provideConsentRepository(userPrefs: UserPrefs): ConsentRepository = ConsentRepositoryImpl(userPrefs)

    @Provides
    @Singleton
    fun provideSubscriptionPrefs(userPrefs: UserPrefs): SubscriptionPrefs = SubscriptionPrefsImpl(userPrefs)
}