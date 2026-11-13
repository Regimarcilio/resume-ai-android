package com.resumeapp.data.repository

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.resumeapp.data.db.AppDatabase
import com.resumeapp.data.db.PdfCacheEntity
import com.resumeapp.domain.model.Resume
import com.resumeapp.domain.model.ResumeSection
import com.resumeapp.domain.model.SectionItem
import com.resumeapp.domain.repository.PdfRepository
import java.io.ByteArrayOutputStream

/**
 * PDF generation on-device.
 *
 * Uses the platform `android.graphics.pdf.PdfDocument` instead of PDFBox:
 * PDFBox 3.x depends on `java.awt.*` (desktop Java) and crashes on Android
 * with `NoClassDefFoundError: java/awt/Point`.
 */
class PdfRepositoryImpl constructor(
    private val context: android.content.Context,
    private val database: AppDatabase
) : PdfRepository {

    private val gson = Gson()
    private val itemsType = object : TypeToken<List<SectionItem>>() {}.type

    override suspend fun generatePdf(
        resume: Resume,
        template: String,
        watermark: Boolean
    ): ByteArray {
        // Cache first
        val cached = database.pdfCacheDao().getCached(resume.id, template, watermark)
        if (cached != null) return cached.pdfData

        val pdfBytes = render(resume, watermark)

        database.pdfCacheDao().insert(
            PdfCacheEntity(
                id = "${resume.id}_${template}_$watermark",
                resumeId = resume.id,
                template = template,
                watermark = watermark,
                pdfData = pdfBytes,
                createdAt = System.currentTimeMillis()
            )
        )
        return pdfBytes
    }

    override suspend fun clearCache() {
        database.pdfCacheDao().deleteAll()
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    private fun render(resume: Resume, watermark: Boolean): ByteArray {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()

        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = MARGIN

        fun newPage(): Pair<PdfDocument.Page, Canvas> {
            val p = document.startPage(pageInfo)
            return p to p.canvas
        }

        val (firstPage, firstCanvas) = newPage()
        page = firstPage
        canvas = firstCanvas

        val painter = Painter(firstCanvas)

        // ---- Header: dados pessoais ----
        val info = resume.personalInfo
        y += painter.drawLine(
            text = info.fullName.ifBlank { "Currículo" },
            y = y,
            size = 20f,
            color = Color.rgb(30, 30, 30),
            bold = true
        )
        y += 4f

        val contact = listOfNotNull(
            info.email.takeIf { it.isNotBlank() },
            info.phone.takeIf { it.isNotBlank() },
            info.location.takeIf { it.isNotBlank() },
            info.linkedin.takeIf { it.isNotBlank() }
        ).joinToString("  |  ")
        if (contact.isNotBlank()) {
            y += painter.drawWrapped(contact, y, 10f, Color.rgb(95, 95, 95))
        }
        if (info.summary.isNotBlank()) {
            y += painter.drawWrapped(info.summary, y, 11f, Color.rgb(45, 45, 45))
        }

        y += painter.drawRule(y) + 14f

        // ---- Sections ----
        resume.sections.sortedBy { it.order }.forEach { section ->
            val block = renderSection(section)
            if (y + block.height > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page!!)
                val (nextPage, nextCanvas) = newPage()
                page = nextPage
                canvas = nextCanvas
                y = MARGIN
                painter.canvas = nextCanvas
            }
            painter.drawBlock(block, y)
            y += block.height
        }

        // ---- Watermark (free tier) ----
        if (watermark) {
            page!!.canvas.save()
            page!!.canvas.rotate(-35f, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f)
            val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(38, 99, 102, 241)
                textSize = 44f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            page!!.canvas.drawText("Resume AI - versão de avaliação", PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f, wmPaint)
            page!!.canvas.restore()
        }

        document.finishPage(page!!)

        val out = ByteArrayOutputStream()
        document.writeTo(out)
        document.close()
        return out.toByteArray()
    }

    /** Desenha uma secao e devolve o bloco medido (para paginacao). */
    private fun renderSection(section: ResumeSection): Block {
        val items = runCatching { gson.fromJson<List<SectionItem>>(section.itemsJson, itemsType) }
            .getOrNull().orEmpty()

        val lines = mutableListOf<Line>()
        lines += Line(section.title.uppercase(), style = Style.HEADING)
        if (items.isEmpty()) {
            lines += Line("(sem itens)", style = Style.MUTED)
        } else {
            items.forEach { item ->
                val titleLine = listOfNotNull(
                    item.title.takeIf { it.isNotBlank() },
                    listOf(item.startDate, item.endDate).filter { it.isNotBlank() }
                        .joinToString(" - ")
                        .takeIf { it.isNotBlank() }
                ).joinToString("  |  ")
                lines += Line(titleLine, style = Style.BODY)
                item.subtitle.takeIf { it.isNotBlank() }?.let {
                    lines += Line(it, style = Style.MUTED)
                }
                item.description.takeIf { it.isNotBlank() }?.let {
                    lines += Line(it, style = Style.BODY)
                }
                item.highlights.forEach { highlight ->
                    lines += Line("- $highlight", style = Style.MUTED)
                }
                lines += Line("", style = Style.BODY, spacer = true)
            }
        }
        return Block(lines)
    }

    // -----------------------------------------------------------------------
    // Painting helpers
    // -----------------------------------------------------------------------

    private enum class Style { HEADING, BODY, MUTED }

    private data class Line(
        val text: String,
        val style: Style,
        val spacer: Boolean = false
    )

    private data class Block(val lines: List<Line>) {
        val height: Float
            get() = lines.fold(0f) { acc, line -> acc +
                if (line.spacer) 8f
                else when (line.style) {
                    Style.HEADING -> 26f
                    Style.BODY -> 15f
                    Style.MUTED -> 14f
                }
            }
    }

    private class Painter(var canvas: Canvas) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        /** Desenha uma linha unica e devolve o avanco vertical. */
        fun drawLine(
            text: String,
            y: Float,
            size: Float,
            color: Int,
            bold: Boolean = false
        ): Float {
            paint.color = color
            paint.textSize = size
            paint.typeface = if (bold) {
                Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            } else {
                Typeface.DEFAULT
            }
            canvas.drawText(text, MARGIN, y, paint)
            return size + 3f
        }

        /** Traca a regua horizontal e devolve o avanco. */
        fun drawRule(y: Float): Float {
            paint.color = Color.rgb(99, 102, 241)
            paint.strokeWidth = 1.5f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint)
            return 8f
        }

        fun drawBlock(block: Block, startY: Float) {
            var y = startY
            block.lines.forEach { line ->
                if (line.spacer) {
                    y += 8f
                    return@forEach
                }
                when (line.style) {
                    Style.HEADING -> {
                        y += drawLine(line.text, y, 13f, Color.rgb(30, 30, 30), bold = true)
                        y += 4f
                    }
                    Style.BODY -> y += drawWrapped(line.text, y, 11f, Color.rgb(45, 45, 45))
                    Style.MUTED -> y += drawWrapped(line.text, y, 10f, Color.rgb(95, 95, 95))
                }
            }
        }

        /** Desenha o texto com quebra automatica e devolve o avanco. */
        fun drawWrapped(text: String, startY: Float, size: Float, color: Int): Float {
            paint.color = color
            paint.textSize = size
            paint.typeface = Typeface.DEFAULT

            val maxWidth = PAGE_WIDTH - 2 * MARGIN
            val words = text.split(" ")
            val builder = StringBuilder()
            var y = startY
            var lines = 0

            words.forEach { word ->
                val candidate = if (builder.isEmpty()) word else "$builder $word"
                if (paint.measureText(candidate) > maxWidth && builder.isNotEmpty()) {
                    canvas.drawText(builder.toString(), MARGIN, y, paint)
                    y += size + 3f
                    lines++
                    builder.setLength(0)
                    builder.append(word)
                } else {
                    builder.setLength(0)
                    builder.append(candidate)
                }
            }
            if (builder.isNotEmpty()) {
                canvas.drawText(builder.toString(), MARGIN, y, paint)
                y += size + 3f
                lines++
            }
            return if (lines == 0) size + 3f else 0f
        }
    }

    companion object {
        // A4 em pontos (1/72 polgada)
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN = 48f
    }
}
