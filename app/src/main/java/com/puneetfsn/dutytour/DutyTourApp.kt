package com.puneetfsn.dutytour

import android.app.Application
import com.puneetfsn.dutytour.data.DutyTourDatabase
import com.puneetfsn.dutytour.data.SettingsRepository
import com.puneetfsn.dutytour.data.TourRepository

/**
 * Manual dependency holder. One process, one user, no DI framework needed.
 * Nothing here touches the network.
 */
class DutyTourApp : Application() {

    val database: DutyTourDatabase by lazy { DutyTourDatabase.get(this) }
    val tourRepository: TourRepository by lazy { TourRepository(database.tourDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}
