package com.puneetfsn.dutytour.ui.settings

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.puneetfsn.dutytour.data.COMPANY_NAME
import com.puneetfsn.dutytour.ui.common.QuietDivider
import com.puneetfsn.dutytour.ui.common.SectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var travellerName by remember { mutableStateOf("") }
    var defaultCity by remember { mutableStateOf("") }
    var watermarkText by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    // Fill the fields once, from what is stored; after that the user owns the text.
    LaunchedEffect(settings, loaded) {
        if (!loaded) {
            travellerName = settings.travellerName
            defaultCity = settings.defaultCity
            watermarkText = settings.watermarkText
            loaded = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.setTravellerName(travellerName)
                        viewModel.setDefaultCity(defaultCity)
                        viewModel.setWatermarkText(watermarkText)
                    }) { Text("Save") }
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

            SectionLabel("Traveller")
            OutlinedTextField(
                value = travellerName,
                onValueChange = { travellerName = it },
                label = { Text("Traveller name") },
                singleLine = true,
                supportingText = { Text("Printed on PDF exports.") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = defaultCity,
                onValueChange = { defaultCity = it },
                label = { Text("Default city") },
                singleLine = true,
                supportingText = { Text("Pre-fills the start location of a new tour.") },
                modifier = Modifier.fillMaxWidth()
            )

            QuietDivider()

            SectionLabel("Company")
            ReadOnlyRow(label = "Company", value = COMPANY_NAME)
            ReadOnlyRow(label = "Currency", value = "₹ (INR, paise precision)")

            QuietDivider()

            SectionLabel("PDF")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Watermark", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "A light word across each printed page. Off by default; " +
                            "your wording, and clearing the text prints a clean sheet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.watermarkEnabled,
                    onCheckedChange = { viewModel.setWatermark(it) }
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = watermarkText,
                onValueChange = { watermarkText = it.take(40) },
                label = { Text("Watermark text") },
                singleLine = true,
                enabled = settings.watermarkEnabled,
                supportingText = { Text("Blank means no watermark. Saved with Save above.") },
                modifier = Modifier.fillMaxWidth()
            )

            QuietDivider()

            SectionLabel("About")
            Text(
                text = "Duty Tour is a private, on-device tour log. No account, no cloud, " +
                    "no analytics, no ads. The app has no internet permission at all — " +
                    "nothing can leave this phone unless you export a PDF and share it yourself.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Amounts and place names only. Duty Tour will never ask for bank " +
                    "passwords, UPI PINs, card CVV, or OTPs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Version 1.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ReadOnlyRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
