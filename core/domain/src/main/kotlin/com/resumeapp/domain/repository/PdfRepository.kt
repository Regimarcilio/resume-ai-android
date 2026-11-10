package com.resumeapp.domain.repository

import com.resumeapp.domain.model.Resume

interface PdfRepository {
    suspend fun generatePdf(
        resume: Resume,
        template: String,
        watermark: Boolean
    ): ByteArray

    suspend fun clearCache()
}