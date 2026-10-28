package com.puneetfsn.dutytour.ui.list

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

class TourListViewModel(repository: TourRepository) : ViewModel() {

    val tours: StateFlow<List<Tour>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun factory(repository: TourRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { TourListViewModel(repository) }
        }
    }
}
