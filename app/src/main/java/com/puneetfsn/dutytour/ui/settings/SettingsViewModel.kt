package com.puneetfsn.dutytour.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.puneetfsn.dutytour.data.AppSettings
import com.puneetfsn.dutytour.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setTravellerName(name: String) = viewModelScope.launch {
        repository.setTravellerName(name)
    }

    fun setDefaultCity(city: String) = viewModelScope.launch {
        repository.setDefaultCity(city)
    }

    fun setWatermark(enabled: Boolean) = viewModelScope.launch {
        repository.setWatermarkEnabled(enabled)
    }

    companion object {
        fun factory(repository: SettingsRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { SettingsViewModel(repository) } }
    }
}
