package com.resumeapp.data.sync

import com.resumeapp.data.db.AppDatabase
import com.resumeapp.data.repository.ResumeRepositoryImpl
import com.resumeapp.pdf.repository.PdfRepositoryImpl
import com.resumeapp.ai.client.AiGatewayClientImpl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val database: AppDatabase,
    private val resumeRepository: ResumeRepositoryImpl,
    private val pdfRepository: PdfRepositoryImpl,
    private val aiGatewayClient: AiGatewayClientImpl,
    private val ioDispatcher: CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO
) {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    private val _pendingOperations = MutableStateFlow<List<SyncOperation>>(emptyList())
    val pendingOperations = _pendingOperations.asStateFlow()

    // Queue operations for offline sync
    suspend fun queueOperation(operation: SyncOperation) {
        _pendingOperations.update { it + operation }
        // Persist to database for durability
        // database.syncOperationDao().insert(operation.toEntity())
    }

    // Process queue when online
    suspend fun processQueue() {
        if (!isOnline()) return
        
        _syncState.value = SyncState.Syncing
        
        val operations = _pendingOperations.value
        for (operation in operations) {
            try {
                when (operation) {
                    is SyncOperation.GeneratePdf -> processPdfGeneration(operation)
                    is SyncOperation.AiAssist -> processAiAssist(operation)
                    is SyncOperation.ATSScore -> processATSScore(operation)
                    is SyncOperation.SubscriptionUpdate -> processSubscriptionUpdate(operation)
                }
                _pendingOperations.update { it - operation }
            } catch (e: Exception) {
                // Log error, keep in queue for retry
                operation.incrementRetry()
                if (operation.retryCount >= 3) {
                    _pendingOperations.update { it - operation }
                    // Move to dead letter queue
                }
            }
        }
        
        _syncState.value = SyncState.Idle
    }

    private suspend fun processPdfGeneration(op: SyncOperation.GeneratePdf) {
        // PDF generation requires server - queue for when online
        // Could generate locally with pdfbox-android as fallback
    }

    private suspend fun processAiAssist(op: SyncOperation.AiAssist) {
        // AI requires server - cache request, process when online
    }

    private suspend fun processATSScore(op: SyncOperation.ATSScore) {
        // ATS scoring is on-device - can process immediately
    }

    private suspend fun processSubscriptionUpdate(op: SyncOperation.SubscriptionUpdate) {
        // Verify with Play Billing when online
    }

    private fun isOnline(): Boolean {
        // Check connectivity - simplified for now
        return true
    }

    // Manual sync trigger (e.g., pull-to-refresh)
    suspend fun triggerSync() {
        processQueue()
    }

    // Clear all pending operations (e.g., user logout)
    suspend fun clearQueue() {
        _pendingOperations.value = emptyList()
    }
}

sealed interface SyncState {
    data class Idle(val lastSyncTime: Long? = null) : SyncState
    data class Syncing(val progress: Float = 0f) : SyncState
    data class Error(val message: String) : SyncState
}

sealed interface SyncOperation {
    val id: String
    val createdAt: Long
    var retryCount: Int
    fun incrementRetry() { retryCount++ }

    data class GeneratePdf(
        override val id: String,
        override val createdAt: Long,
        var retryCount: Int,
        val resumeId: String,
        val template: String,
        val watermark: Boolean
    ) : SyncOperation

    data class AiAssist(
        override val id: String,
        override val createdAt: Long,
        var retryCount: Int,
        val promptHash: String,
        val promptType: String,
        val contextJson: String
    ) : SyncOperation

    data class ATSScore(
        override val id: String,
        override val createdAt: Long,
        var retryCount: Int,
        val resumeId: String,
        val jobDescription: String
    ) : SyncOperation

    data class SubscriptionUpdate(
        override val id: String,
        override val createdAt: Long,
        var retryCount: Int,
        val purchaseToken: String
    ) : SyncOperation
}