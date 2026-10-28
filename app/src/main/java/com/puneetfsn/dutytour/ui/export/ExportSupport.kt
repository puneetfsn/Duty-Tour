package com.puneetfsn.dutytour.ui.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.puneetfsn.dutytour.DutyTourApp
import com.puneetfsn.dutytour.pdf.PdfExporter

/**
 * One exporter per screen, built from the app's own repositories.
 * Cheap to construct; holds no state of its own.
 */
@Composable
fun rememberPdfExporter(): PdfExporter {
    val context = LocalContext.current
    val app = context.applicationContext as DutyTourApp
    return remember(app, context) {
        PdfExporter(
            context = context,
            repository = app.tourRepository,
            settingsRepository = app.settingsRepository
        )
    }
}
