package com.clickdownloader.app

import java.util.UUID

enum class JobState { QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED }

data class DownloadJob(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String = "",
    val preset: DownloadPreset = DownloadPreset.AUTO,
    val state: JobState = JobState.QUEUED,
    val progress: Int = 0,
    val etaSeconds: Long = -1,
    val error: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
