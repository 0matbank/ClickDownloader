package com.clickdownloader.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val urlText: String = "",
    val analyzing: Boolean = false,
    val media: MediaSummary? = null,
    val error: String? = null,
    val selectedPreset: DownloadPreset = DownloadPreset.AUTO,
    val settings: AppSettings = AppSettings(),
    val queue: List<DownloadJob> = emptyList(),
    val history: List<DownloadJob> = emptyList(),
    val cookieLoaded: Boolean = false,
    val selectedTab: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val wm = WorkManager.getInstance(application)
    private val engine = DownloaderEngine(application)
    private val settingsStore = SettingsStore(application)
    private val cookieStore = PrivateCookieStore(application)
    private val historyStore = HistoryStore(application)

    private val _state = MutableStateFlow(
        UiState(history = historyStore.load(), cookieLoaded = cookieStore.exists())
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsStore.flow.collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
    }

    fun setTab(v: Int) = _state.update { it.copy(selectedTab = v) }
    fun setUrlText(v: String) = _state.update { it.copy(urlText = v, media = null, error = null) }
    fun setPreset(v: DownloadPreset) = _state.update { it.copy(selectedPreset = v) }

    fun analyze() {
        val url = extractUrls(state.value.urlText).firstOrNull() ?: return
        viewModelScope.launch {
            _state.update { it.copy(analyzing = true, error = null) }
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    engine.analyze(url, cookieStore.pathOrNull())
                }
            }
            _state.update {
                it.copy(
                    analyzing = false,
                    media = result.getOrNull(),
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun enqueueBatch() {
        val s = state.value
        val urls = extractUrls(s.urlText).distinct()
        if (urls.isEmpty()) return

        urls.forEach { url ->
            val job = DownloadJob(url = url, preset = s.selectedPreset)
            _state.update { it.copy(queue = it.queue + job) }
            enqueueWork(job)
        }
    }

    private fun enqueueWork(job: DownloadJob) {
        val s = state.value
        val data = workDataOf(
            DownloadWorker.KEY_URL to job.url,
            DownloadWorker.KEY_PRESET to job.preset.name,
            DownloadWorker.KEY_ARIA2 to s.settings.useAria2,
            DownloadWorker.KEY_THUMB to s.settings.embedThumbnail,
            DownloadWorker.KEY_METADATA to s.settings.embedMetadata,
            DownloadWorker.KEY_SUBS to s.settings.subtitles,
            DownloadWorker.KEY_SPONSOR to s.settings.sponsorBlock,
            DownloadWorker.KEY_COOKIE_PATH to (cookieStore.pathOrNull() ?: ""),
            DownloadWorker.KEY_TEMPLATE to s.settings.filenameTemplate
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (s.settings.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
            )
            .build()

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .addTag("click-download")
            .addTag("job:${job.id}")
            .build()

        wm.enqueue(request)

        viewModelScope.launch {
            wm.getWorkInfoByIdFlow(request.id).filterNotNull().collect { info ->
                val stateName = when (info.state) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> JobState.QUEUED
                    WorkInfo.State.RUNNING -> JobState.RUNNING
                    WorkInfo.State.SUCCEEDED -> JobState.COMPLETED
                    WorkInfo.State.FAILED -> JobState.FAILED
                    WorkInfo.State.CANCELLED -> JobState.CANCELLED
                }
                val progress = info.progress.getInt(DownloadWorker.KEY_PROGRESS, 0)
                val eta = info.progress.getLong(DownloadWorker.KEY_ETA, -1)
                val err = info.outputData.getString("error") ?: ""

                _state.update { current ->
                    val newQueue = current.queue.map {
                        if (it.id == job.id) it.copy(
                            state = stateName, progress = progress,
                            etaSeconds = eta, error = err
                        ) else it
                    }

                    var history = current.history
                    if (info.state.isFinished) {
                        val finished = newQueue.firstOrNull { it.id == job.id }
                        if (finished != null && history.none { it.id == job.id }) {
                            history = listOf(finished) + history
                            historyStore.save(history)
                        }
                    }
                    current.copy(queue = newQueue, history = history)
                }
            }
        }
    }

    fun retry(job: DownloadJob) {
        val retried = job.copy(
            id = java.util.UUID.randomUUID().toString(),
            state = JobState.QUEUED,
            progress = 0,
            error = "",
            createdAt = System.currentTimeMillis()
        )
        _state.update { it.copy(queue = it.queue + retried) }
        enqueueWork(retried)
    }

    fun cancel(job: DownloadJob) {
        wm.cancelAllWorkByTag("job:${job.id}")
    }

    fun clearFinishedQueue() {
        _state.update {
            it.copy(queue = it.queue.filter { j ->
                j.state == JobState.QUEUED || j.state == JobState.RUNNING
            })
        }
    }

    fun clearHistory() {
        historyStore.clear()
        _state.update { it.copy(history = emptyList()) }
    }

    fun importCookies(uri: Uri) {
        val ok = cookieStore.importFrom(uri)
        _state.update {
            it.copy(cookieLoaded = cookieStore.exists(), error = if (ok) null else "Cookie import failed")
        }
    }

    fun clearCookies() {
        cookieStore.clear()
        _state.update { it.copy(cookieLoaded = false) }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsStore.update(transform) }
    }

    private fun extractUrls(text: String): List<String> =
        Regex("""https?://[^\s]+""")
            .findAll(text)
            .map { it.value.trim().trimEnd(',', ';') }
            .toList()
}
