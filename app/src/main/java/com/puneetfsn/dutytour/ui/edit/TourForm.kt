package com.puneetfsn.dutytour.ui.edit

import com.puneetfsn.dutytour.data.Tour
import com.puneetfsn.dutytour.data.TravelMode
import com.puneetfsn.dutytour.util.Dates
import com.puneetfsn.dutytour.util.Money

/**
 * The screen's working copy of a tour. Money is held as raw text while typing
 * (so "12." is not destroyed under the cursor) and parsed to paise for the live total.
 */
data class TourForm(
    val id: Long = 0L,
    val startLocation: String = "",
    val destination: String = "",
    val startDate: Long? = Dates.todayEpochDay(),
    val endDate: Long? = null,
    val travelMode: TravelMode = TravelMode.TRAIN,
    val travelAmount: String = "",
    val travelNote: String = "",
    val hotelName: String = "",
    val hotelNights: String = "",
    val hotelAmount: String = "",
    val workAmount: String = "",
    val otherAmount: String = "",
    val otherNote: String = "",
    val workTitle: String = "",
    val workDetails: String = "",
    val purposeLine: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val travelPaise: Int get() = Money.parseToPaise(travelAmount)
    val hotelPaise: Int get() = Money.parseToPaise(hotelAmount)
    val workPaise: Int get() = Money.parseToPaise(workAmount)
    val otherPaise: Int get() = Money.parseToPaise(otherAmount)
    val totalPaise: Int get() = travelPaise + hotelPaise + workPaise + otherPaise

    val nights: Int get() = hotelNights.toIntOrNull() ?: 0

    fun toTour(): Tour = Tour(
        id = id,
        startLocation = startLocation.trim(),
        destination = destination.trim(),
        startDate = startDate ?: Dates.todayEpochDay(),
        endDate = endDate,
        travelMode = travelMode,
        travelAmountPaise = travelPaise,
        travelNote = travelNote.trim(),
        hotelName = hotelName.trim(),
        hotelNights = nights,
        hotelAmountPaise = hotelPaise,
        workAmountPaise = workPaise,
        otherAmountPaise = otherPaise,
        otherNote = otherNote.trim(),
        workTitle = workTitle.trim(),
        workDetails = workDetails.trim(),
        purposeLine = purposeLine.trim(),
        createdAt = createdAt
    )

    companion object {
        fun from(tour: Tour): TourForm = TourForm(
            id = tour.id,
            startLocation = tour.startLocation,
            destination = tour.destination,
            startDate = tour.startDate,
            endDate = tour.endDate,
            travelMode = tour.travelMode,
            travelAmount = Money.toEditableString(tour.travelAmountPaise),
            travelNote = tour.travelNote,
            hotelName = tour.hotelName,
            hotelNights = if (tour.hotelNights > 0) tour.hotelNights.toString() else "",
            hotelAmount = Money.toEditableString(tour.hotelAmountPaise),
            workAmount = Money.toEditableString(tour.workAmountPaise),
            otherAmount = Money.toEditableString(tour.otherAmountPaise),
            otherNote = tour.otherNote,
            workTitle = tour.workTitle,
            workDetails = tour.workDetails,
            purposeLine = tour.purposeLine,
            createdAt = tour.createdAt
        )
    }
}
