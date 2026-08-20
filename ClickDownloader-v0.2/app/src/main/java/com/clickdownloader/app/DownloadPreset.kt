package com.clickdownloader.app

enum class DownloadPreset(
    val label: String,
    val format: String,
    val extraArgs: List<Pair<String, String?>> = emptyList()
) {
    AUTO(
        "Auto / Best",
        "bestvideo*+bestaudio/best"
    ),
    P1080(
        "1080p",
        "bestvideo*[height<=1080]+bestaudio/best[height<=1080]"
    ),
    P720(
        "720p",
        "bestvideo*[height<=720]+bestaudio/best[height<=720]"
    ),
    P480(
        "480p",
        "bestvideo*[height<=480]+bestaudio/best[height<=480]"
    ),
    P360(
        "360p",
        "bestvideo*[height<=360]+bestaudio/best[height<=360]"
    ),
    AUDIO(
        "Audio",
        "bestaudio/best",
        listOf(
            "--extract-audio" to null,
            "--audio-format" to "mp3",
            "--audio-quality" to "0"
        )
    )
}
