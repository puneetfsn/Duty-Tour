package com.puneetfsn.dutytour.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.puneetfsn.dutytour.data.COMPANY_NAME
import com.puneetfsn.dutytour.data.Tour
import com.puneetfsn.dutytour.ui.common.KeyValueRow
import com.puneetfsn.dutytour.ui.export.rememberPdfExporter
import com.puneetfsn.dutytour.ui.common.QuietDivider
import com.puneetfsn.dutytour.ui.common.SectionLabel
import com.puneetfsn.dutytour.util.Dates
import com.puneetfsn.dutytour.util.Money
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourDetailScreen(
    viewModel: TourDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenExport: () -> Unit
) {
    val tour by viewModel.tour.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    val exporter = rememberPdfExporter()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }

    // Build the file off the main thread, then hand it to the share sheet.
    fun export(action: suspend () -> java.io.File) {
        if (exporting) return
        exporting = true
        exportError = null
        scope.launch {
            runCatching { action() }
                .onSuccess { file -> exporter.share(file, context) }
                .onFailure { problem ->
                    exportError = problem.message ?: "Could not build the PDF."
                }
            exporting = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tour")
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
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit tour")
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete tour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        val current = tour
        if (current == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(20.dp)
            ) {
                Text("Tour not found.", style = MaterialTheme.typography.bodyMedium)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = routeLabel(current),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = Dates.formatRange(current.startDate, current.endDate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (current.purposeLine.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = current.purposeLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            QuietDivider()

            SectionLabel("Spend")
            KeyValueRow(
                label = "Travel (${current.travelMode.label})",
                value = Money.format(current.travelAmountPaise)
            )
            if (current.travelNote.isNotBlank()) {
                Text(
                    text = current.travelNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (current.hasHotel) {
                KeyValueRow(
                    label = hotelLabel(current),
                    value = Money.format(current.hotelAmountPaise)
                )
            }
            if (current.hasWorkCharge) {
                KeyValueRow(
                    label = if (current.workTitle.isBlank()) "Work charged"
                    else "Work charged — ${current.workTitle}",
                    value = Money.format(current.workAmountPaise)
                )
            }
            if (current.otherAmountPaise > 0) {
                KeyValueRow(
                    label = if (current.otherNote.isBlank()) "Others" else "Others — ${current.otherNote}",
                    value = Money.format(current.otherAmountPaise)
                )
            }
            QuietDivider()
            KeyValueRow(
                label = "Total",
                value = Money.format(current.totalPaise),
                emphasise = true
            )

            SectionLabel("Work done")
            if (current.workTitle.isBlank() && current.workDetails.isBlank()) {
                Text(
                    text = "Nothing recorded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (current.workTitle.isNotBlank()) {
                    Text(current.workTitle, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                }
                if (current.workDetails.isNotBlank()) {
                    Text(current.workDetails, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PDF", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "A printable A4 sheet, built on this phone. The watermark " +
                            "wording is yours to set in Settings, or clear it for none.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { export { exporter.exportSingle(current) } },
                            enabled = !exporting
                        ) {
                            Text(if (exporting) "Building…" else "Export this tour")
                        }
                        OutlinedButton(
                            onClick = { export { exporter.exportAll() } },
                            enabled = !exporting
                        ) {
                            Text("Export all")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onOpenExport, enabled = !exporting) {
                        Text("More export options")
                    }
                    exportError?.let { message ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this tour?") },
            text = { Text("This removes it from this phone. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onDeleted = onBack)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

private fun routeLabel(tour: Tour): String =
    "${tour.startLocation.ifBlank { "—" }} → ${tour.destination.ifBlank { "—" }}"

private fun hotelLabel(tour: Tour): String = buildString {
    append("Hotel")
    if (tour.hotelName.isNotBlank()) append(" — ${tour.hotelName}")
    if (tour.hotelNights > 0) {
        append(" (")
        append(tour.hotelNights)
        append(if (tour.hotelNights == 1) " night)" else " nights)")
    }
}
