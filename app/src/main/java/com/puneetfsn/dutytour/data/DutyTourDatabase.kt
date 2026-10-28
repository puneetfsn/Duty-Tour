package com.puneetfsn.dutytour.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Tour::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DutyTourDatabase : RoomDatabase() {

    abstract fun tourDao(): TourDao

    companion object {
        /**
         * v1 -> v2 adds the work-charged amount. Existing tours keep their data
         * and simply start at zero, so nothing already recorded is lost.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tours ADD COLUMN workAmountPaise INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var instance: DutyTourDatabase? = null

        fun get(context: Context): DutyTourDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DutyTourDatabase::class.java,
                    "duty-tour.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
