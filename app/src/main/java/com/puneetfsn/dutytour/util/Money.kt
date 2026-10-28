package com.puneetfsn.dutytour.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * All money lives as paise (Int) in the database and in model code.
 * Only this file knows how to turn that into text, and back again.
 *
 * Grouping is written out by hand instead of borrowed from `NumberFormat`:
 * the JVM on a build server and the JVM inside a phone can disagree about
 * Indian digit grouping, and a tour sheet must read the same everywhere.
 */
object Money {

    const val SYMBOL = "₹"

    /** ₹1,23,456.50 — the form used on screen and in the PDF. */
    fun format(paise: Int): String = SYMBOL + formatAmount(paise)

    /** 1,23,456.50 — amount only, for table cells that already show a ₹ header. */
    fun formatAmount(paise: Int): String {
        val negative = paise < 0
        val absolute = kotlin.math.abs(paise.toLong())
        val rupees = absolute / 100
        val fraction = (absolute % 100).toInt()
        val fractionText = if (fraction < 10) "0$fraction" else fraction.toString()
        return (if (negative) "-" else "") + groupIndian(rupees.toString()) + "." + fractionText
    }

    /** 1,23,456 — for compact list rows; drops ".00" but keeps real paise. */
    fun formatCompact(paise: Int): String {
        return if (paise % 100 == 0) {
            val negative = paise < 0
            val rupees = kotlin.math.abs(paise.toLong()) / 100
            (if (negative) "-" else "") + groupIndian(rupees.toString())
        } else {
            formatAmount(paise)
        }
    }

    /**
     * Indian grouping: the last three digits stay together, then every two digits.
     * 1234567 -> 12,34,567
     */
    internal fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits
        val lastThree = digits.takeLast(3)
        var rest = digits.dropLast(3)
        val groups = ArrayList<String>()
        while (rest.length > 2) {
            groups.add(0, rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        if (rest.isNotEmpty()) groups.add(0, rest)
        return groups.joinToString(",") + "," + lastThree
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
