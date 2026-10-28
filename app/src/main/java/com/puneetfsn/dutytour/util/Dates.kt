package com.puneetfsn.dutytour.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Dates {

    private val display: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

    private val compact: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    fun fromEpochDay(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    /** Material3 date pickers hand back UTC midnight millis; convert without drift. */
    fun epochDayFromPickerMillis(millis: Long?): Long? =
        millis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().toEpochDay() }

    /** Turn an epoch day back into picker millis so the picker opens on the saved date. */
    fun pickerMillisFromEpochDay(epochDay: Long): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

    fun format(epochDay: Long?): String =
        epochDay?.let { display.format(fromEpochDay(it)) } ?: "—"

    /** 04 Oct 2026 → 07 Oct 2026, or just the start date for a one-day tour. */
    fun formatRange(startEpochDay: Long, endEpochDay: Long?): String {
        val start = format(startEpochDay)
        if (endEpochDay == null || endEpochDay == startEpochDay) return start
        return "$start – ${format(endEpochDay)}"
    }

    /** Short route label for list rows: 04 Oct – 07 Oct 2026. */
    fun formatRangeShort(startEpochDay: Long, endEpochDay: Long?): String {
        val start = fromEpochDay(startEpochDay)
        if (endEpochDay == null || endEpochDay == startEpochDay) return display.format(start)
        val end = fromEpochDay(endEpochDay)
        return if (start.year == end.year) {
            "${compact.format(start)} – ${display.format(end)}"
        } else {
            "${display.format(start)} – ${display.format(end)}"
        }
    }
}
