package com.resumeapp.domain.repository

import com.resumeapp.domain.model.Template
import com.resumeapp.domain.model.TemplateCategory

/**
 * Repository interface for Template operations
 * Implemented in core:data module with Room
 */
interface TemplateRepository {

    /**
     * Get all active templates
     */
    suspend fun getAllActive(): List<Template>

    /**
     * Get template by ID
     */
    suspend fun getById(id: String): Template?

    /**
     * Get templates by category
     */
    suspend fun getByCategory(category: TemplateCategory): List<Template>

    /**
     * Get premium templates
     */
    suspend fun getPremiumTemplates(): List<Template>

    /**
     * Get free templates
     */
    suspend fun getFreeTemplates(): List<Template>

    /**
     * Save a template (for admin/seed)
     */
    suspend fun save(template: Template): Template

    /**
     * Update a template
     */
    suspend fun update(template: Template): Template
}