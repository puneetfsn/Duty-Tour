package com.puneetfsn.dutytour.pdf

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.puneetfsn.dutytour.data.SettingsRepository
import com.puneetfsn.dutytour.data.Tour
import com.puneetfsn.dutytour.data.TourRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Turns tours into a PDF file in the app's cache, then hands that file to
 * whatever the traveller picks in the share sheet (mail, WhatsApp, Drive…).
 *
 * Export happens on a background thread; the app itself never uploads anything.
 */
class PdfExporter(
    private val context: Context,
    private val repository: TourRepository,
    private val settingsRepository: SettingsRepository
) {

    private val writer = TourPdfWriter()

    /** One tour, one sheet. */
    suspend fun exportSingle(tour: Tour): File = withContext(Dispatchers.IO) {
        write(listOf(tour), PdfContent.fileName(tour))
    }

    /** Every tour on the phone, oldest first, one sheet each. */
    suspend fun exportAll(): File = withContext(Dispatchers.IO) {
        val tours = repository.all().sortedBy { it.startDate }
        require(tours.isNotEmpty()) { "There are no tours to export yet." }
        write(tours, PdfContent.fileNameAll())
    }

    /** Inclusive date range, oldest first. */
    suspend fun exportRange(fromEpochDay: Long, toEpochDay: Long): File = withContext(Dispatchers.IO) {
        val tours = repository.between(fromEpochDay, toEpochDay).sortedBy { it.startDate }
        require(tours.isNotEmpty()) { "No tours start inside that range." }
        write(tours, PdfContent.fileNameRange(fromEpochDay, toEpochDay))
    }

    private suspend fun write(tours: List<Tour>, fileName: String): File {
        val settings = settingsRepository.settings.first()
        val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        // Keep the cache tidy: old exports are never needed once shared.
        dir.listFiles()?.forEach { stale -> stale.delete() }
        return writer.write(tours, settings, File(dir, fileName))
    }

    /** Opens the system share sheet for a finished PDF. */
    fun share(file: File, context: Context = this.context) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Duty Tour — ${file.nameWithoutExtension}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Share tour PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    companion object {
        const val EXPORT_DIR = "duty-tour-exports"
    }
}
