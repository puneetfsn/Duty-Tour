package com.puneetfsn.dutytour.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.puneetfsn.dutytour.data.Tour
import com.puneetfsn.dutytour.ui.common.AppMasthead
import com.puneetfsn.dutytour.ui.theme.DutyTourTheme
import com.puneetfsn.dutytour.util.Dates
import com.puneetfsn.dutytour.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourListScreen(
    viewModel: TourListViewModel,
    onAddTour: () -> Unit,
    onOpenTour: (Long) -> Unit,
    onOpenSettings: () -> Unit
) {
    val tours by viewModel.tours.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTour,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New tour") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppMasthead(modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (tours.isEmpty()) "Tour log" else "${tours.size} tour${if (tours.size == 1) "" else "s"} on this phone",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            if (tours.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(items = tours, key = { it.id }) { tour ->
                        TourRow(tour = tour, onClick = { onOpenTour(tour.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "No tours yet.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Each tour is one sitting: route, dates, what was spent, what was done. " +
                "Everything stays on this phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TourRow(tour: Tour, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = routeLabel(tour),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = Dates.formatRangeShort(tour.startDate, tour.endDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (tour.workTitle.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = tour.workTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(0.dp))
            Text(
                text = Money.format(tour.totalPaise),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun routeLabel(tour: Tour): String {
    val from = tour.startLocation.ifBlank { "—" }
    val to = tour.destination.ifBlank { "—" }
    return "$from → $to"
}

@Preview(showBackground = true)
@Composable
private fun TourListPreview() {
    DutyTourTheme {
        Column(modifier = Modifier.padding(20.dp)) {
            AppMasthead()
            Spacer(Modifier.height(16.dp))
            TourRow(
                tour = Tour(
                    id = 1,
                    startLocation = "Delhi",
                    destination = "Jaipur",
                    startDate = 20_000,
                    workTitle = "Plant audit",
                    travelAmountPaise = 120_000,
                    hotelAmountPaise = 250_000
                ),
                onClick = {}
            )
        }
    }
}
