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
    val watermarkEnabled: Boolean = false,
    /**
     * Wording of the diagonal mark. Editable, so it is not a permanent company
     * stamp; blank it out and the sheet prints with no watermark at all.
     */
    val watermarkText: String = COMPANY_NAME,
    /** Placement of the mark: "diagonal" (default) or "center". */
    val watermarkAlign: String = "diagonal"
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "duty_tour_settings"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val travellerName = stringPreferencesKey("traveller_name")
        val defaultCity = stringPreferencesKey("default_city")
        val watermark = booleanPreferencesKey("watermark_enabled")
        val watermarkText = stringPreferencesKey("watermark_text")
        val watermarkAlign = stringPreferencesKey("watermark_align")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            travellerName = prefs[Keys.travellerName].orEmpty(),
            defaultCity = prefs[Keys.defaultCity].orEmpty(),
            currencySymbol = "₹",
            watermarkEnabled = prefs[Keys.watermark] ?: false,
            watermarkText = prefs[Keys.watermarkText] ?: COMPANY_NAME,
            watermarkAlign = prefs[Keys.watermarkAlign] ?: "diagonal"
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

    suspend fun setWatermarkText(text: String) {
        context.settingsDataStore.edit { it[Keys.watermarkText] = text.trim().take(40) }
    }

    suspend fun setWatermarkAlign(align: String) {
        context.settingsDataStore.edit {
            it[Keys.watermarkAlign] = if (align == "center") "center" else "diagonal"
        }
    }
}
