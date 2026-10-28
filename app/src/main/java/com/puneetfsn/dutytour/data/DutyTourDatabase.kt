package com.puneetfsn.dutytour.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Tour::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DutyTourDatabase : RoomDatabase() {

    abstract fun tourDao(): TourDao

    companion object {
        @Volatile
        private var instance: DutyTourDatabase? = null

        fun get(context: Context): DutyTourDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DutyTourDatabase::class.java,
                    "duty-tour.db"
                ).build().also { instance = it }
            }
    }
}
