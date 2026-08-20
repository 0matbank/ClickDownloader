package com.clickdownloader.app

import android.content.Context
import android.os.Environment
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

class DownloaderEngine(private val context: Context) {

    fun analyze(url: String, cookiePath: String? = null): MediaSummary {
        val request = YoutubeDLRequest(url).apply {
            addOption("--no-playlist")
            addOption("--no-warnings")
            cookiePath?.let { addOption("--cookies", it) }
        }
        val info = YoutubeDL.getInstance().getInfo(request)
        return MediaSummary(
            title = info.title ?: "Unknown title",
            uploader = info.uploader ?: "",
            durationSeconds = info.duration?.toLong() ?: 0L,
            thumbnail = info.thumbnail ?: ""
        )
    }

    fun download(
        model: DownloadRequestModel,
        processId: String,
        onProgress: (Float, Long) -> Unit
    ) {
        val outputDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "ClickDownloader"
        ).apply { mkdirs() }

        val safeTemplate = model.filenameTemplate
            .ifBlank { "%(title).180B [%(id)s].%(ext)s" }
            .replace("/", "_")
            .replace("\\", "_")

        val request = YoutubeDLRequest(model.url).apply {
            addOption("-f", model.preset.format)
            addOption("-o", File(outputDir, safeTemplate).absolutePath)
            addOption("--no-overwrites")
            addOption("--newline")
            addOption("--no-warnings")
            addOption("--continue")
            addOption("--part")
            addOption("--concurrent-fragments", "4")
            addOption("--retries", "10")
            addOption("--fragment-retries", "10")
            addOption("--retry-sleep", "fragment:exp=1:20")
            addOption("--merge-output-format", "mp4")

            model.cookiePath?.let { addOption("--cookies", it) }

            if (model.useAria2) {
                addOption("--downloader", "libaria2c.so")
                addOption("--downloader-args", "aria2c:-x8 -s8 -k1M --file-allocation=none")
            }
            if (model.embedThumbnail) addOption("--embed-thumbnail")
            if (model.embedMetadata) addOption("--embed-metadata")
            if (model.writeSubtitles) {
                addOption("--write-subs")
                addOption("--write-auto-subs")
                addOption("--sub-langs", "all,-live_chat")
                addOption("--embed-subs")
            }
            if (model.sponsorBlock) {
                addOption("--sponsorblock-remove", "sponsor,selfpromo,interaction")
            }

            model.preset.extraArgs.forEach { (key, value) ->
                if (value == null) addOption(key) else addOption(key, value)
            }
        }

        YoutubeDL.getInstance().execute(
            request,
            { progress, eta -> onProgress(progress, eta) },
            processId
        )
    }

    fun cancel(processId: String) {
        YoutubeDL.getInstance().destroyProcessById(processId)
    }
}

data class MediaSummary(
    val title: String,
    val uploader: String,
    val durationSeconds: Long,
    val thumbnail: String
)
