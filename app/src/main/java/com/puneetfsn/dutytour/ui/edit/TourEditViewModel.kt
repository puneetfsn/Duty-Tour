package com.puneetfsn.dutytour.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.puneetfsn.dutytour.data.SettingsRepository
import com.puneetfsn.dutytour.data.TourRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TourEditViewModel(
    private val repository: TourRepository,
    private val settingsRepository: SettingsRepository,
    private val tourId: Long
) : ViewModel() {

    private val _form = MutableStateFlow(TourForm())
    val form: StateFlow<TourForm> = _form.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    val isNew: Boolean = tourId <= 0L

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        if (isNew) {
            val settings = settingsRepository.settings.first()
            _form.value = TourForm(startLocation = settings.defaultCity)
        } else {
            repository.get(tourId)?.let { _form.value = TourForm.from(it) }
        }
    }

    fun update(transform: (TourForm) -> TourForm) {
        _form.update(transform)
        if (_error.value != null) _error.value = null
    }

    fun save(onSaved: () -> Unit) {
        val form = _form.value
        val problem = validate(form)
        if (problem != null) {
            _error.value = problem
            return
        }
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            repository.save(form.toTour())
            _saving.value = false
            onSaved()
        }
    }

    private fun validate(form: TourForm): String? = when {
        form.startLocation.isBlank() -> "Start location is required."
        form.destination.isBlank() -> "Destination is required."
        form.startDate == null -> "Start date is required."
        form.endDate != null && form.startDate != null && form.endDate < form.startDate ->
            "End date cannot be before the start date."
        else -> null
    }

    companion object {
        fun factory(
            repository: TourRepository,
            settingsRepository: SettingsRepository,
            tourId: Long
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { TourEditViewModel(repository, settingsRepository, tourId) }
        }
    }
}
