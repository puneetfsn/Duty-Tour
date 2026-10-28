package com.puneetfsn.dutytour

import com.puneetfsn.dutytour.data.Tour
import com.puneetfsn.dutytour.data.TravelMode
import com.puneetfsn.dutytour.pdf.PdfContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The numbers that go on a sheet for accounts. These run on a plain JVM —
 * no Android, no PDF, just the figures the drawing code is handed.
 */
class PdfContentTest {

    private val full = Tour(
        id = 1,
        startLocation = "Delhi",
        destination = "Jaipur",
        startDate = 20_000,
        endDate = 20_003,
        travelMode = TravelMode.TRAIN,
        travelAmountPaise = 120_050,
        travelNote = "PNR 4421",
        hotelName = "Hotel Neelam",
        hotelNights = 3,
        hotelAmountPaise = 250_000,
        workAmountPaise = 500_000,
        workTitle = "Line audit",
        otherAmountPaise = 4_500,
        otherNote = "Food"
    )

    @Test
    fun sheet_has_four_named_cost_heads() {
        val heads = PdfContent.costLines(full).map { it.head }
        assertEquals(listOf("Travel cost", "Hotel cost", "Work charged", "Others"), heads)
    }

    @Test
    fun total_on_sheet_matches_the_four_lines() {
        assertEquals(874_550, PdfContent.totalPaise(full))
        assertEquals(full.totalPaise, PdfContent.totalPaise(full))
        assertEquals(874_550, PdfContent.costLines(full).sumOf { it.amountPaise })
    }

    @Test
    fun work_charge_is_its_own_line_not_folded_into_others() {
        val lines = PdfContent.costLines(full)
        val work = lines.first { it.head == "Work charged" }
        val other = lines.first { it.head == "Others" }
        assertEquals(500_000, work.amountPaise)
        assertEquals("Line audit", work.detail)
        assertEquals(4_500, other.amountPaise)
    }

    @Test
    fun optional_lines_only_appear_when_used() {
        val bare = Tour(
            startLocation = "Delhi",
            destination = "Ajmer",
            startDate = 20_000,
            travelAmountPaise = 80_000
        )
        assertEquals(listOf("Travel cost"), PdfContent.costLines(bare).map { it.head })
        assertEquals(80_000, PdfContent.totalPaise(bare))
        assertFalse(bare.hasWorkCharge)
    }

    @Test
    fun hotel_line_carries_name_and_nights() {
        val hotel = PdfContent.costLines(full).first { it.head == "Hotel cost" }
        assertEquals("Hotel Neelam · 3 nights", hotel.detail)
    }

    @Test
    fun amounts_are_indian_grouped_with_paise() {
        assertEquals("8,745.50", PdfContent.amount(874_550))
        assertEquals("₹8,745.50", PdfContent.money(874_550))
        assertEquals("1,20,000.00", PdfContent.amount(12_000_000))
    }

    @Test
    fun route_and_dates_read_like_the_list_screen() {
        assertEquals("Delhi → Jaipur", PdfContent.route(full))
        assertEquals(3, PdfContent.nightsAway(full))
        assertEquals(0, PdfContent.nightsAway(full.copy(endDate = null)))
    }

    @Test
    fun long_details_wrap_instead_of_overflowing() {
        // A crude monospace-ish measure: 1 unit per character.
        fun measure(text: String) = text.length.toFloat()
        val sentence = "Reviewed the rework log with plant QA and shared the torque report on Monday"
        val lines = PdfContent.wrap(sentence, 20f) { measure(it) }
        assertTrue("expected several lines, got $lines", lines.size > 3)
        assertTrue("a line overflowed its width: $lines", lines.all { measure(it) <= 20f })
        assertTrue("words were split: $lines", lines.all { !it.startsWith(" ") && it == it.trim() })
    }

    @Test
    fun deliberate_line_breaks_are_kept() {
        val lines = PdfContent.wrap("First line\n\nSecond line", 200f) { it.length.toFloat() }
        assertEquals(listOf("First line", "", "Second line"), lines)
    }

    @Test
    fun blank_details_produce_no_lines() {
        assertEquals(emptyList<String>(), PdfContent.wrap("   \n  ", 100f) { it.length.toFloat() })
    }

    @Test
    fun control_characters_are_stripped() {
        assertEquals("a b", PdfContent.sanitize("a\u0000\tb").replace("    ", " "))
        assertEquals("one\ntwo", PdfContent.sanitize("one\r\ntwo"))
    }

    @Test
    fun file_name_is_safe_and_recognisable() {
        assertEquals("Duty-Tour-Jaipur-2024-10-04.pdf", PdfContent.fileName(full))
        val messy = full.copy(destination = "Sri Ganganagar / Plant #2")
        assertTrue(PdfContent.fileName(messy).all { it.isLetterOrDigit() || it in "-. " })
        assertTrue(PdfContent.fileName(messy).startsWith("Duty-Tour-Sri-Ganganagar-Plant-2"))
    }
}
