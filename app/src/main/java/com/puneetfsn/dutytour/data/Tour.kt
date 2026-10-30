package com.puneetfsn.dutytour.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** How the traveller got there. Kept as a fixed list so the PDF table stays tidy. */
enum class TravelMode(val label: String) {
    TRAIN("Train"),
    BUS("Bus"),
    CAB("Cab"),
    FLIGHT("Flight"),
    BIKE("Bike"),
    OTHER("Other")
}

/**
 * One duty tour = one sitting.
 *
 * Money is stored in **paise** (1 rupee = 100 paise) as [Int], so no floating point
 * rounding ever creeps into a total. Display code divides by 100.
 *
 * Dates are stored as **epoch day** (`LocalDate.toEpochDay()`), i.e. a plain calendar
 * date with no time or timezone attached. That keeps range filters exact.
 */
@Entity(tableName = "tours")
data class Tour(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,

    // Route
    val startLocation: String = "",
    val destination: String = "",

    // Dates
    val startDate: Long = 0L,
    val endDate: Long? = null,

    // Travel spend
    val travelMode: TravelMode = TravelMode.TRAIN,
    val travelAmountPaise: Int = 0,
    val travelNote: String = "",

    // Hotel (optional)
    val hotelName: String = "",
    val hotelNights: Int = 0,
    val hotelAmountPaise: Int = 0,

    // Work charged (what this tour was billed for)
    val workAmountPaise: Int = 0,

    // Other spend (optional)
    val otherAmountPaise: Int = 0,
    val otherNote: String = "",

    // Work done
    val workTitle: String = "",
    val workDetails: String = "",

    // Optional single line: client / office / purpose
    val purposeLine: String = "",

    // Bookkeeping
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Travel + hotel + work charged + other. Never stored, always computed,
     * so the four lines on a PDF can never disagree with the total beside them.
     */
    val totalPaise: Int
        get() = travelAmountPaise + hotelAmountPaise + workAmountPaise + otherAmountPaise

    /** True when there is a work charge to show as its own line. */
    val hasWorkCharge: Boolean
        get() = workAmountPaise > 0

    /** True when the tour has a hotel stay to show. */
    val hasHotel: Boolean
        get() = hotelName.isNotBlank() || hotelNights > 0 || hotelAmountPaise > 0
}
