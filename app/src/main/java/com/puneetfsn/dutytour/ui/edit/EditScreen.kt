package com.puneetfsn.dutytour.ui.edit

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.puneetfsn.dutytour.DutyTourApp
import com.puneetfsn.dutytour.data.Dates
import com.puneetfsn.dutytour.data.Money
import com.puneetfsn.dutytour.data.Tour
import com.puneetfsn.dutytour.ui.components.SectionCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val MODES = listOf("Train", "Bus", "Cab", "Flight", "Other")

data class FormState(
    val loaded: Boolean = false,
    val id: Int? = null,
    val error: String = "",
    val traveller: String = "",
    val startLocation: String = "",
    val destination: String = "",
    val purposeLine: String = "",
    val startDateEpochDay: Long = Dates.today(),
    val endDateEpochDay: Long? = null,
    val travelMode: String = "Train",
    val travelAmountText: String = "",
    val travelNote: String = "",
    val hotelName: String = "",
    val hotelNightsText: String = "",
    val hotelAmountText: String = "",
    val workTitle: String = "",
    val workDetails: String = "",
    val workAmountText: String = "",
    val otherAmountText: String = "",
    val otherNote: String = "",
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
) {
    val travelPaise: Int get() = Money.parsePaise(travelAmountText)
    val hotelPaise: Int get() = Money.parsePaise(hotelAmountText)
    val workPaise: Int get() = Money.parsePaise(workAmountText)
    val otherPaise: Int get() = Money.parsePaise(otherAmountText)
    val totalPaise: Int get() = travelPaise + hotelPaise + workPaise + otherPaise
}

class EditViewModel(app: Application) : AndroidViewModel(app) {
    private val dutyApp = app as DutyTourApp
    private val dao = dutyApp.database.tourDao()
    private val settingsRepo = dutyApp.settings

    private val _state = MutableStateFlow(FormState())
    val state: StateFlow<FormState> = _state.asStateFlow()
    val saved = MutableStateFlow<Int?>(null)

    fun start(id: Int?) {
        if (_state.value.loaded) return
        viewModelScope.launch {
            val settings = settingsRepo.settings.first()
            val tour = id?.let { dao.byId(it) }
            _state.value = if (tour == null) {
                FormState(loaded = true, traveller = settings.travellerName, startLocation = settings.defaultCity)
            } else {
                FormState(
                    loaded = true,
                    id = tour.id,
                    traveller = tour.traveller,
                    startLocation = tour.startLocation,
                    destination = tour.destination,
                    purposeLine = tour.purposeLine,
                    startDateEpochDay = tour.startDateEpochDay,
                    endDateEpochDay = tour.endDateEpochDay,
                    travelMode = tour.travelMode,
                    travelAmountText = edit(tour.travelAmountPaise),
                    travelNote = tour.travelNote,
                    hotelName = tour.hotelName,
                    hotelNightsText = if (tour.hotelNights > 0) tour.hotelNights.toString() else "",
                    hotelAmountText = edit(tour.hotelAmountPaise),
                    workTitle = tour.workTitle,
                    workDetails = tour.workDetails,
                    workAmountText = edit(tour.workAmountPaise),
                    otherAmountText = edit(tour.otherAmountPaise),
                    otherNote = tour.otherNote,
                    createdAtEpochMillis = tour.createdAtEpochMillis,
                )
            }
        }
    }

    private fun edit(paise: Int): String = if (paise == 0) "" else Money.amount(paise)

    fun update(transform: (FormState) -> FormState) {
        _state.update { transform(it).copy(error = "") }
    }

    fun save() {
        val f = _state.value
        val problem = when {
            f.startLocation.isBlank() -> "Start location is required."
            f.destination.isBlank() -> "Destination is required."
            f.endDateEpochDay != null && f.endDateEpochDay < f.startDateEpochDay ->
                "End date cannot be before the start date."
            else -> null
        }
        if (problem != null) {
            _state.update { it.copy(error = problem) }
            return
        }
        viewModelScope.launch {
            val settings = settingsRepo.settings.first()
            val tour = Tour(
                id = f.id ?: 0,
                traveller = f.traveller.trim().ifBlank { settings.travellerName },
                startLocation = f.startLocation.trim(),
                destination = f.destination.trim(),
                startDateEpochDay = f.startDateEpochDay,
                endDateEpochDay = f.endDateEpochDay,
                purposeLine = f.purposeLine.trim(),
                travelMode = f.travelMode,
                travelAmountPaise = f.travelPaise,
                travelNote = f.travelNote.trim(),
                hotelName = f.hotelName.trim(),
                hotelNights = f.hotelNightsText.toIntOrNull() ?: 0,
                hotelAmountPaise = f.hotelPaise,
                workAmountPaise = f.workPaise,
                workTitle = f.workTitle.trim(),
                workDetails = f.workDetails.trim(),
                otherAmountPaise = f.otherPaise,
                otherNote = f.otherNote.trim(),
                createdAtEpochMillis = f.createdAtEpochMillis,
            )
            val rowId = dao.save(tour)
            saved.value = if (tour.id != 0) tour.id else rowId.toInt()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(id: String?, onDone: () -> Unit, vm: EditViewModel = viewModel()) {
    val tourId = id?.toIntOrNull()
    LaunchedEffect(tourId) { vm.start(tourId) }
    val f by vm.state.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    LaunchedEffect(saved) { if (saved != null) onDone() }

    if (!f.loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (f.id == null) "New tour" else "Edit tour") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 6.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "TOTAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            Money.format(f.totalPaise),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { vm.save() }) { Text("Save tour") }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard("Route") {
                Field("Start location", f.startLocation) { v -> vm.update { s -> s.copy(startLocation = v) } }
                Field("Destination", f.destination) { v -> vm.update { s -> s.copy(destination = v) } }
                Field("Client / office / purpose (optional)", f.purposeLine) { v -> vm.update { s -> s.copy(purposeLine = v) } }
            }

            SectionCard("Dates") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DateButton("Start date", f.startDateEpochDay) { d ->
                        vm.update { s -> s.copy(startDateEpochDay = d) }
                    }
                    DateButton(
                        "End date (optional)",
                        f.endDateEpochDay,
                        onClear = if (f.endDateEpochDay == null) null else { vm.update { s -> s.copy(endDateEpochDay = null) } },
                    ) { d -> vm.update { s -> s.copy(endDateEpochDay = d) } }
                }
            }

            SectionCard("Travel spend") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MODES.forEach { mode ->
                        FilterChip(
                            selected = f.travelMode == mode,
                            onClick = { vm.update { s -> s.copy(travelMode = mode) } },
                            label = { Text(mode) },
                        )
                    }
                }
                AmountField("Travel amount", f.travelAmountText, "Rupees and paise, e.g. 1200.50") { v ->
                    vm.update { s -> s.copy(travelAmountText = v) }
                }
                Field("Note (optional)", f.travelNote) { v -> vm.update { s -> s.copy(travelNote = v) } }
            }

            SectionCard("Hotel (optional)") {
                Field("Hotel name", f.hotelName) { v -> vm.update { s -> s.copy(hotelName = v) } }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        Field("Nights", f.hotelNightsText) { v ->
                            vm.update { s -> s.copy(hotelNightsText = v.filter { it.isDigit() }) }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        AmountField("Hotel amount", f.hotelAmountText, "") { v ->
                            vm.update { s -> s.copy(hotelAmountText = v) }
                        }
                    }
                }
            }

            SectionCard("Work done") {
                Field("Short title", f.workTitle) { v -> vm.update { s -> s.copy(workTitle = v) } }
                OutlinedTextField(
                    value = f.workDetails,
                    onValueChange = { v -> vm.update { s -> s.copy(workDetails = v) } },
                    label = { Text("Details") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                AmountField("Work charged amount", f.workAmountText, "Charged for this work. Added to the total as its own line.") { v ->
                    vm.update { s -> s.copy(workAmountText = v) }
                }
            }

            SectionCard("Other spend (optional)") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        AmountField("Other amount", f.otherAmountText, "") { v ->
                            vm.update { s -> s.copy(otherAmountText = v) }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        Field("What was it for?", f.otherNote) { v -> vm.update { s -> s.copy(otherNote = v) } }
                    }
                }
            }

            SectionCard("Traveller on this entry") {
                Field("Name printed on the PDF", f.traveller) { v -> vm.update { s -> s.copy(traveller = v) } }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    TotalRow("Travel cost", f.travelPaise)
                    TotalRow("Hotel cost", f.hotelPaise)
                    TotalRow("Work charged", f.workPaise)
                    TotalRow("Others", f.otherPaise)
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(Money.format(f.totalPaise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (f.error.isNotBlank()) {
                Text(f.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AmountField(label: String, value: String, hint: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        supportingText = if (hint.isBlank()) null else { Text(hint) },
    )
}

@Composable
private fun TotalRow(label: String, paise: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(Money.format(paise), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateButton(label: String, epochDay: Long?, onClear: (() -> Unit)? = null, onPick: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = { open = true }) { Text(if (epochDay == null) label else Dates.format(epochDay)) }
        if (onClear != null) TextButton(onClick = onClear) { Text("Clear") }
    }
    if (open) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = (epochDay ?: Dates.today()) * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { onPick(it / 86_400_000L) }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = picker)
        }
    }
}
