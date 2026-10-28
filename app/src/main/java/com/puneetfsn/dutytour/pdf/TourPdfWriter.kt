package com.puneetfsn.dutytour.pdf

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.puneetfsn.dutytour.data.AppSettings
import com.puneetfsn.dutytour.data.Tour
import com.puneetfsn.dutytour.util.Dates
import java.io.File

/**
 * Draws printable A4 sheets with the platform's own `PdfDocument`.
 *
 * No PDF library, no network, nothing to download — the file is produced on the
 * phone. One tour gets one sheet; notes that run long continue on a follow-on
 * sheet with the same header, so nothing is ever silently cut off.
 */
class TourPdfWriter {

    private companion object {
        const val PAGE_WIDTH = 595 // A4 at 72 dpi
        const val PAGE_HEIGHT = 842
        const val MARGIN = 42f
        const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
        const val BOTTOM = PAGE_HEIGHT - MARGIN - 24f
        const val HEADER_HEIGHT = 46f

        const val LEDGER = 0xFF1F3A34.toInt()
        const val INK = 0xFF1B1B1F.toInt()
        const val SOFT = 0xFF55555C.toInt()
        const val RULE = 0xFFC9C4B8.toInt()
        const val WATERMARK_ALPHA = 15 // roughly 6% ink

        const val COL_HEAD = 118f
        const val COL_AMOUNT = 96f
        const val META_LABEL = 62f
    }

    // ---------------------------------------------------------------- paints

    private fun paint(
        size: Float,
        color: Int = INK,
        bold: Boolean = false,
        letterSpacing: Float = 0f,
        align: Paint.Align = Paint.Align.LEFT
    ) = Paint().apply {
        isAntiAlias = true
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        else Typeface.SANS_SERIF
        this.letterSpacing = letterSpacing
        textAlign = align
    }

    private val titlePaint = paint(19f, LEDGER, bold = true)
    private val companyPaint = paint(8.5f, SOFT, letterSpacing = 0.14f)
    private val stampPaint = paint(8.5f, SOFT, align = Paint.Align.RIGHT)
    private val routePaint = paint(15f, INK, bold = true)
    private val subPaint = paint(10f, SOFT)
    private val metaLabelPaint = paint(9f, SOFT)
    private val metaValuePaint = paint(9.5f, INK)
    private val tableHeadPaint = paint(8f, SOFT, bold = true, letterSpacing = 0.09f)
    private val cellPaint = paint(9.5f, INK)
    private val cellMutedPaint = paint(9f, SOFT)
    private val cellAmountPaint = paint(9.5f, INK, align = Paint.Align.RIGHT)
    private val totalLabelPaint = paint(11.5f, INK, bold = true)
    private val totalAmountPaint = paint(11.5f, INK, bold = true, align = Paint.Align.RIGHT)
    private val sectionPaint = paint(8.5f, SOFT, bold = true, letterSpacing = 0.12f)
    private val workTitlePaint = paint(10.5f, INK, bold = true)
    private val bodyPaint = paint(9.5f, INK)
    private val footerPaint = paint(7.8f, SOFT)
    private val footerRightPaint = paint(7.8f, SOFT, align = Paint.Align.RIGHT)

    private val ledgerRule = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 0.8f
        color = LEDGER
    }
    private val hairline = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 0.5f
        color = RULE
    }

    // ---------------------------------------------------------------- public

    /**
     * Writes one sheet per tour into [target].
     *
     * @return the file written, ready to hand to a share sheet.
     */
    fun write(tours: List<Tour>, settings: AppSettings, target: File): File {
        require(tours.isNotEmpty()) { "Nothing to export" }
        target.parentFile?.mkdirs()

        val document = PdfDocument()
        try {
            tours.forEach { tour -> drawTour(document, tour, settings) }
            target.outputStream().use { document.writeTo(it) }
        } finally {
            document.close()
        }
        return target
    }

    // ---------------------------------------------------------------- sheets

    /** One open page plus its vertical cursor. */
    private class Sheet(val page: PdfDocument.Page, val canvas: Canvas, var y: Float)

    /** Tracks the current sheet and its vertical cursor, opening sheets as needed. */
    private inner class Sheets(
        private val document: PdfDocument,
        private val settings: AppSettings
    ) {
        private var sheet: Sheet
        private var pageNumber = document.pages.size + 1

        val canvas: Canvas get() = sheet.canvas
        var y: Float
            get() = sheet.y
            set(value) { sheet.y = value }

        init {
            sheet = open(continued = false)
        }

        private fun open(continued: Boolean): Sheet {
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = document.startPage(info)
            val fresh = Sheet(page, page.canvas, MARGIN + HEADER_HEIGHT + 12f)
            // Watermark first, so every line of real content sits on top of it.
            drawWatermark(fresh.canvas, settings)
            drawHeader(fresh.canvas, continued)
            return fresh
        }

        /** Moves to a follow-on sheet if [needed] points will not fit here. */
        fun ensure(needed: Float) {
            if (sheet.y + needed <= BOTTOM) return
            close()
            pageNumber += 1
            sheet = open(continued = true)
        }

        fun close() {
            drawFooter(sheet.canvas)
            document.finishPage(sheet.page)
        }
    }

    private fun drawTour(document: PdfDocument, tour: Tour, settings: AppSettings) {
        val sheets = Sheets(document, settings)
        drawRouteAndMeta(sheets, tour, settings)
        drawSpendTable(sheets, tour)
        drawWorkDone(sheets, tour)
        sheets.close()
    }

    private fun drawHeader(canvas: Canvas, continued: Boolean) {
        canvas.drawText(PdfContent.TITLE, MARGIN, MARGIN + 14f, titlePaint)
        canvas.drawText(PdfContent.COMPANY.uppercase(), MARGIN, MARGIN + 28f, companyPaint)

        val stamp = buildString {
            append(if (continued) "Continued" else "Tour record")
            append("\nGenerated on this device")
            append("\n")
            append(Dates.format(Dates.todayEpochDay()))
        }
        var stampY = MARGIN + 10f
        stamp.split("\n").forEach { line ->
            canvas.drawText(line, PAGE_WIDTH - MARGIN, stampY, stampPaint)
            stampY += 10.5f
        }

        canvas.drawLine(MARGIN, MARGIN + 36f, PAGE_WIDTH - MARGIN, MARGIN + 36f, ledgerRule)
    }

    private fun drawFooter(canvas: Canvas) {
        val y = PAGE_HEIGHT - MARGIN + 8f
        canvas.drawLine(MARGIN, y - 16f, PAGE_WIDTH - MARGIN, y - 16f, hairline)
        canvas.drawText(PdfContent.FOOTER, MARGIN, y, footerPaint)
        canvas.drawText(PdfContent.COMPANY, PAGE_WIDTH - MARGIN, y, footerRightPaint)
    }

    /**
     * A light diagonal word, drawn only when the traveller switched it on and
     * left some wording in Settings. Blank text means a clean sheet.
     */
    private fun drawWatermark(canvas: Canvas, settings: AppSettings) {
        if (!settings.watermarkEnabled) return
        val text = settings.watermarkText.trim()
        if (text.isEmpty()) return

        val markPaint = paint(84f, LEDGER, bold = true)
        markPaint.alpha = WATERMARK_ALPHA
        val maxWidth = PAGE_WIDTH * 1.15f
        var size = 84f
        while (size > 20f && markPaint.measureText(text) > maxWidth) {
            size -= 4f
            markPaint.textSize = size
        }

        canvas.save()
        canvas.rotate(-33f, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f)
        canvas.drawText(text, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f + size / 3f, markPaint)
        canvas.restore()
    }

    private fun drawRouteAndMeta(sheets: Sheets, tour: Tour, settings: AppSettings) {
        val canvas = sheets.canvas
        canvas.drawText(PdfContent.route(tour), MARGIN, sheets.y, routePaint)
        sheets.y += 16f
        canvas.drawText(PdfContent.dates(tour), MARGIN, sheets.y, subPaint)
        sheets.y += 20f

        val pairs = buildList {
            add("Dates" to PdfContent.dates(tour))
            if (tour.purposeLine.isNotBlank()) add("Purpose" to tour.purposeLine)
            add("Traveller" to settings.travellerName.ifBlank { "—" })
            val nights = PdfContent.nightsAway(tour)
            if (nights > 0) add("Nights away" to nights.toString())
        }

        val columnWidth = CONTENT_WIDTH / 2f
        val top = sheets.y
        pairs.forEachIndexed { index, (label, value) ->
            val x = MARGIN + (index % 2) * columnWidth
            val rowY = top + (index / 2) * 14f
            sheets.canvas.drawText(label, x, rowY, metaLabelPaint)
            val available = columnWidth - META_LABEL - 10f
            sheets.canvas.drawText(fit(value, metaValuePaint, available), x + META_LABEL, rowY, metaValuePaint)
        }
        sheets.y = top + ((pairs.size + 1) / 2) * 14f + 8f
    }

    private fun drawSpendTable(sheets: Sheets, tour: Tour) {
        val amountX = PAGE_WIDTH - MARGIN
        val detailX = MARGIN + COL_HEAD
        val detailWidth = amountX - COL_AMOUNT - detailX - 10f

        sheets.canvas.drawText("HEAD", MARGIN, sheets.y, tableHeadPaint)
        sheets.canvas.drawText("DETAIL", detailX, sheets.y, tableHeadPaint)
        sheets.canvas.drawText("AMOUNT (₹)", amountX, sheets.y, tableHeadPaint)
        sheets.y += 6f
        sheets.canvas.drawLine(MARGIN, sheets.y, amountX, sheets.y, hairline)
        sheets.y += 15f

        PdfContent.costLines(tour).forEach { line ->
            sheets.ensure(30f)
            val canvas = sheets.canvas
            canvas.drawText(line.head, MARGIN, sheets.y, cellPaint)
            canvas.drawText(fit(line.detail, cellMutedPaint, detailWidth), detailX, sheets.y, cellMutedPaint)
            canvas.drawText(PdfContent.amount(line.amountPaise), amountX, sheets.y, cellAmountPaint)
            sheets.y += 8f
            canvas.drawLine(MARGIN, sheets.y, amountX, sheets.y, hairline)
            sheets.y += 14f
        }

        sheets.ensure(46f)
        sheets.y += 4f
        sheets.canvas.drawLine(MARGIN, sheets.y, amountX, sheets.y, ledgerRule)
        sheets.y += 17f
        sheets.canvas.drawText("Total", MARGIN, sheets.y, totalLabelPaint)
        sheets.canvas.drawText(
            "₹ " + PdfContent.amount(PdfContent.totalPaise(tour)),
            amountX,
            sheets.y,
            totalAmountPaint
        )
        sheets.y += 16f
    }

    private fun drawWorkDone(sheets: Sheets, tour: Tour) {
        sheets.ensure(70f)
        sheets.canvas.drawText("WORK DONE", MARGIN, sheets.y, sectionPaint)
        sheets.y += 6f
        sheets.canvas.drawLine(MARGIN, sheets.y, PAGE_WIDTH - MARGIN, sheets.y, hairline)
        sheets.y += 15f

        if (tour.workTitle.isBlank() && tour.workDetails.isBlank()) {
            sheets.canvas.drawText("Nothing recorded.", MARGIN, sheets.y, cellMutedPaint)
            sheets.y += 14f
            return
        }

        val titleLines = wrap(tour.workTitle, workTitlePaint, CONTENT_WIDTH)
        titleLines.forEach { line ->
            sheets.ensure(18f)
            sheets.canvas.drawText(line, MARGIN, sheets.y, workTitlePaint)
            sheets.y += 14.5f
        }
        if (titleLines.isNotEmpty() && tour.workDetails.isNotBlank()) sheets.y += 4f

        wrap(tour.workDetails, bodyPaint, CONTENT_WIDTH).forEach { line ->
            sheets.ensure(16f)
            if (line.isEmpty()) {
                sheets.y += 7f
            } else {
                sheets.canvas.drawText(line, MARGIN, sheets.y, bodyPaint)
                sheets.y += 13.5f
            }
        }
    }

    // ---------------------------------------------------------------- text

    /** Trims to one line, ellipsising rather than overflowing a column. */
    private fun fit(text: String, paint: Paint, maxWidth: Float): String {
        val clean = sanitize(text)
        if (paint.measureText(clean) <= maxWidth) return clean
        var cut = clean
        while (cut.length > 1 && paint.measureText("$cut…") > maxWidth) {
            cut = cut.dropLast(1)
        }
        return "$cut…"
    }

    /** Wraps at word boundaries, respecting deliberate line breaks. */
    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val clean = sanitize(text).trim()
        if (clean.isEmpty()) return emptyList()

        val out = ArrayList<String>()
        clean.split("\n").forEach { paragraph ->
            if (paragraph.isBlank()) {
                out.add("")
                return@forEach
            }
            val line = StringBuilder()
            paragraph.split(" ").forEach { word ->
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    line.clear().append(candidate)
                } else {
                    if (line.isNotEmpty()) out.add(line.toString())
                    line.clear().append(word)
                }
            }
            if (line.isNotEmpty()) out.add(line.toString())
        }
        return out
    }

    /** Drops control characters that would upset a PDF text run. */
    private fun sanitize(text: String): String = text
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replace("\t", "    ")
        .filter { it == '\n' || it.code >= 0x20 }
}
