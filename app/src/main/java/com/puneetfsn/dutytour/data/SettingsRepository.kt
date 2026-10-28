package com.puneetfsn.dutytour.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Company name is fixed for v1; it is not user editable. */
const val COMPANY_NAME = "Hendrickson"

data class AppSettings(
    val travellerName: String = "",
    val defaultCity: String = "",
    val currencySymbol: String = "₹",
    val watermarkEnabled: Boolean = true
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "duty_tour_settings"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val travellerName = stringPreferencesKey("traveller_name")
        val defaultCity = stringPreferencesKey("default_city")
        val watermark = booleanPreferencesKey("watermark_enabled")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            travellerName = prefs[Keys.travellerName].orEmpty(),
            defaultCity = prefs[Keys.defaultCity].orEmpty(),
            currencySymbol = "₹",
            watermarkEnabled = prefs[Keys.watermark] ?: true
        )
    }

    suspend fun setTravellerName(name: String) {
        context.settingsDataStore.edit { it[Keys.travellerName] = name.trim() }
    }

    suspend fun setDefaultCity(city: String) {
        context.settingsDataStore.edit { it[Keys.defaultCity] = city.trim() }
    }

    suspend fun setWatermarkEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.watermark] = enabled }
    }
}
