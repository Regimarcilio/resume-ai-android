package com.resumeapp.data.prefs

import com.resumeapp.domain.model.ConsentRecord
import com.resumeapp.domain.model.ConsentPurpose
import com.resumeapp.domain.model.Result
import com.resumeapp.domain.model.Result.Success
import com.resumeapp.domain.model.Result.Failure
import com.resumeapp.domain.repository.ConsentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ConsentRepositoryImpl constructor(
    private val userPrefs: UserPrefs
) : ConsentRepository {

    override suspend fun save(record: ConsentRecord): Result<Unit> {
        return try {
            userPrefs.updateConsent(record)
            Success(Unit)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    override suspend fun getLatest(userId: String): Result<ConsentRecord?> {
        return try {
            val record = runBlocking { userPrefs.consentRecordFlow.first() }
            Success(record)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    override suspend fun getAll(userId: String): Result<List<ConsentRecord>> {
        return try {
            val record = runBlocking { userPrefs.consentRecordFlow.first() }
            Success(record?.let { listOf(it) } ?: emptyList())
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    override suspend fun hasConsent(userId: String, purpose: String): Result<Boolean> {
        return try {
            val hasPurpose = userPrefs.consentPurposes.contains(purpose)
            Success(hasPurpose)
        } catch (e: Exception) {
            Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    // Legacy interface methods for backward compatibility
    val currentConsent: ConsentRecord?
        get() = runBlocking { userPrefs.consentRecordFlow.first() }

    val consentFlow: Flow<ConsentRecord?> = userPrefs.consentRecordFlow

    val hasValidConsent: Boolean
        get() = userPrefs.consentAccepted

    val hasPurpose: (ConsentPurpose) -> Boolean = { purpose ->
        userPrefs.consentPurposes.contains(purpose.name)
    }

    suspend fun accept(record: ConsentRecord) {
        userPrefs.updateConsent(record)
    }

    suspend fun revoke() {
        userPrefs.clearConsent()
    }

    suspend fun updatePurposes(purposes: Set<ConsentPurpose>) {
        val current = runBlocking { userPrefs.consentRecordFlow.first() }
            ?: ConsentRecord(
                version = "1.0",
                timestamp = System.currentTimeMillis(),
                policyHash = "",
                accepted = true,
                purposes = purposes
            )
        val updated = current.copy(
            purposes = purposes,
            timestamp = System.currentTimeMillis()
        )
        userPrefs.updateConsent(updated)
    }
}