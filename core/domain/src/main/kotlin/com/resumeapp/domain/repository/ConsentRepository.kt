package com.resumeapp.domain.repository

import com.resumeapp.domain.model.ConsentRecord
import com.resumeapp.domain.model.Result

/**
 * Repository interface for Consent operations (LGPD compliance)
 */
interface ConsentRepository {
    /**
     * Saves a consent record
     */
    suspend fun save(record: ConsentRecord): Result<Unit>

    /**
     * Gets the latest consent record for a user
     */
    suspend fun getLatest(userId: String): Result<ConsentRecord?>

    /**
     * Gets all consent records for a user
     */
    suspend fun getAll(userId: String): Result<List<ConsentRecord>>

    /**
     * Checks if user has given consent for a specific purpose
     */
    suspend fun hasConsent(userId: String, purpose: String): Result<Boolean>
}