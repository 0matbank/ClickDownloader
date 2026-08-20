package com.clickdownloader.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_URL = "url"
        const val KEY_PRESET = "preset"
        const val KEY_ARIA2 = "aria2"
        const val KEY_THUMB = "thumb"
        const val KEY_METADATA = "metadata"
        const val KEY_SUBS = "subs"
        const val KEY_SPONSOR = "sponsor"
        const val KEY_COOKIE_PATH = "cookie_path"
        const val KEY_TEMPLATE = "template"
        const val KEY_PROGRESS = "progress"
        const val KEY_ETA = "eta"
        private const val CHANNEL_ID = "downloads"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val preset = runCatching {
            DownloadPreset.valueOf(
                inputData.getString(KEY_PRESET) ?: DownloadPreset.AUTO.name
            )
        }.getOrDefault(DownloadPreset.AUTO)

        val model = DownloadRequestModel(
            url = url,
            preset = preset,
            useAria2 = inputData.getBoolean(KEY_ARIA2, true),
            embedThumbnail = inputData.getBoolean(KEY_THUMB, true),
            embedMetadata = inputData.getBoolean(KEY_METADATA, true),
            writeSubtitles = inputData.getBoolean(KEY_SUBS, false),
            sponsorBlock = inputData.getBoolean(KEY_SPONSOR, false),
            cookiePath = inputData.getString(KEY_COOKIE_PATH)?.takeIf { it.isNotBlank() },
            filenameTemplate = inputData.getString(KEY_TEMPLATE)
                ?: "%(title).180B [%(id)s].%(ext)s"
        )

        createChannel()
        setForeground(createForegroundInfo(0, -1))

        try {
            DownloaderEngine(applicationContext).download(model, id.toString()) { progress, eta ->
                val p = progress.toInt().coerceIn(0, 100)
                setProgressAsync(
                    workDataOf(KEY_PROGRESS to p, KEY_ETA to eta)
                )
                setForegroundAsync(createForegroundInfo(p, eta))
            }
            Result.success()
        } catch (t: Throwable) {
            if (isStopped) Result.failure(workDataOf("error" to "Cancelled"))
            else Result.failure(workDataOf("error" to (t.message ?: t.javaClass.simpleName)))
        }
    }

    private fun createForegroundInfo(progress: Int, eta: Long): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Click Downloader")
            .setContentText(if (eta >= 0) "$progress% • ETA ${eta}s" else "$progress%")
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .build()

        return if (Build.VERSION.SDK_INT >= 29) {
            ForegroundInfo(
                id.hashCode(),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(id.hashCode(), notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Downloads",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
        }
    }
}
