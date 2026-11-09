package com.resumeapp.di

import com.resumeapp.data.db.AppDatabase
import com.resumeapp.data.repository.ResumeRepositoryImpl
import com.resumeapp.data.repository.PdfRepositoryImpl
import com.resumeapp.data.cache.RoomAICache
import com.resumeapp.domain.repository.PdfRepository
import com.resumeapp.domain.repository.ResumeRepository
import com.resumeapp.ai.cache.AICache
import com.resumeapp.ai.gateway.AIGatewayClient
import kotlinx.coroutines.Dispatchers
import jakarta.inject.Singleton
import dagger.Module
import dagger.Provides

/**
 * Sync Module - Provides sync-related dependencies as plain Dagger module
 * Included in AppModule via @Module(includes = [SyncModule::class])
 */
@Module
object SyncModule {

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

    @Provides
    @Singleton
    fun provideAICache(roomAICache: RoomAICache): AICache = roomAICache
}