package com.resumeapp.pdfbox.test

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PdfBoxGenerator {

    companion object {
        private const val TAG = "PdfBoxGenerator"
        
        fun generateResumePdf(context: Context, resumeData: ResumeData, outputFile: File): BenchmarkResult {
            val startTime = System.nanoTime()
            var peakMemory = 0L
            
            try {
                val document = PDDocument()
                val page = PDPage(PDRectangle.A4)
                document.addPage(page)
                
                // Load font (using built-in or custom TTF)
                val fontInputStream = context.assets.open("fonts/Roboto-Regular.ttf")
                val fontBoldInputStream = context.assets.open("fonts/Roboto-Bold.ttf")
                
                val font = PDType0Font.load(document, fontInputStream)
                val fontBold = PDType0Font.load(document, fontBoldInputStream)
                
                val contentStream = PDPageContentStream(document, page)
                
                val margin = 50f
                val pageWidth = PDRectangle.A4.width - 2 * margin
                var yPosition = PDRectangle.A4.height - margin
                val lineHeight = 14f
                val sectionSpacing = 20f
                
                // Helper functions
                fun drawText(text: String, x: Float, y: Float, fontSize: Float = 11f, isBold: Boolean = false, color: Int = Color.BLACK) {
                    contentStream.beginText()
                    contentStream.setFont(if (isBold) fontBold else font, fontSize)
                    contentStream.setNonStrokingColor(Color.red(color), Color.green(color), Color.blue(color))
                    contentStream.newLineAtOffset(x, y)
                    contentStream.showText(text)
                    contentStream.endText()
                }
                
                fun drawLine(y: Float, color: Int = Color.LTGRAY) {
                    contentStream.setStrokingColor(Color.red(color), Color.green(color), Color.blue(color))
                    contentStream.moveTo(margin, y)
                    contentStream.lineTo(PDRectangle.A4.width - margin, y)
                    contentStream.stroke()
                }
                
                fun checkNewPage(requiredSpace: Float) {
                    if (yPosition - requiredSpace < margin) {
                        contentStream.close()
                        val newPage = PDPage(PDRectangle.A4)
                        document.addPage(newPage)
                        val newContentStream = PDPageContentStream(document, newPage)
                        // Copy reference (need to recreate)
                        return
                    }
                }
                
                // ===== HEADER =====
                drawText(resumeData.fullName, margin, yPosition, 24f, true, Color.DKGRAY)
                yPosition -= lineHeight * 1.5
                
                val contactInfo = "${resumeData.email}  |  ${resumeData.phone}  |  ${resumeData.location}"
                drawText(contactInfo, margin, yPosition, 10f, false, Color.GRAY)
                yPosition -= lineHeight
                
                if (resumeData.linkedIn.isNotEmpty()) {
                    drawText("LinkedIn: ${resumeData.linkedIn}", margin, yPosition, 10f, false, Color.GRAY)
                    yPosition -= lineHeight
                }
                
                if (resumeData.github.isNotEmpty()) {
                    drawText("GitHub: ${resumeData.github}", margin, yPosition, 10f, false, Color.GRAY)
                    yPosition -= lineHeight
                }
                
                yPosition -= sectionSpacing
                drawLine(yPosition)
                yPosition -= sectionSpacing
                
                // ===== PROFESSIONAL SUMMARY =====
                drawText("PROFESSIONAL SUMMARY", margin, yPosition, 14f, true, Color.DKGRAY)
                yPosition -= lineHeight * 1.5
                
                val summaryLines = wrapText(resumeData.summary, font, 11f, pageWidth)
                for (line in summaryLines) {
                    checkNewPage(lineHeight)
                    drawText(line, margin, yPosition, 11f)
                    yPosition -= lineHeight
                }
                yPosition -= sectionSpacing
                drawLine(yPosition)
                yPosition -= sectionSpacing
                
                // ===== EXPERIENCE =====
                drawText("EXPERIENCE", margin, yPosition, 14f, true, Color.DKGRAY)
                yPosition -= lineHeight * 1.5
                
                for (job in resumeData.experience) {
                    checkNewPage(lineHeight * 4)
                    
                    drawText(job.title, margin, yPosition, 12f, true)
                    yPosition -= lineHeight
                    
                    val companyLine = "${job.company}  |  ${job.startDate} - ${job.endDate}  |  ${job.location}"
                    drawText(companyLine, margin, yPosition, 10f, false, Color.GRAY)
                    yPosition -= lineHeight * 1.5
                    
                    for (bullet in job.bullets) {
                        checkNewPage(lineHeight)
                        val bulletLines = wrapText("• $bullet", font, 10f, pageWidth - 20)
                        for (bl in bulletLines) {
                            drawText(bl, margin + 10, yPosition, 10f)
                            yPosition -= lineHeight
                        }
                    }
                    yPosition -= lineHeight
                }
                yPosition -= sectionSpacing
                drawLine(yPosition)
                yPosition -= sectionSpacing
                
                // ===== EDUCATION =====
                drawText("EDUCATION", margin, yPosition, 14f, true, Color.DKGRAY)
                yPosition -= lineHeight * 1.5
                
                for (edu in resumeData.education) {
                    checkNewPage(lineHeight * 3)
                    drawText(edu.degree, margin, yPosition, 11f, true)
                    yPosition -= lineHeight
                    drawText("${edu.institution}  |  ${edu.graduationYear}", margin, yPosition, 10f, false, Color.GRAY)
                    yPosition -= lineHeight * 1.5
                }
                yPosition -= sectionSpacing
                drawLine(yPosition)
                yPosition -= sectionSpacing
                
                // ===== SKILLS =====
                drawText("TECHNICAL SKILLS", margin, yPosition, 14f, true, Color.DKGRAY)
                yPosition -= lineHeight * 1.5
                
                val skillsText = resumeData.skills.joinToString(", ")
                val skillLines = wrapText(skillsText, font, 10f, pageWidth)
                for (line in skillLines) {
                    checkNewPage(lineHeight)
                    drawText(line, margin, yPosition, 10f)
                    yPosition -= lineHeight
                }
                
                contentStream.close()
                
                // Save
                val outputStream = FileOutputStream(outputFile)
                document.save(outputStream)
                document.close()
                outputStream.close()
                
                val endTime = System.nanoTime()
                val durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime)
                
                // Estimate memory (simplified)
                val runtime = Runtime.getRuntime()
                peakMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
                
                return BenchmarkResult(
                    success = true,
                    durationMs = durationMs,
                    fileSizeBytes = outputFile.length(),
                    peakMemoryMb = peakMemory,
                    pageCount = document.numberOfPages,
                    errorMessage = null
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "PDF generation failed", e)
                val endTime = System.nanoTime()
                return BenchmarkResult(
                    success = false,
                    durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime),
                    fileSizeBytes = 0,
                    peakMemoryMb = 0,
                    pageCount = 0,
                    errorMessage = e.message
                )
            }
        }
        
        private fun wrapText(text: String, font: PDType0Font, fontSize: Float, maxWidth: Float): List<String> {
            val words = text.split(" ")
            val lines = mutableListOf<String>()
            var currentLine = ""
            
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                val textWidth = font.getStringWidth(testLine) / 1000 * fontSize
                
                if (textWidth > maxWidth) {
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine)
                        currentLine = word
                    } else {
                        lines.add(word)
                    }
                } else {
                    currentLine = testLine
                }
            }
            
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
            }
            
            return lines
        }
    }
}

data class ResumeData(
    val fullName: String,
    val email: String,
    val phone: String,
    val location: String,
    val linkedIn: String,
    val github: String,
    val summary: String,
    val experience: List<Experience>,
    val education: List<Education>,
    val skills: List<String>
)

data class Experience(
    val title: String,
    val company: String,
    val location: String,
    val startDate: String,
    val endDate: String,
    val bullets: List<String>
)

data class Education(
    val degree: String,
    val institution: String,
    val graduationYear: String
)

data class BenchmarkResult(
    val success: Boolean,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val peakMemoryMb: Long,
    val pageCount: Int,
    val errorMessage: String?
)