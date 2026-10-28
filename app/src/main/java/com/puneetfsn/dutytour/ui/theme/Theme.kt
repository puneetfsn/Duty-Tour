package com.puneetfsn.dutytour.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// A quiet paper-and-ink palette. No travel-startup gradients.
private val Ink = Color(0xFF1B1B1F)
private val Paper = Color(0xFFFBFAF7)
private val Ledger = Color(0xFF1F3A34)
private val LedgerSoft = Color(0xFF37544C)
private val Rule = Color(0xFFD9D4C8)

private val LightColors = lightColorScheme(
    primary = Ledger,
    onPrimary = Color(0xFFF7F4EC),
    primaryContainer = Color(0xFFDCE8E2),
    onPrimaryContainer = Ledger,
    secondary = LedgerSoft,
    onSecondary = Color(0xFFF7F4EC),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF1EEE6),
    onSurfaceVariant = Color(0xFF4A4A4F),
    outline = Rule,
    error = Color(0xFF8C2F2F)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB9CFC6),
    onPrimary = Color(0xFF10221D),
    primaryContainer = Color(0xFF2A443C),
    onPrimaryContainer = Color(0xFFDCE8E2),
    secondary = Color(0xFFA8BDB4),
    background = Color(0xFF121413),
    onBackground = Color(0xFFE7E5DF),
    surface = Color(0xFF121413),
    onSurface = Color(0xFFE7E5DF),
    surfaceVariant = Color(0xFF242725),
    onSurfaceVariant = Color(0xFFC3C6C1),
    outline = Color(0xFF4A4F4C),
    error = Color(0xFFE0B4B4)
)

@Composable
fun DutyTourTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
