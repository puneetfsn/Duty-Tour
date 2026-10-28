package com.puneetfsn.dutytour.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.puneetfsn.dutytour.data.Tour
import com.puneetfsn.dutytour.data.TourRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TourDetailViewModel(
    private val repository: TourRepository,
    private val tourId: Long
) : ViewModel() {

    val tour: StateFlow<Tour?> = repository.observeById(tourId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(onDeleted: () -> Unit) {
        val current = tour.value ?: return
        viewModelScope.launch {
            repository.delete(current)
            onDeleted()
        }
    }

    companion object {
        fun factory(repository: TourRepository, tourId: Long): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { TourDetailViewModel(repository, tourId) }
            }
    }
}
