package com.resumeapp.data.sync

import com.resumeapp.data.repository.ResumeRepositoryImpl
import com.resumeapp.pdf.repository.PdfRepositoryImpl
import com.resumeapp.ai.client.AiGatewayClientImpl
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.repository.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstRepository @Inject constructor(
    private val syncManager: SyncManager,
    private val resumeRepository: ResumeRepositoryImpl,
    private val pdfRepository: PdfRepositoryImpl,
    private val aiGatewayClient: AiGatewayClientImpl,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    // Resume operations (always work offline - local DB)
    suspend fun createResume(resume: Resume) = resumeRepository.createResume(resume)
    suspend fun getResume(id: String) = resumeRepository.getResume(id)
    suspend fun updateResume(resume: Resume) = resumeRepository.updateResume(resume)
    suspend fun deleteResume(id: String) = resumeRepository.deleteResume(id)
    suspend fun getResumes(userId: String) = resumeRepository.getResumes(userId)

    // PDF generation - works offline with cached results, queues for server when needed
    suspend fun generatePdf(resume: Resume, template: String, watermark: Boolean) = pdfRepository.generatePdf(resume, template, watermark)

    // AI Assist - works offline with cached results, queues for server
    suspend fun aiChatCompletion(request: com.resumeapp.ai.client.AiModels.AiChatRequest) = 
        aiGatewayClient.chatCompletion(request)

    // ATS Scoring - fully offline (on-device)
    suspend fun calculateATSScore(resumeId: String, jobDescription: String) = 
        syncManager.queueOperation(SyncOperation.ATSScore(
            id = "ats_${resumeId}_${System.currentTimeMillis()}",
            createdAt = System.currentTimeMillis(),
            retryCount = 0,
            resumeId = resumeId,
            jobDescription = jobDescription
        ))

    // Sync status
    val syncState = syncManager.syncState
    val pendingOperations = syncManager.pendingOperations

    suspend fun triggerSync() = syncManager.triggerSync()
    suspend fun clearSyncQueue() = syncManager.clearQueue()
}