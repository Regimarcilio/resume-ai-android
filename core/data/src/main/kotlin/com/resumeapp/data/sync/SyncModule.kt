package com.resumeapp.data.sync

import com.resumeapp.data.db.AppDatabase
import com.resumeapp.data.repository.ResumeRepositoryImpl
import com.resumeapp.pdf.repository.PdfRepositoryImpl
import com.resumeapp.ai.client.AiGatewayClientImpl
import com.resumeapp.domain.repository.PdfRepository
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideSyncManager(
        database: AppDatabase,
        resumeRepository: ResumeRepositoryImpl,
        pdfRepository: PdfRepositoryImpl,
        aiGatewayClient: AiGatewayClientImpl,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ): SyncManager {
        return SyncManager(database, resumeRepository, pdfRepository, aiGatewayClient, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideOfflineFirstRepository(
        syncManager: SyncManager,
        resumeRepository: ResumeRepositoryImpl,
        pdfRepository: PdfRepositoryImpl,
        aiGatewayClient: AiGatewayClientImpl,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ): OfflineFirstRepository {
        return OfflineFirstRepository(syncManager, resumeRepository, pdfRepository, aiGatewayClient, ioDispatcher)
    }
}