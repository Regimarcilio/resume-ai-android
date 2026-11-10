package com.resumeapp.domain.repository

import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.usecase.SearchParams

/**
 * Repository interface for Resume operations
 * Implemented in core:data module with Room + SQLCipher
 */
interface ResumeRepository {

    /**
     * Save a new resume
     */
    suspend fun save(resume: Resume): Resume

    /**
     * Update an existing resume
     */
    suspend fun update(resume: Resume): Resume

    /**
     * Get resume by ID
     */
    suspend fun getById(id: String): Resume?

    /**
     * Get all resumes for a user
     */
    suspend fun getAllByUserId(userId: String): List<Resume>

    /**
     * Search resumes with filters
     */
    suspend fun search(params: SearchParams): List<Resume>

    /**
     * Delete resume by ID
     */
    suspend fun deleteById(id: String)

    /**
     * Get resume count for user
     */
    suspend fun countByUserId(userId: String): Int
}