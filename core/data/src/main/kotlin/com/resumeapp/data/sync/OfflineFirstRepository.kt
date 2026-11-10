package com.resumeapp.data.sync

import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.repository.PdfRepository
import com.resumeapp.domain.repository.ResumeRepository
import com.resumeapp.ai.cache.AICache
import com.resumeapp.ai.gateway.AIGatewayClient
import com.resumeapp.ai.gateway.AIRequest
import com.resumeapp.ai.gateway.AIResponse
import com.resumeapp.ai.gateway.Result as GatewayResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class OfflineFirstRepository @Inject constructor(
    private val syncManager: SyncManager,
    private val resumeRepository: ResumeRepository,
    private val pdfRepository: PdfRepository,
    private val aiCache: AICache,
    private val aiGatewayClient: AIGatewayClient,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) {

    // Resume operations (always work offline - local DB)
    suspend fun createResume(resume: Resume) = resumeRepository.save(resume)
    suspend fun getResume(id: String) = resumeRepository.getById(id)
    suspend fun updateResume(resume: Resume) = resumeRepository.update(resume)
    suspend fun deleteResume(id: String) = resumeRepository.deleteById(id)
    suspend fun getResumes(userId: String) = resumeRepository.getAllByUserId(userId)

    // PDF generation - works offline with cached results, queues for server when needed
    suspend fun generatePdf(resume: Resume, template: String, watermark: Boolean) = pdfRepository.generatePdf(resume, template, watermark)

    // AI Assist - works offline with cached results, queues for server
    suspend fun aiChatCompletion(request: AIRequest): AIResponse = aiGatewayClient.complete(request).let { result ->
        when (result) {
            is GatewayResult.Success -> result.value
            is GatewayResult.Failure -> throw IllegalStateException(result.errorMessage)
        }
    }

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