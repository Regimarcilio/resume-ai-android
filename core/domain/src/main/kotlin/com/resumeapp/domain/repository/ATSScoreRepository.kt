package com.resumeapp.domain.repository

import com.resumeapp.domain.model.ATSScore

/**
 * Repository interface for ATS Score operations
 * Implemented in core:data module with Room
 */
interface ATSScoreRepository {

    /**
     * Save ATS score
     */
    suspend fun save(score: ATSScore): ATSScore

    /**
     * Get ATS score by resume ID
     */
    suspend fun getByResumeId(resumeId: String): ATSScore?

    /**
     * Get all ATS scores for a user
     */
    suspend fun getAllByUserId(userId: String): List<ATSScore>

    /**
     * Delete ATS score
     */
    suspend fun delete(score: ATSScore)
}