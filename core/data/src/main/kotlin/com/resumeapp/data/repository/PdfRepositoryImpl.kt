package com.resumeapp.data.repository

import com.resumeapp.domain.model.Result
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.repository.PdfRepository
import kotlinx.coroutines.tasks.await
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfRepositoryImpl @Inject constructor(
    private val context: android.content.Context
) : PdfRepository {

    private val fontFile by lazy {
        File(context.filesDir, "fonts/Roboto-Regular.ttf")
    }

    override suspend fun generatePdf(
        resume: Resume,
        template: String,
        watermark: Boolean
    ): Result<ByteArray> {
        return try {
            val pdfBytes = generatePdfDocument(resume, template, watermark)
            Result.Success(pdfBytes)
        } catch (e: Exception) {
            Result.Failure(e.message ?: "PDF generation failed")
        }
    }

    override suspend fun clearCache(): Result<Unit> {
        // No-op for local generation
        return Result.Success(Unit)
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

        // Sections
        y = addSection(document, page, contentStream, font, y, margin, "Summary", resume.summary)
        y = addSection(document, page, contentStream, font, y, margin, "Experience", 
            resume.experience.map { "${it.role} at ${it.company} (${it.startDate} - ${it.endDate})\n${it.description}" }.joinToString("\n\n"))
        y = addSection(document, page, contentStream, font, y, margin, "Education", 
            resume.education.map { "${it.degree} at ${it.institution} (${it.startDate} - ${it.endDate})" }.joinToString("\n"))
        y = addSection(document, page, contentStream, font, y, margin, "Skills", resume.skills.joinToString(", "))
        y = addSection(document, page, contentStream, font, y, margin, "Languages", 
            resume.languages.map { "${it.language} (${it.proficiency})" }.joinToString(", "))

        if (watermark) {
            addWatermark(page)
        }

        contentStream.close()
        document.save(outputStream)
        document.close()

        return outputStream.toByteArray()
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

    private fun addWatermark(page: PDPage) {
        val contentStream = PDPageContentStream(page, PDPageContentStream.AppendMode.APPEND, true, true)
        contentStream.setNonStrokingColor(200f/255f, 200f/255f, 200f/255f)
        contentStream.setFont(PDType0Font.load(contentStream.getResources(), java.lang.ClassLoader.getSystemResourceAsStream("fonts/Roboto-Regular.ttf")), 60f)
        contentStream.beginText()
        contentStream.setTextMatrix(java.awt.geom.AffineTransform.getRotateInstance(Math.toRadians(45.0)))
        contentStream.newLineAtOffset(page.mediaBox.width / 2 - 150, page.mediaBox.height / 2 - 50)
        contentStream.showText("RESUME AI")
        contentStream.endText()
        contentStream.close()
    }

    companion object {
        const val FONT_PATH = "fonts/Roboto-Regular.ttf"
    }
}