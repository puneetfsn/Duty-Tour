package com.puneetfsn.dutytour.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.puneetfsn.dutytour.data.COMPANY_NAME

/** Small caps label used above form groups. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

/**
 * The app's masthead: "Duty Tour" with "Hendrickson" underneath.
 * Deliberately plain — this is a company tour file, not a travel app.
 */
@Composable
fun AppMasthead(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Duty Tour",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = COMPANY_NAME,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Label on the left, value right-aligned on the right. */
@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasise: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = if (emphasise) MaterialTheme.typography.titleMedium
            else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Thin rule used between summary blocks. */
@Composable
fun QuietDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    )
}

/** Money input: decimal keypad, no symbols, stored as text until saved. */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        prefix = { Text("₹") },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth()
    )
}

/** Whole-number input for nights. */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> onValueChange(new.filter { it.isDigit() }.take(4)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth()
    )
}
