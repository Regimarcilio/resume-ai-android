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
    fun getAllActive(): List<Template>

    /**
     * Get template by ID
     */
    fun getById(id: String): Template?

    /**
     * Get templates by category
     */
    fun getByCategory(category: TemplateCategory): List<Template>

    /**
     * Get premium templates
     */
    fun getPremiumTemplates(): List<Template>

    /**
     * Get free templates
     */
    fun getFreeTemplates(): List<Template>

    /**
     * Save a template (for admin/seed)
     */
    fun save(template: Template): Template

    /**
     * Update a template
     */
    fun update(template: Template): Template
}