package com.resumeapp.domain.repository

import com.resumeapp.domain.model.Resume
import kotlinx.coroutines.Dispatchers

interface PdfRepository {
    suspend fun generatePdf(
        resume: Resume,
        template: String,
        watermark: Boolean
    ): Result<ByteArray>

    suspend fun clearCache()
}