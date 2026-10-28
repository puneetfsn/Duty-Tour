package com.puneetfsn.dutytour.ui.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.puneetfsn.dutytour.data.COMPANY_NAME
import com.puneetfsn.dutytour.data.Tour
import com.puneetfsn.dutytour.ui.common.DateField
import com.puneetfsn.dutytour.ui.common.QuietDivider
import com.puneetfsn.dutytour.ui.common.SectionLabel
import com.puneetfsn.dutytour.ui.export.ExportViewModel.Scope
import com.puneetfsn.dutytour.util.Dates
import com.puneetfsn.dutytour.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: ExportViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exporter = rememberPdfExporter()
    val context = LocalContext.current

    var showTourMenu by remember { mutableStateOf(false) }
    var pickFrom by remember { mutableStateOf(false) }
    var pickTo by remember { mutableStateOf(false) }

    // Hand the finished file to the share sheet, then forget it.
    LaunchedEffect(state.readyFile) {
        state.readyFile?.let { file ->
            exporter.share(file, context)
            viewModel.clearReadyFile()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Export")
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
            Text(
                text = "Printable A4. The PDF is built on this phone and leaves it only " +
                    "through the share sheet you choose.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SectionLabel("What to include")

            ScopeOption(
                scope = Scope.SINGLE,
                selected = state.scope,
                title = "Single tour",
                description = "One sheet for one tour.",
                onSelect = { viewModel.setScope(Scope.SINGLE) }
            ) {
                OutlinedButton(
                    onClick = { showTourMenu = true },
                    enabled = state.tours.isNotEmpty()
                ) {
                    Text(state.selectedTour?.let { tourLabel(it) } ?: "Choose a tour")
                }
                DropdownMenu(
                    expanded = showTourMenu,
                    onDismissRequest = { showTourMenu = false }
                ) {
                    state.tours.forEach { tour ->
                        DropdownMenuItem(
                            text = { Text(tourLabel(tour)) },
                            onClick = {
                                viewModel.selectTour(tour.id)
                                showTourMenu = false
                            }
                        )
                    }
                }
            }

            ScopeOption(
                scope = Scope.ALL,
                selected = state.scope,
                title = "All tours",
                description = "One PDF, one sheet per tour, oldest first.",
                onSelect = { viewModel.setScope(Scope.ALL) }
            )

            ScopeOption(
                scope = Scope.RANGE,
                selected = state.scope,
                title = "By date range",
                description = "Every tour that starts inside the range.",
                onSelect = { viewModel.setScope(Scope.RANGE) }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DateField(
                        label = "From",
                        value = Dates.format(state.fromEpochDay),
                        onClick = { pickFrom = true },
                        modifier = Modifier.weight(1f)
                    )
                    DateField(
                        label = "To",
                        value = Dates.format(state.toEpochDay),
                        onClick = { pickTo = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            QuietDivider()

            Text(
                text = when {
                    state.tours.isEmpty() -> "Nothing to export yet."
                    state.pendingCount == 0 -> "Nothing matches this choice."
                    state.pendingCount == 1 -> "1 tour · 1 sheet"
                    else -> "${state.pendingCount} tours · ${state.pendingCount} sheets"
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(
                    onClick = { viewModel.export() },
                    enabled = !state.busy && state.pendingCount > 0
                ) {
                    Text(if (state.busy) "Building…" else "Create PDF")
                }
                if (state.busy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }

            state.error?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            state.done?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (pickFrom) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = Dates.pickerMillisFromEpochDay(state.fromEpochDay)
        )
        DatePickerDialog(
            onDismissRequest = { pickFrom = false },
            confirmButton = {
                TextButton(onClick = {
                    Dates.epochDayFromPickerMillis(picker.selectedDateMillis)
                        ?.let { viewModel.setFrom(it) }
                    pickFrom = false
                }) { Text("Select") }
            },
            dismissButton = { TextButton(onClick = { pickFrom = false }) { Text("Cancel") } }
        ) { DatePicker(state = picker) }
    }

    if (pickTo) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = Dates.pickerMillisFromEpochDay(state.toEpochDay)
        )
        DatePickerDialog(
            onDismissRequest = { pickTo = false },
            confirmButton = {
                TextButton(onClick = {
                    Dates.epochDayFromPickerMillis(picker.selectedDateMillis)
                        ?.let { viewModel.setTo(it) }
                    pickTo = false
                }) { Text("Select") }
            },
            dismissButton = { TextButton(onClick = { pickTo = false }) { Text("Cancel") } }
        ) { DatePicker(state = picker) }
    }
}

@Composable
private fun ScopeOption(
    scope: Scope,
    selected: Scope,
    title: String,
    description: String,
    onSelect: () -> Unit,
    extra: @Composable (() -> Unit)? = null
) {
    val isSelected = selected == scope
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, onClick = onSelect)
            .padding(vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isSelected, onClick = onSelect)
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (isSelected && extra != null) {
            Column(modifier = Modifier.padding(start = 44.dp, top = 10.dp)) {
                extra()
                Spacer(Modifier.height(4.dp))
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
}

private fun tourLabel(tour: Tour): String {
    val route = "${tour.startLocation.ifBlank { "—" }} → ${tour.destination.ifBlank { "—" }}"
    return "${Dates.formatRangeShort(tour.startDate, tour.endDate)} · $route · " +
        Money.formatCompact(tour.totalPaise)
}
