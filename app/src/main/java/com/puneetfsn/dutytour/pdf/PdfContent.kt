package com.puneetfsn.dutytour.pdf

import com.puneetfsn.dutytour.data.COMPANY_NAME
import com.puneetfsn.dutytour.data.Tour
import com.puneetfsn.dutytour.util.Dates
import com.puneetfsn.dutytour.util.Money

/**
 * Everything the sheet will say, worked out before a single pixel is drawn.
 *
 * Kept free of `android.*` on purpose: the numbers that go on a PDF for accounts
 * are the part worth unit-testing, and they can be tested on a plain JVM.
 */
object PdfContent {

    const val TITLE = "DUTY TOUR"
    const val COMPANY = COMPANY_NAME
    const val FOOTER = "Personal duty-tour record. Not a GST invoice."

    /** One line of the spend table. */
    data class CostLine(
        val head: String,
        val detail: String,
        val amountPaise: Int
    )

    /**
     * Four named cost heads, always in this order, plus whatever detail the
     * traveller wrote for each. Hotel and other only appear when used.
     */
    fun costLines(tour: Tour): List<CostLine> = buildList {
        add(CostLine("Travel cost", travelDetail(tour), tour.travelAmountPaise))
        if (tour.hasHotel) add(CostLine("Hotel cost", hotelDetail(tour), tour.hotelAmountPaise))
        if (tour.hasWorkCharge) {
            add(CostLine("Work charged", tour.workTitle.ifBlank { "—" }, tour.workAmountPaise))
        }
        if (tour.otherAmountPaise > 0) {
            add(CostLine("Others", tour.otherNote.ifBlank { "—" }, tour.otherAmountPaise))
        }
    }

    /** The total printed beside those lines: travel + hotel + work + other. */
    fun totalPaise(tour: Tour): Int = costLines(tour).sumOf { it.amountPaise }

    fun travelDetail(tour: Tour): String = buildString {
        append(tour.travelMode.label)
        if (tour.travelNote.isNotBlank()) append(" · ").append(tour.travelNote)
    }

    fun hotelDetail(tour: Tour): String = buildString {
        if (tour.hotelName.isNotBlank()) append(tour.hotelName) else append("—")
        if (tour.hotelNights > 0) {
            append(" · ")
            append(tour.hotelNights)
            append(if (tour.hotelNights == 1) " night" else " nights")
        }
    }

    /** Route line: From → To. */
    fun route(tour: Tour): String =
        "${tour.startLocation.ifBlank { "—" }} → ${tour.destination.ifBlank { "—" }}"

    fun dates(tour: Tour): String = Dates.formatRange(tour.startDate, tour.endDate)

    fun nightsAway(tour: Tour): Int {
        val end = tour.endDate ?: return 0
        val days = (end - tour.startDate).toInt()
        return if (days > 0) days else 0
    }

    /** Short, filesystem-safe name for the exported PDF. */
    fun fileName(tour: Tour): String {
        val place = tour.destination.ifBlank { tour.startLocation }.ifBlank { "tour" }
        val safePlace = place.replace(Regex("[^A-Za-z0-9]+"), "-").trim('-').ifBlank { "tour" }
        return "Duty-Tour-${safePlace}-${isoDate(tour.startDate)}.pdf"
    }

    fun fileNameAll(): String = "Duty-Tour-all-${isoDate(Dates.todayEpochDay())}.pdf"

    fun fileNameRange(fromEpochDay: Long, toEpochDay: Long): String =
        "Duty-Tour-${isoDate(fromEpochDay)}-to-${isoDate(toEpochDay)}.pdf"

    private fun isoDate(epochDay: Long): String = Dates.fromEpochDay(epochDay).toString()

    /**
     * Wraps text to [maxWidth], one entry per printed line, at word boundaries
     * and honouring deliberate line breaks. [measure] is the caller's own text
     * measurement (a `Paint` on the phone), which keeps this testable off-device.
     */
    fun wrap(
        text: String,
        maxWidth: Float,
        measure: (String) -> Float
    ): List<String> {
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
                if (measure(candidate) <= maxWidth) {
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

    /** Drops characters that would upset a PDF text run. */
    fun sanitize(text: String): String = text
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replace("\t", "    ")
        .filter { it == '\n' || it.code >= 0x20 }

    /** ₹1,20,000.00 — mirrors what the app shows on screen. */
    fun money(paise: Int): String = Money.format(paise)

    /** Amount column without the symbol, because the header already says ₹. */
    fun amount(paise: Int): String = Money.formatAmount(paise)
}
