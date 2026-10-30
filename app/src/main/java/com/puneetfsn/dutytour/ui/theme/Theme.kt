package com.puneetfsn.dutytour.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Ledger,
    onPrimary = Color.White,
    primaryContainer = LedgerSoft,
    onPrimaryContainer = Ledger,
    secondary = Amber,
    onSecondary = Ink,
    secondaryContainer = Color(0xFFF6E8D0),
    onSecondaryContainer = Color(0xFF5A3E14),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Raise,
    onSurfaceVariant = Soft,
    surfaceContainerLow = Paper,
    surfaceContainer = Raise,
    outline = Rule,
    outlineVariant = Color(0xFFE7E2D6),
    error = Danger,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CC5B5),
    onPrimary = Color(0xFF0F221D),
    primaryContainer = Color(0xFF27443C),
    onPrimaryContainer = Color(0xFFDCE8E2),
    secondary = Amber,
    onSecondary = Color(0xFF201503),
    secondaryContainer = Color(0xFF4A3410),
    onSecondaryContainer = Color(0xFFF6E8D0),
    background = Color(0xFF141417),
    onBackground = Color(0xFFE7E5E0),
    surface = Color(0xFF141417),
    onSurface = Color(0xFFE7E5E0),
    surfaceVariant = Color(0xFF222226),
    onSurfaceVariant = Color(0xFFB9B7B0),
    surfaceContainerLow = Color(0xFF1A1A1E),
    surfaceContainer = Color(0xFF222226),
    outline = Color(0xFF3A3A40),
    outlineVariant = Color(0xFF2C2C31),
    error = Color(0xFFE08585),
)

/** Serif display faces echo the paper-slip look of the printed PDF. */
private val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = (-0.1).sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp,
    ),
    bodySmall = TextStyle(fontSize = 12.5.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
)

@Composable
fun DutyTourTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
