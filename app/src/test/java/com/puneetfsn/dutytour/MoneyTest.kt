package com.puneetfsn.dutytour

import com.puneetfsn.dutytour.data.Tour
import com.puneetfsn.dutytour.data.TravelMode
import com.puneetfsn.dutytour.util.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test
    fun parses_plain_rupees() {
        assertEquals(120000, Money.parseToPaise("1200"))
        assertEquals(120000, Money.parseToPaise("1,200"))
        assertEquals(120000, Money.parseToPaise("₹ 1200"))
    }

    @Test
    fun parses_paise() {
        assertEquals(120050, Money.parseToPaise("1200.50"))
        assertEquals(5, Money.parseToPaise("0.05"))
    }

    @Test
    fun rounds_extra_decimals_half_up() {
        assertEquals(120045, Money.parseToPaise("1200.449"))
        assertEquals(120045, Money.parseToPaise("1200.445"))
    }

    @Test
    fun junk_becomes_zero_instead_of_crashing() {
        assertEquals(0, Money.parseToPaise(""))
        assertEquals(0, Money.parseToPaise("abc"))
        assertEquals(0, Money.parseToPaise("."))
        assertEquals(0, Money.parseToPaise("-"))
    }

    @Test
    fun formats_with_rupee_symbol_and_indian_grouping() {
        assertEquals("₹1,20,000.00", Money.format(12000000))
        assertEquals("₹0.00", Money.format(0))
    }

    @Test
    fun editable_string_is_blank_for_zero() {
        assertEquals("", Money.toEditableString(0))
        assertEquals("1200.50", Money.toEditableString(120050))
    }

    @Test
    fun total_is_computed_from_three_buckets() {
        val tour = Tour(
            travelMode = TravelMode.TRAIN,
            travelAmountPaise = 120050,
            hotelAmountPaise = 250000,
            otherAmountPaise = 4500
        )
        assertEquals(374550, tour.totalPaise)
    }

    @Test
    fun hotel_flag_tracks_any_hotel_detail() {
        assertTrue(!Tour().hasHotel)
        assertTrue(Tour(hotelNights = 2).hasHotel)
        assertTrue(Tour(hotelName = "Hotel Neelam").hasHotel)
        assertTrue(Tour(hotelAmountPaise = 1).hasHotel)
    }
}
