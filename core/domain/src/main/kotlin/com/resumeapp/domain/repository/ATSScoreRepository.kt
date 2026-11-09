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
    fun save(score: ATSScore): ATSScore

    /**
     * Get ATS score by resume ID
     */
    fun getByResumeId(resumeId: String): ATSScore?

    /**
     * Get all ATS scores for a user
     */
    fun getAllByUserId(userId: String): List<ATSScore>

    /**
     * Delete ATS score
     */
    fun delete(score: ATSScore)
}