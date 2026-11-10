package com.resumeapp.data.repository

import com.resumeapp.data.db.AppDatabase
import com.resumeapp.data.db.PdfCacheEntity
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.model.SectionItem
import com.resumeapp.domain.repository.PdfRepository
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import java.io.ByteArrayOutputStream
import java.io.File
import android.graphics.Matrix

class PdfRepositoryImpl constructor(
    private val context: android.content.Context,
    private val database: AppDatabase
) : PdfRepository {

    private val fontFile by lazy {
        File(context.filesDir, "fonts/Roboto-Regular.ttf")
    }

    override suspend fun generatePdf(
        resume: Resume,
        template: String,
        watermark: Boolean
    ): ByteArray {
        // Check cache first
        val cached = database.pdfCacheDao().getCached(resume.id, template, watermark)
        if (cached != null) {
            return cached.pdfData
        }

        val pdfBytes = generatePdfDocument(resume, template, watermark)

        // Cache the result
        val cacheEntry = PdfCacheEntity(
            id = "${resume.id}_${template}_${watermark}",
            resumeId = resume.id,
            template = template,
            watermark = watermark,
            pdfData = pdfBytes,
            createdAt = System.currentTimeMillis()
        )
        database.pdfCacheDao().insert(cacheEntry)

        return pdfBytes
    }

    override suspend fun clearCache() {
        database.pdfCacheDao().deleteAll()
    }

    private fun generatePdfDocument(
        resume: Resume,
        template: String,
        watermark: Boolean
    ): ByteArray {
        val document = PDDocument()
        val page = PDPage(PDRectangle.A4)
        document.addPage(page)

        val font = if (fontFile.exists()) {
            PDType0Font.load(document, fontFile)
        } else {
            // Fallback to built-in font
            PDType0Font.load(document, context.assets.open("fonts/Roboto-Regular.ttf"))
        }

        val outputStream = ByteArrayOutputStream()
        val contentStream = PDPageContentStream(document, page)
        val margin = 50f
        var y = page.mediaBox.height - margin

        // Title
        contentStream.beginText()
        contentStream.setFont(font, 24f)
        contentStream.newLineAtOffset(margin, y)
        contentStream.showText(resume.personalInfo.fullName)
        contentStream.endText()
        y -= 40f

        // Contact info
        contentStream.beginText()
        contentStream.setFont(font, 10f)
        contentStream.newLineAtOffset(margin, y)
        contentStream.showText("${resume.personalInfo.email} | ${resume.personalInfo.phone}")
        contentStream.endText()
        y -= 20f

        if (resume.personalInfo.linkedin.isNotBlank()) {
            contentStream.beginText()
            contentStream.setFont(font, 10f)
            contentStream.newLineAtOffset(margin, y)
            contentStream.showText(resume.personalInfo.linkedin)
            contentStream.endText()
            y -= 20f
        }

        // Sections - Build content from sections
        val summarySection = resume.sections.firstOrNull { it.type.name == "CUSTOM" && it.title == "Summary" }
        val experienceSection = resume.sections.firstOrNull { it.type.name == "EXPERIENCE" }
        val educationSection = resume.sections.firstOrNull { it.type.name == "EDUCATION" }
        val skillsSection = resume.sections.firstOrNull { it.type.name == "SKILLS" }
        val languagesSection = resume.sections.firstOrNull { it.type.name == "LANGUAGES" }

        // Use personalInfo summary if no summary section
        val summary = summarySection?.itemsJson?.let { parseItems(it).joinToString("\n") } ?: resume.personalInfo.summary

        y = addSection(document, page, contentStream, font, y, margin, "Summary", summary)
        
        // Build experience content from SectionItems
        val experienceContent = experienceSection?.let { parseItems(it.itemsJson)
            .map { "${it.title} at ${it.subtitle} (${it.startDate} - ${it.endDate})\n${it.description}" }
            .joinToString("\n\n")
        }
        y = addSection(document, page, contentStream, font, y, margin, "Experience", experienceContent)
        
        // Build education content from SectionItems
        val educationContent = educationSection?.let { parseItems(it.itemsJson)
            .map { "${it.title} at ${it.subtitle} (${it.startDate} - ${it.endDate})" }
            .joinToString("\n")
        }
        y = addSection(document, page, contentStream, font, y, margin, "Education", educationContent)
        
        // Build skills content from SectionItems
        val skillsContent = skillsSection?.let { parseItems(it.itemsJson)
            .map { it.title }
            .joinToString(", ")
        }
        y = addSection(document, page, contentStream, font, y, margin, "Skills", skillsContent)
        
        // Build languages content from SectionItems
        val languagesContent = languagesSection?.let { parseItems(it.itemsJson)
            .map { "${it.title} (${it.subtitle})" }
            .joinToString(", ")
        }
        y = addSection(document, page, contentStream, font, y, margin, "Languages", languagesContent)

        if (watermark) {
            addWatermark(page, document)
        }

        contentStream.close()
        document.save(outputStream)
        document.close()

        return outputStream.toByteArray()
    }

    private fun parseItems(itemsJson: String): List<SectionItem> {
        val gson = com.google.gson.Gson()
        val type = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, SectionItem::class.java).type
        return gson.fromJson(itemsJson, type) ?: emptyList()
    }

    private fun addSection(
        document: PDDocument,
        page: PDPage,
        contentStream: PDPageContentStream,
        font: PDType0Font,
        y: Float,
        margin: Float,
        title: String,
        content: String?
    ): Float {
        if (content.isNullOrBlank()) return y

        var currentY = y
        if (currentY < margin + 50f) {
            contentStream.close()
            val newPage = PDPage(PDRectangle.A4)
            document.addPage(newPage)
            return addSection(document, newPage, PDPageContentStream(document, newPage), font, newPage.mediaBox.height - margin, margin, title, content)
        }

        contentStream.beginText()
        contentStream.setFont(font, 14f)
        contentStream.newLineAtOffset(margin, currentY)
        contentStream.showText(title)
        contentStream.endText()
        currentY -= 25f

        contentStream.beginText()
        contentStream.setFont(font, 11f)
        contentStream.newLineAtOffset(margin, currentY)
        content.split("\n").forEach { line ->
            if (currentY < margin) {
                contentStream.endText()
                contentStream.close()
                val newPage = PDPage(PDRectangle.A4)
                document.addPage(newPage)
                val newContentStream = PDPageContentStream(document, newPage)
                newContentStream.beginText()
                newContentStream.setFont(font, 11f)
                newContentStream.newLineAtOffset(margin, newPage.mediaBox.height - margin)
                return@forEach
            }
            contentStream.showText(line)
            contentStream.newLine()
            currentY -= 15f
        }
        contentStream.endText()
        currentY -= 15f

        return currentY
    }

    private fun addWatermark(page: PDPage, document: PDDocument) {
        val watermarkStream = PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)
        watermarkStream.setNonStrokingColor(200f/255f, 200f/255f, 200f/255f)
        val watermarkFont = if (fontFile.exists()) {
            PDType0Font.load(document, fontFile)
        } else {
            PDType0Font.load(document, context.assets.open("fonts/Roboto-Regular.ttf"))
        }
        watermarkStream.setFont(watermarkFont, 60f)
        // Create rotation matrix using pdfbox's own Matrix class
        val matrix = org.apache.pdfbox.util.Matrix.getRotateInstance(
            Math.toRadians(45.0).toFloat(),
            (page.mediaBox.width / 2.0f).toFloat(),
            (page.mediaBox.height / 2.0f).toFloat()
        )
        watermarkStream.setTextMatrix(matrix)
        watermarkStream.newLineAtOffset(page.mediaBox.width / 2 - 150, page.mediaBox.height / 2 - 50)
        watermarkStream.showText("RESUME AI")
        watermarkStream.endText()
        watermarkStream.close()
    }

    companion object {
        const val FONT_PATH = "fonts/Roboto-Regular.ttf"
    }
}