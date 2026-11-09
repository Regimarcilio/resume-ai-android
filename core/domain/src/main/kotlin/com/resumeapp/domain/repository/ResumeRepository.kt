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
    fun save(resume: Resume): Resume

    /**
     * Update an existing resume
     */
    fun update(resume: Resume): Resume

    /**
     * Get resume by ID
     */
    fun getById(id: String): Resume?

    /**
     * Get all resumes for a user
     */
    fun getAllByUserId(userId: String): List<Resume>

    /**
     * Search resumes with filters
     */
    fun search(params: SearchParams): List<Resume>

    /**
     * Delete resume by ID
     */
    fun deleteById(id: String)

    /**
     * Get resume count for user
     */
    fun countByUserId(userId: String): Int
}