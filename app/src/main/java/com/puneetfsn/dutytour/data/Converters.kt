package com.puneetfsn.dutytour.data

import androidx.room.TypeConverter

/** Room cannot store enums by itself; store the stable name, not the ordinal. */
class Converters {

    @TypeConverter
    fun fromTravelMode(mode: TravelMode): String = mode.name

    @TypeConverter
    fun toTravelMode(value: String): TravelMode =
        runCatching { TravelMode.valueOf(value) }.getOrDefault(TravelMode.OTHER)
}
