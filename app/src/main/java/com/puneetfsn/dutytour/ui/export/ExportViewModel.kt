package com.puneetfsn.dutytour.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.puneetfsn.dutytour.data.Tour
import com.puneetfsn.dutytour.data.TourRepository
import com.puneetfsn.dutytour.pdf.PdfExporter
import com.puneetfsn.dutytour.util.Dates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class ExportViewModel(
    private val exporter: PdfExporter,
    repository: TourRepository
) : ViewModel() {

    enum class Scope(val label: String) {
        SINGLE("Single tour"),
        ALL("All tours"),
        RANGE("By date range")
    }

    data class UiState(
        val tours: List<Tour> = emptyList(),
        val scope: Scope = Scope.ALL,
        val selectedTourId: Long? = null,
        val fromEpochDay: Long = Dates.todayEpochDay() - 30,
        val toEpochDay: Long = Dates.todayEpochDay(),
        val busy: Boolean = false,
        val error: String? = null,
        val readyFile: File? = null,
        val done: String? = null
    ) {
        val selectedTour: Tour? get() = tours.firstOrNull { it.id == selectedTourId }
        val rangeCount: Int
            get() = tours.count { it.startDate in fromEpochDay..toEpochDay }

        /** What the traveller will get if they press the button right now. */
        val pendingCount: Int
            get() = when (scope) {
                Scope.SINGLE -> if (selectedTour != null) 1 else 0
                Scope.ALL -> tours.size
                Scope.RANGE -> rangeCount
            }
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAll().collect { tours ->
                _state.update { current ->
                    current.copy(
                        tours = tours,
                        selectedTourId = current.selectedTourId
                            ?: tours.firstOrNull()?.id
                    )
                }
            }
        }
    }

    fun setScope(scope: Scope) = _state.update { it.copy(scope = scope, error = null, done = null) }

    fun selectTour(id: Long) = _state.update { it.copy(selectedTourId = id, error = null) }

    fun setFrom(epochDay: Long) = _state.update {
        it.copy(
            fromEpochDay = epochDay,
            // Keep the range sane instead of silently producing an empty PDF.
            toEpochDay = if (it.toEpochDay < epochDay) epochDay else it.toEpochDay
        )
    }

    fun setTo(epochDay: Long) = _state.update {
        it.copy(
            toEpochDay = epochDay,
            fromEpochDay = if (it.fromEpochDay > epochDay) epochDay else it.fromEpochDay
        )
    }

    fun export() {
        val snapshot = _state.value
        if (snapshot.busy) return
        if (snapshot.tours.isEmpty()) {
            _state.update { it.copy(error = "There are no tours to export yet.") }
            return
        }

        _state.update { it.copy(busy = true, error = null, done = null) }
        viewModelScope.launch {
            val result = runCatching {
                when (snapshot.scope) {
                    Scope.SINGLE -> exporter.exportSingle(
                        snapshot.selectedTour ?: error("Choose a tour first.")
                    )
                    Scope.ALL -> exporter.exportAll()
                    Scope.RANGE -> exporter.exportRange(snapshot.fromEpochDay, snapshot.toEpochDay)
                }
            }
            _state.update { current ->
                result.fold(
                    onSuccess = { file ->
                        current.copy(busy = false, readyFile = file, done = "PDF ready — ${file.name}")
                    },
                    onFailure = { problem ->
                        current.copy(
                            busy = false,
                            error = problem.message ?: "Could not build the PDF."
                        )
                    }
                )
            }
        }
    }

    /** Called once the share sheet has been opened. */
    fun clearReadyFile() = _state.update { it.copy(readyFile = null) }

    companion object {
        fun factory(exporter: PdfExporter, repository: TourRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ExportViewModel(exporter, repository) }
            }
    }
}
