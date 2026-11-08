package com.resumeapp.data.prefs

import com.resumeapp.domain.model.ConsentRecord
import com.resumeapp.domain.model.ConsentPurpose
import kotlinx.coroutines.flow.Flow

interface ConsentRepository {
    val currentConsent: ConsentRecord?
    val consentFlow: Flow<ConsentRecord?>
    val hasValidConsent: Boolean
    val hasPurpose: (ConsentPurpose) -> Boolean

    suspend fun accept(record: ConsentRecord)
    suspend fun revoke()
    suspend fun updatePurposes(purposes: Set<ConsentPurpose>)
}

class ConsentRepositoryImpl @Inject constructor(
    private val userPrefs: UserPrefs
) : ConsentRepository {

    override val currentConsent: ConsentRecord?
        get() = userPrefs.consentRecordFlow.firstOrNull() ?: null

    override val consentFlow: Flow<ConsentRecord?> = userPrefs.consentRecordFlow

    override val hasValidConsent: Boolean
        get() = userPrefs.consentAccepted

    override val hasPurpose: (com.resumeapp.domain.model.ConsentPurpose) -> Boolean = { purpose ->
        userPrefs.consentPurposes.contains(purpose.name)
    }

    override suspend fun accept(record: ConsentRecord) {
        userPrefs.updateConsent(record)
    }

    override suspend fun revoke() {
        userPrefs.clearConsent()
    }

    override suspend fun updatePurposes(purposes: Set<com.resumeapp.domain.model.ConsentPurpose>) {
        val current = userPrefs.consentRecordFlow.firstOrNull()
            ?: com.resumeapp.domain.model.ConsentRecord(
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