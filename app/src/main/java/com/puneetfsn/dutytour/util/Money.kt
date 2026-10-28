package com.puneetfsn.dutytour.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * All money lives as paise (Int) in the database and in model code.
 * Only this file knows how to turn that into text, and back again.
 */
object Money {

    const val SYMBOL = "₹"

    private val indianNumbers: NumberFormat =
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

    private val indianWholeNumbers: NumberFormat =
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }

    /** ₹1,23,456.50 — the form used on screen and in the PDF. */
    fun format(paise: Int): String = SYMBOL + formatAmount(paise)

    /** 1,23,456.50 — amount only, for table cells that already show a ₹ header. */
    fun formatAmount(paise: Int): String =
        indianNumbers.format(BigDecimal.valueOf(paise.toLong(), 2))

    /** 1,23,456 — for compact list rows. */
    fun formatCompact(paise: Int): String {
        val rupees = paise / 100
        val hasPaise = paise % 100 != 0
        return if (hasPaise) indianNumbers.format(BigDecimal.valueOf(paise.toLong(), 2))
        else indianWholeNumbers.format(rupees)
    }

    /**
     * Accepts "1200", "1,200", "1200.5", "₹ 1200.50", "" and junk.
     * Junk becomes 0 rather than throwing — typing in a form must never crash.
     */
    fun parseToPaise(input: String): Int {
        val cleaned = input.filter { it.isDigit() || it == '.' }
        if (cleaned.isEmpty() || cleaned == ".") return 0
        val normalized = if (cleaned.count { it == '.' } > 1) {
            cleaned.dropLast(cleaned.length - cleaned.lastIndexOf('.'))
        } else {
            cleaned
        }
        return runCatching {
            BigDecimal(normalized)
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .toInt()
        }.getOrDefault(0)
    }

    /** "1200.50" for a text field; empty string when the amount is zero. */
    fun toEditableString(paise: Int): String =
        if (paise == 0) "" else BigDecimal.valueOf(paise.toLong(), 2).toPlainString()
}
