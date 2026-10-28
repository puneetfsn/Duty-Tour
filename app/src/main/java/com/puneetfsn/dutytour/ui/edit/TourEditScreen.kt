package com.puneetfsn.dutytour.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.puneetfsn.dutytour.data.COMPANY_NAME
import com.puneetfsn.dutytour.data.TravelMode
import com.puneetfsn.dutytour.ui.common.AmountField
import com.puneetfsn.dutytour.ui.common.DateField
import com.puneetfsn.dutytour.ui.common.NumberField
import com.puneetfsn.dutytour.ui.common.QuietDivider
import com.puneetfsn.dutytour.ui.common.SectionLabel
import com.puneetfsn.dutytour.util.Dates
import com.puneetfsn.dutytour.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourEditScreen(
    viewModel: TourEditViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (viewModel.isNew) "New tour" else "Edit tour")
                        Text(
                            text = COMPANY_NAME,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Total".uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Money.format(form.totalPaise),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Button(
                        onClick = { viewModel.save(onSaved) },
                        enabled = !saving
                    ) {
                        Text(if (saving) "Saving…" else "Save tour")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ---- Route -------------------------------------------------------
            SectionLabel("Route")
            OutlinedTextField(
                value = form.startLocation,
                onValueChange = { value -> viewModel.update { it.copy(startLocation = value) } },
                label = { Text("Start location") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = form.destination,
                onValueChange = { value -> viewModel.update { it.copy(destination = value) } },
                label = { Text("Destination") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = form.purposeLine,
                onValueChange = { value -> viewModel.update { it.copy(purposeLine = value) } },
                label = { Text("Client / office / purpose (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // ---- Dates -------------------------------------------------------
            SectionLabel("Dates")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField(
                    label = "Start date",
                    value = Dates.format(form.startDate),
                    onClick = { showStartPicker = true },
                    modifier = Modifier.weight(1f)
                )
                DateField(
                    label = "End date (optional)",
                    value = form.endDate?.let { Dates.format(it) }.orEmpty(),
                    onClick = { showEndPicker = true },
                    onClear = if (form.endDate != null) {
                        { viewModel.update { it.copy(endDate = null) } }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
            }

            // ---- Travel spend -------------------------------------------------
            SectionLabel("Travel spend")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TravelMode.entries.forEach { mode ->
                    FilterChip(
                        selected = form.travelMode == mode,
                        onClick = { viewModel.update { it.copy(travelMode = mode) } },
                        label = { Text(mode.label) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            AmountField(
                value = form.travelAmount,
                onValueChange = { value -> viewModel.update { it.copy(travelAmount = value) } },
                label = "Travel amount"
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = form.travelNote,
                onValueChange = { value -> viewModel.update { it.copy(travelNote = value) } },
                label = { Text("Note (optional) — PNR, cab, etc.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // ---- Hotel --------------------------------------------------------
            SectionLabel("Hotel (optional)")
            OutlinedTextField(
                value = form.hotelName,
                onValueChange = { value -> viewModel.update { it.copy(hotelName = value) } },
                label = { Text("Hotel name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(
                    value = form.hotelNights,
                    onValueChange = { value -> viewModel.update { it.copy(hotelNights = value) } },
                    label = "Nights",
                    modifier = Modifier.weight(1f)
                )
                AmountField(
                    value = form.hotelAmount,
                    onValueChange = { value -> viewModel.update { it.copy(hotelAmount = value) } },
                    label = "Hotel amount",
                    modifier = Modifier.weight(2f)
                )
            }

            // ---- Other spend ---------------------------------------------------
            SectionLabel("Other spend (optional)")
            AmountField(
                value = form.otherAmount,
                onValueChange = { value -> viewModel.update { it.copy(otherAmount = value) } },
                label = "Other amount"
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = form.otherNote,
                onValueChange = { value -> viewModel.update { it.copy(otherNote = value) } },
                label = { Text("What was it for? (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            QuietDivider()
            LiveTotal(form)

            // ---- Work done -----------------------------------------------------
            SectionLabel("Work done")
            OutlinedTextField(
                value = form.workTitle,
                onValueChange = { value -> viewModel.update { it.copy(workTitle = value) } },
                label = { Text("Short title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = form.workDetails,
                onValueChange = { value -> viewModel.update { it.copy(workDetails = value) } },
                label = { Text("Details") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            AmountField(
                value = form.workAmount,
                onValueChange = { value -> viewModel.update { it.copy(workAmount = value) } },
                label = "Work charged amount"
            )
            Text(
                text = "Charged for this work. Added to the total as its own line on the PDF.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showStartPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = form.startDate?.let { Dates.pickerMillisFromEpochDay(it) }
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    Dates.epochDayFromPickerMillis(state.selectedDateMillis)?.let { picked ->
                        viewModel.update {
                            val end = it.endDate
                            it.copy(
                                startDate = picked,
                                endDate = if (end != null && end < picked) picked else end
                            )
                        }
                    }
                    showStartPicker = false
                }) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (form.endDate ?: form.startDate)
                ?.let { Dates.pickerMillisFromEpochDay(it) }
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    Dates.epochDayFromPickerMillis(state.selectedDateMillis)?.let { picked ->
                        viewModel.update {
                            it.copy(endDate = if (picked < it.startDate.orZero()) it.startDate else picked)
                        }
                    }
                    showEndPicker = false
                }) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

private fun Long?.orZero(): Long = this ?: 0L

/** Compact running breakdown; mirrors the Total in the bottom bar. */
@Composable
private fun LiveTotal(form: TourForm) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        TotalRow("Travel cost", form.travelPaise)
        TotalRow("Hotel cost", form.hotelPaise)
        TotalRow("Work charged", form.workPaise)
        TotalRow("Others", form.otherPaise)
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 6.dp),
            color = MaterialTheme.colorScheme.outline
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Total", style = MaterialTheme.typography.titleSmall)
            Text(
                Money.format(form.totalPaise),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TotalRow(label: String, paise: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(Money.format(paise), style = MaterialTheme.typography.bodySmall)
    }
}
